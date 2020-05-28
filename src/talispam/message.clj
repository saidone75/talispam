(ns talispam.message
  (:gen-class)
  (:import (org.jsoup Jsoup)
           (org.jsoup.safety Whitelist)
           (java.util Base64)))

(require '[clojure.string :as s]
         '[clojure.java.io :as io]
         '[talispam.corpus :as corpus])

(defn get-headers [msg]
  (reduce
   #(if (not (nil? (first %2)))
      (assoc %1 (keyword (s/replace (s/lower-case (first %2)) #":$" "")) (last %2))
      %1)
   {}
   (map
    #(drop 1 (re-find  #"(^From|^[^:]+:) (.*)" %))
    (s/split (first (s/split msg #"\n\n")) #"\n(?=[^\s])"))))

(defn is-multipart? [message]
  (let [s (first (filter #(s/starts-with? % "Content-Type: multipart/mixed") (s/split message #"\n")))]
    (if (nil? s)
      false
      true)))

(defn- is-base64? [message]
  (let [s (first (filter #(s/starts-with? % "Content-Transfer-Encoding: base64") (s/split message #"\n")))]
    (if (nil? s)
      false
      true)))

(defn- extract-body [message]
  (let [body
        (s/join
         " "
         (rest
          (s/split message #"\n\n")))]
    (Jsoup/clean
     (if (is-base64? message)
       (do
         (try
           (String. (.decode (Base64/getDecoder) (s/replace body #"[\r\n ]" "")))
           (catch Exception e (println message))))
       body)
     (Whitelist.))))

(defn- extract-subject [message]
  (let [s (first (filter #(s/starts-with? % "Subject: ") (s/split message #"\n")))]
    (if (nil? s)
      ""
      (subs s 9))))

(defn extract-text [message]
  (if (> (count (s/split message #"\n\n")) 1)
    ;; likely an e-mail
    (str
     (extract-subject message)
     " "
     (extract-body message))
    ;; likely not an e-mail
    message))











