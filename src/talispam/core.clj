(ns talispam.core
  (:gen-class))

(require '[clojure.string :as s]
         '[talispam.config :as c]
         '[talispam.db :as db]
         '[talispam.dictionary :as dict]
         '[talispam.filter :as f]
         '[talispam.utils :as utils]
         '[talispam.whitelist :as w]
         '[clojure.tools.cli :refer [parse-opts]]
         '[com.stuartsierra.frequencies :as freq])

(def cli-options
  [["-m" "--mbox FILE" "mailbox to analyze"
    :validate [#(utils/is-file? %) "must be a mbox file"]]
   ["-h" "--help"]])

(defn- usage [options-summary]
  (->> [(str "TaliSpam " c/version)
        ""
        "Usage: talispam [action [options]]"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  learn        train talispam classifier"
        "  score        print ham/spam score for stdin"
        "  whitelist    print a list of addresses in ham corpus"
        "  print-db     print all words from classifier db by spam score"
        "  stats        print stats summary for a mbox"]
       (s/join \newline)))

(defn- error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (s/join \newline errors)))

(defn- validate-args
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options)
      {:exit-message (usage summary) :ok? true}
      errors
      {:exit-message (error-msg errors)}
      (and (= 1 (count arguments))
           (#{"learn" "score" "whitelist" "print-db" "stats"} (first arguments)))
      {:action (first arguments) :options options}
      :else
      {:action nil})))

(defn- exit [status msg]
  (println msg)
  (System/exit status))

;; train classifier
(defn- learn! []
  (println "building classifier db...")
  (f/learn)
  (db/write-db)
  (println "done"))

(defn- format-score [score]
  ;; type hint needed for GraalVM
  (Math/round ^Float (* 100 score)))

(defn- load-db []
  (if (not (db/exists-db))
    (exit 1 "classifier db not found, see talispam -h"))
  (try
    (db/load-db)
    (catch Exception e (exit 1 "error loading classifier db"))))

;; classify stdin
(defn- classify [in & [print-score]]
  (load-db)
  (let [in (slurp in)
        score (format-score (f/score in))]
    (if print-score
      (println score)
      (println (utils/add-headers in c/version score
                                  (if (:use (:whitelist @c/config))
                                    (w/whitelisted? (utils/get-sender in))))))))

(defn- print-db []
  (load-db)
  (println
   (s/join \newline
           (map #(str (key %) " " (val %))
                (f/db-by-score)))))

(defn- stats [options]
  (load-db)
  (let [res
        (->> (s/split (slurp (:mbox options)) #"\n\n(?=From )")
             (map f/score)
             frequencies
             freq/stats)]
    (doseq [[k v] (map vector (keys res) (vals res))]
      (println (str (name k) " " v)))))

(defn -main [& args]
  ;; set version string
  ;; would be nice to read it from project.clj
  (alter-var-root #'c/version (constantly "0.2.1-SNAPSHOT"))
  
  ;; load configuration
  (try
    (c/load-config (utils/expand-home "~/.talispam/talispam.cfg.edn"))
    (catch Exception e (exit 1 (.getMessage e))))
  
  ;; load dictionary if needed
  (if (:use (:dictionary @c/config))
    (try
      (dict/load-dictionary!)
      (catch Exception e (exit 1 (.getMessage e)))))
  
  (let [{:keys [action options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (case action
        "learn" (learn!)
        "score" (classify *in* 'score)
        "whitelist" (w/print-whitelist-from-corpus)
        "print-db" (print-db)
        "stats" (stats options)
        nil (classify *in*)))))

