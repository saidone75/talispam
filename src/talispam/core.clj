(ns talispam.core
  (:gen-class))

(require '[clojure.string :as s]
         '[talispam.config :as c]
         '[talispam.db :as db]
         '[talispam.dictionary :as dict]
         '[talispam.filter :as f]
         '[clojure.tools.cli :refer [parse-opts]])

(->> "project.clj"
     slurp
     read-string
     (drop 2)
     (cons :version)
     (apply hash-map)
     (def project))

;; init dictionary if required
(if (:use (:dictionary c/config)) (dict/init-dictionary))

(def cli-options
  [["-h" "--help"]])

(defn- usage [options-summary]
  (->> ["Spamulet"
        ""
        "Usage: spamulet [options] action"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  score    print ham/spam score for stdin"
        "  learn    train spamulet classifier"]
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
           (#{"learn" "score"} (first arguments)))
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
  (Math/round (* 100 score)))

(defn- add-headers [message score]
  (str
   "X-Spam-Checker-Version: "
   "TaliSpam "
   (:version project)
   " on "
   (.getHostName (java.net.InetAddress/getLocalHost))
   "\r\n"
   "X-Spam-Flag: "
   (if (> score 60) "YES")
   "\r\n"
   "X-Spam-Level: "
   score
   "\r\n"
   message))

;; classify stdin
(defn- classify [in & [print-score]]
  (if (not (db/exists-db))
    (exit 1 "classifier db not found, see spamulet -h"))
  (db/read-db)
  (let [in (slurp in)
        score (format-score (f/score in))]
    (if print-score
      (println score)
      (println (add-headers in score)))))

(defn -main [& args]
  (let [{:keys [action options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (case action
        "learn" (learn!)
        "score" (classify *in* 'score)
        nil (classify *in*)))))
