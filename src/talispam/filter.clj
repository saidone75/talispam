(ns talispam.filter
  (:gen-class))

(require '[clojure.string :as s]
         '[talispam.config :as c]
         '[talispam.db :as db]
         '[talispam.dictionary :as dict]
         '[talispam.corpus :as corpus])

;; mostly based on http://www.gigamonkeys.com/book/practical-a-spam-filter.html

;; extract words from a text
;; can use a dictionary of "admissible keys" to keep classifier db size low
(defn- extract-words [text]
  (let [words
        (->> text
             (re-seq #"\w{3,}")
             (map #(.toLowerCase ^String %)))]
    (if (:use (:dictionary c/config))
      (filter #(contains? @dict/dictionary %) words)
      words)))

;; increment ham/spam counter for a given word
(defn- increment-count [word type]
  (let [word (keyword word)]
    (if (not (contains? @db/words word))
      (swap! db/words assoc word [0 0]))
    (let [v (if (= 'ham type) [1 0] [0 1])]
      (swap! db/words assoc word (mapv + v (word @db/words))))))

;; train classifier db with a single message 
(defn- train [text type]
  (doall (map #(increment-count % type) (extract-words text)))
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
  (let [m (if (= Double/POSITIVE_INFINITY value)
            (/ Double/MAX_VALUE 2.0)
            (/ value 2.0))]
    (min
     (reduce +
             (reductions * (Math/exp (- m)) (for [i (range 1 (/ degrees-of-freedom 2))] (/ m i))))
     1.0)))

(defn- fisher [probs number-of-probs]
  (inverse-chi-square
   (* -2 (Math/log (reduce * probs)))
   (* 2 number-of-probs)))

(defn score [text]
  (let [text (filter #(contains? @db/words %) (map keyword (extract-words text)))
        spam-probs (map bayesian-spam-probability text)
        ham-probs (map #(- 1 %) spam-probs)
        number-of-probs (count text)
        h (- 1 (fisher spam-probs number-of-probs))
        s (- 1 (fisher ham-probs number-of-probs))]
    (/ (+ (- 1 h) s) 2.0)))

;; ham and spam corpus from training

;; build a new classifier db
(defn learn []
  (db/clear-db)
  (doall (map
          #(train % 'ham)
          (corpus/ham)))
  (doall (map
          #(train % 'spam)
          (corpus/spam))))
