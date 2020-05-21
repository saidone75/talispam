(ns talispam.utils
  (:gen-class))

(require '[clojure.string :as s]
         '[talispam.config :as c])

(defn add-headers [message score & [whitelisted]]
  (let [message (s/split-lines message)]
    (str
     (first message)
     "\r\n"
     "X-Spam-Checker-Version: "
     "TaliSpam "
     c/version
     " on "
     (.getHostName (java.net.InetAddress/getLocalHost))
     "\r\n"
     "X-Spam-Flag: "
     (if (not whitelisted)
       (if (> score 60) "YES" "NO")
       "*** sender address whitelisted ***")
     "\r\n"
     "X-Spam-Level: "
     (str score "/100")
     "\r\n"
     (s/join \newline (rest message)))))

(defn get-sender [message]
  (first
   (drop 1
         (re-find #"^From ([A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\.)+[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)" message))))
