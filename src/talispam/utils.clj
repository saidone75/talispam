(ns talispam.utils
  (:gen-class))

(require '[clojure.string :as s]
         '[talispam.config :as c])

(defn add-headers [message score]
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
     (if (> score 60) "YES")
     "\r\n"
     "X-Spam-Level: "
     (str score "/100")
     "\r\n"
     (s/join \newline (rest message)))))

(defn get-sender [message]
  (first
   (drop 1
         (re-find #"^From ([a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)" message))))


