(ns talispam.core
  (:gen-class))

(require '[clojure.string :as s]
         '[clojure.spec.alpha :as spec]
         '[talispam.config :as c]
         '[talispam.db :as db]
         '[talispam.dictionary :as dict]
         '[talispam.filter :as f]
         '[talispam.utils :as utils]
         '[talispam.whitelist :as w]
         '[cli-matic.core :refer [run-cmd]]
         '[expound.alpha :as ex]
         '[com.stuartsierra.frequencies :as freq]
         '[tlight.spin :refer [spin done]])

(defn- exit [status msg]
  (println msg)
  (System/exit status))

;; train classifier
(defn- learn! [parms]
  (spin :type :spin1 :ms 200)
  (print "building classifier db ")
  (f/learn)
  (db/write-db)
  (done)
  (println "\ndone!"))

(defn- load-db []
  (if (not (db/exists-db))
    (exit 1 (str "classifier db not found, see " c/program-name " --help")))
  (try
    (db/load-db)
    (catch Exception e (exit 1 "error loading classifier db"))))

;; classify stdin
(defn- classify [in & [print-score]]
  (load-db)
  (let [in (slurp in)
        score (Math/round ^Float (* 100 (f/score in)))]
    (if print-score
      (println score)
      (println (utils/add-headers in
                                  c/program-version
                                  score
                                  (or (:spam-threshold @c/config) 70)
                                  (if (:use (:whitelist @c/config))
                                    (w/whitelisted? (utils/get-sender in))))))))

(defn- classify-score [parms]
  (classify *in* 'score))

(defn- print-db [parms]
  (load-db)
  (println
   (s/join \newline
           (map #(str (key %) " " (val %))
                (f/db-by-score)))))

(defn- stats [options]
  (spin :type :spin1 :ms 200)
  (print "analyzing mbox ")
  (load-db)
  (let [res
        (->> (s/split (slurp (:mbox options)) #"\n\n(?=From )")
             (map f/score)
             frequencies
             freq/stats)]
    (done)
    (println "\ndone!")
    (doseq [[k v] (map vector (keys res) (vals res))]
      (println (str (name k) " " v)))))

;; validator for mbox option
(ex/def ::mbox utils/is-mbox? "should be a valid mbox file")

;; cli-matic config
(def CONFIGURATION
  {:app         {:command     c/program-name
                 :description c/program-description
                 :version     c/program-version}
   :commands    [{:command     "learn"
                  :description ["train talispam classifier"]
                  :opts []
                  :runs        learn!}
                 {:command     "score"
                  :description ["print ham/spam score for stdin"]
                  :opts []
                  :runs        classify-score}
                 {:command     "whitelist"
                  :description ["print a list of addresses in ham corpus"]
                  :opts []
                  :runs        w/print-whitelist-from-corpus}
                 {:command     "print-db"
                  :description ["print all words from classifier db by spam score"]
                  :opts []
                  :runs        print-db}
                 {:command     "stats"
                  :description ["print stats summary for a mbox"]
                  :opts        [{:option "mbox" :short "m" :default :present :as "mbox file" :type :string :spec ::mbox}]
                  :runs        stats}]})

(defn -main [& args]
  ;; load configuration
  (try
    (c/load-config (utils/expand-home "~/.talispam/talispam.cfg.edn"))
    (catch Exception e (exit 1 (.getMessage e))))
  
  ;; load dictionary if needed
  (if (:use (:dictionary @c/config))
    (try
      (dict/load-dictionary!)
      (catch Exception e (exit 1 (.getMessage e)))))
  
  (if (nil? args)
    (classify *in*)
    (do
      (println (str c/program-name " " c/program-version)) 
      (run-cmd args CONFIGURATION))))
