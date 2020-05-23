(ns talispam.utils
  (:gen-class))

(require '[clojure.string :as s])

(defn expand-home [path]
  (if (s/starts-with? path "~/")
    (s/replace path #"^~" (System/getProperty "user.home"))
    path))

(defn is-file? [path]
  (and
   (.exists (clojure.java.io/file path))
   (.isFile (clojure.java.io/file path))))

(defn is-mbox? [path]
  ;; TODO proper implementation
  (is-file? path))

(defn add-headers [message version score & [whitelisted]]
  (let [message (s/split-lines message)]
    (str
     (first message)
     "\r\n"
     "X-Spam-Checker-Version: "
     "TaliSpam "
     version
     " on "
     (.getHostName (java.net.InetAddress/getLocalHost))
     "\r\n"
     "X-Spam-Flag: "
     (if (not whitelisted)
       (if (> score 60) "YES" "NO")
       "*** sender whitelisted ***")
     "\r\n"
     "X-Spam-Level: "
     (str score "/100")
     "\r\n"
     (s/join \newline (rest message)))))

(defn get-sender [message]
  (first
   (drop 1
         (re-find #"^From ([A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\.)+[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)" message))))
