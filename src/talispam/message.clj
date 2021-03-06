(ns talispam.message
  (:gen-class)
  (:import (org.jsoup Jsoup)
           (org.jsoup.safety Whitelist)
           (java.util Base64)))

(require '[clojure.string :as s])

(defn- get-headers [msg]
  (reduce
   #(if-not (nil? (first %2))
      (assoc %1 (keyword (s/replace (s/lower-case (first %2)) #":$" "")) (last %2))
      %1)
   {}
   (map
    #(drop 1 (re-find  #"(^From|^[^:]+:) (.*)" %))
    (s/split (first (s/split msg #"\n\n")) #"\n(?=[^\s])"))))

(defn- is-base64? [body]
  (let [s (first (filter #(s/starts-with? % "Content-Transfer-Encoding: base64") (s/split body #"\n")))]
    (if (nil? s)
      false
      true)))

(defn- extract-part [part]
  (let [part
        (if (seq? part) (first part) part)]
    (Jsoup/clean
     (if (is-base64? part)
       (try
         (String. (.decode (Base64/getDecoder) (s/replace (second (s/split part #"\n\n")) #"[\r\n ]" "")))
         (catch Exception e part))
       (s/join " " (drop 1 (s/split part #"\n\n"))))
     (Whitelist.))))

(defn- extract-parts [message boundary]
  (let [parts
        (drop 1 (s/split message (re-pattern (str boundary "[\\n\\r]+"))))]
    (map extract-part parts)))

(defn- extract-body [headers message]
  (if (and (not (nil? (:content-type headers)))
           (s/starts-with? (:content-type headers) "multipart"))
    ;; multipart
    (let [boundary (first (drop 1 (re-find #".*boundary=\"([^\"]+)\"" (:content-type (get-headers message)))))]
      (if-not (nil? boundary)
        (apply str (extract-parts message boundary))
        (extract-part message)))
    (extract-part message)))

(defn extract-text [message]
  (let [parts (s/split message #"\n\n")]
    (if (> (count parts) 1)
      ;; likely an e-mail
      (let [headers (get-headers (first parts))]
        (s/join " "
                [(:subject headers)
                 (extract-body headers message)]))
      ;; likely not an e-mail
      message)))
