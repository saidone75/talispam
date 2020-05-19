(ns talispam.dictionary
  (:gen-class))

(require '[clojure.string :as s]
         '[cognitect.transit :as transit]
         '[talispam.config :as c])

(def dictionary (atom #{}))

(defn exists-dictionary []
  (.exists (clojure.java.io/file
            (str
             (System/getProperty "user.home")
             "/"
             (:location (:dictionary c/config))))))

(defn write-dictionary! []
  (with-open [o (clojure.java.io/output-stream (str (System/getProperty "user.home") "/" (:location (:dictionary c/config))))]
    (let [writer (transit/writer o :json)]
      (transit/write writer @dictionary))))

(defn init-dictionary! []
  (reset! dictionary (set
                      (let [dictionary
                            (map
                             #(s/split (slurp %) #"\n")
                             (map
                              #(str (System/getProperty "user.home") "/" %)
                              (:files (:dictionary c/config))))]
                        (map #(.toLowerCase ^String (s/trim-newline %)) (flatten dictionary)))))
  (write-dictionary!))

(defn load-dictionary! []
  (if (exists-dictionary)
    (with-open [i (clojure.java.io/input-stream (str (System/getProperty "user.home") "/" (:location (:dictionary c/config))))]
      (let [reader (transit/reader i :json)]
        (reset! dictionary (transit/read reader))))
    (init-dictionary!)))



