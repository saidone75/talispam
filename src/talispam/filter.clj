(ns talispam.filter
  (:gen-class))

(set! *warn-on-reflection* true)

(require '[talispam.config :as c]
         '[talispam.db :as db]
         '[talispam.dictionary :as dict]
         '[talispam.corpus :as corpus]
         '[talispam.message :as msg])

;; mostly based on http://www.gigamonkeys.com/book/practical-a-spam-filter.html

;; extract words from a text
;; can use a dictionary of "admissible keys" to keep classifier db size low
(defn- extract-words [text]
  (let [text
        (msg/extract-text text)
        words
        (->> text
             (re-seq #"\w{3,}")
             (map #(.toLowerCase ^String %)))]
    (if (:use (:dictionary @c/config))
      (filter #(contains? @dict/dictionary %) words)
      words)))

;; increment ham/spam counter for a given word
(defn- increment-count [word type]
  (let [word (keyword word)
        index (if (= 'ham type) 0 1)]
    (swap! db/words update word
           (fn [counts]
             (update (or counts [0 0]) index inc)))))

;; train classifier db with a single message 
(defn- train [text type]
  (run! #(increment-count % type) (distinct (extract-words text)))
  (if (= 'ham type)
    (swap! db/total-hams inc)
    (swap! db/total-spams inc)))

(defn- spam-probability [word]
  (let [word (keyword word)
        ham-count (nth (word @db/words) 0)
        spam-count (nth (word @db/words) 1)
        ham-frequency (/ ham-count (max 1 @db/total-hams))
        spam-frequency (/ spam-count (max 1 @db/total-spams))]
    (/ spam-frequency (+ spam-frequency ham-frequency))))

(defn- bayesian-spam-probability [word & {:keys [assumed-probability weight] :or {assumed-probability 0.5 weight 1.0}}]
  (let [word (keyword word)
        basic-probability (spam-probability word)
        data-points (+ (nth (word @db/words) 0) (nth (word @db/words) 1))]
    (/ (+ (* weight assumed-probability)
          (* data-points basic-probability))
       (+ weight data-points))))

(defn- inverse-chi-square [value degrees-of-freedom]
  ;; Sum the chi-square survival series in log space to avoid underflow
  ;; in its first term, even when later terms carry substantial probability.
  (cond
    (zero? value) 1.0
    (= Double/POSITIVE_INFINITY value) 0.0
    :else
    (let [m (/ value 2.0)
          log-m (Math/log m)]
      (loop [i 1, log-term (- m), log-sum (- m)]
        (if (< i (quot degrees-of-freedom 2))
          (let [next-term (+ log-term log-m (- (Math/log (double i))))
                hi (max log-sum next-term)
                lo (min log-sum next-term)]
            (recur (inc i) next-term
                   (+ hi (Math/log1p (Math/exp (- lo hi))))))
          (min 1.0 (Math/exp log-sum)))))))

(defn- fisher [probs number-of-probs]
  (inverse-chi-square
   (* -2.0 (reduce + 0.0 (map #(Math/log (double %)) probs)))
   (* 2 number-of-probs)))

(defn score [text]
  ;; Training holds the same lock so scoring never reads a partial rebuild.
  (locking db/words
    (let [words (->> (extract-words text)
                     (map keyword)
                     distinct
                     (filter #(contains? @db/words %)))
          spam-probs (map bayesian-spam-probability words)
          ham-probs (map #(- 1 %) spam-probs)
          number-of-probs (count words)]
      (if (zero? number-of-probs)
        0.5
        (/ (+ (fisher spam-probs number-of-probs)
              (- 1 (fisher ham-probs number-of-probs)))
           2.0)))))

;; build a new classifier db
(defn learn []
  (locking db/words
    (db/clear-db)
    (let [futures (doall
                   (for [ham (corpus/ham)]
                     (future (train ham 'ham))))]
      (run! deref futures))
    (let [futures (doall
                   (for [spam (corpus/spam)]
                     (future (train spam 'spam))))]
      (run! deref futures))))

(defn db-by-score [& [asc]]
  (sort-by
   val
   (if asc
     #(< %1 %2)
     #(> %1 %2)) 
   (reduce
    #(assoc %1 (name (key %2)) (score (str (key %2))))
    {}
    @db/words)))
