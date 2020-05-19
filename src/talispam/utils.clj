(ns talispam.utils
  (:gen-class))

(require '[clojure.string :as s])

(defn add-headers [message score]
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
     (if (> score 60) "YES")
     "\r\n"
     "X-Spam-Level: "
     score
     "\r\n"
     (s/join \newline (rest message)))))

