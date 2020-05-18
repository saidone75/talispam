(ns talispam.db
  (:gen-class))

(require '[clojure.string :as s]
         '[cognitect.transit :as transit]
         '[talispam.config :as c])

(def words (atom {}))
(def total-hams (atom 0))
(def total-spams (atom 0))

(defn clear-db []
  (reset! words {})
  (reset! total-hams 0)
  (reset! total-spams 0))

(defn exists-db []
  (.exists (clojure.java.io/file
            (str
             (System/getProperty "user.home")
             "/"
             (:filter-db-location c/config)
             "/db"))))

(defn read-db []
  (with-open [i (clojure.java.io/input-stream (str (System/getProperty "user.home") "/" (:filter-db-location c/config) "/db"))]
    (let [reader (transit/reader i :json)
          db (transit/read reader)]
      (reset! words (first db))
      (reset! total-hams (second db))
      (reset! total-spams (last db)))))

(defn write-db []
  (with-open [o (clojure.java.io/output-stream (str (System/getProperty "user.home") "/" (:filter-db-location c/config) "/db"))]
    (let [writer (transit/writer o :json)]
      (transit/write writer [@words @total-hams @total-spams]))))
