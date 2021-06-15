(ns talispam.utils
  (:gen-class))

(set! *warn-on-reflection* true)

(require '[clojure.string :as s]
         '[clojure.java.io :as io])

(defn expand-home [path]
  (if (s/starts-with? path "~/")
    (s/replace path #"^~" (System/getProperty "user.home"))
    path))

(defn is-file? [path]
  (and
   (.exists (io/file path))
   (.isFile (io/file path))))

(defn is-mbox? [path]
  (and (is-file? path)
       (with-open [rdr (clojure.java.io/reader path)]
         (if (s/starts-with? (or (first (line-seq rdr)) "") "From ")
           true
           false))))

(defn add-headers [message version score threshold & [whitelisted]]
  (let [message (s/split-lines message)]
    (str
     (first message)
     "\r\n"
     "X-Spam-Checker-Version: "
     "talispam "
     version
     " on "
     (.getHostName (java.net.InetAddress/getLocalHost))
     "\r\n"
     "X-Spam-Flag: "
     (if (not whitelisted)
       (if (> score threshold) "YES" "NO")
       "*** sender whitelisted ***")
     "\r\n"
     "X-Spam-Score: "
     (str score)
     "\r\n"
     (s/join \newline (rest message)))))

(defn get-sender [message]
  (first
   (drop 1
         (re-find #"^From ([A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\.)+[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)" (first (s/split message #"\n\n"))))))
