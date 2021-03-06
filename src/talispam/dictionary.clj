(ns talispam.dictionary
  (:gen-class))

(require '[clojure.java.io :as io]
         '[clojure.string :as s]
         '[cognitect.transit :as transit]
         '[talispam.config :as c])

(def dictionary (atom #{}))

(defn exists-dictionary []
  (.exists (clojure.java.io/file
            (:location (:dictionary @c/config)))))

(defn write-dictionary! []
  (with-open [o (io/output-stream (:location (:dictionary @c/config)))]
    (let [writer (transit/writer o :json)]
      (transit/write writer @dictionary))))

(defn init-dictionary! []
  (reset! dictionary (set
                      (let [dictionary
                            (map
                             #(s/split (slurp %) #"\n")
                             (:files (:dictionary @c/config)))]
                        (map #(.toLowerCase ^String (s/trim-newline %)) (flatten dictionary)))))
  (write-dictionary!))

(defn load-dictionary! []
  (if (exists-dictionary)
    (with-open [i (clojure.java.io/input-stream (:location (:dictionary @c/config)))]
      (let [reader (transit/reader i :json)]
        (reset! dictionary (transit/read reader))))
    (init-dictionary!)))

;; extracting dictionary from https://github.com/hermitdave/FrequencyWords *_full.txt
;; on line 5 set the minimum word length
;; on line 6 the number of words to keep

;1 (->> "/home/saidone/.talispam/it_full.txt"
;2      slurp
;3      (#(s/split % #"\n"))
;4      (map #(first (s/split % #" ")))
;5      (filter #(< 3 (count %)))
;6      (take 30000)
;7      (s/join \newline)
;8      (spit "/home/saidone/.talispam/it_30k.txt"))
