(ns talispam.message
  (:gen-class)
  (:require [clojure.string :as s])
  (:import (org.jsoup Jsoup)
           (java.io ByteArrayOutputStream)
           (java.nio.charset Charset)
           (java.util Base64)))

(set! *warn-on-reflection* true)

(defn- parse-headers [text]
  (into {}
        (keep (fn [line]
                (when-let [[_ name value] (re-matches #"([^\s:]+):[ \t]*(.*)" line)]
                  [(s/lower-case name) (s/trim value)])))
        (s/split (s/replace text #"\r?\n[ \t]+" " ") #"\r?\n")))

(defn- split-entity [text]
  (let [[head body] (s/split text #"\r?\n\r?\n" 2)]
    {:headers (if (some? body) (parse-headers head) {})
     :body (if (some? body) body text)}))

(defn- media-type [value default]
  (s/lower-case (s/trim (first (s/split (or value default) #";" 2)))))

(defn- parameters [value]
  ;; Quoted parameters may contain semicolons and escaped characters.
  (into {}
        (map (fn [[_ name quoted unquoted]]
               [(s/lower-case name)
                (if (some? quoted)
                  (s/replace quoted #"\\(.)" "$1")
                  (s/trim unquoted))]))
        (re-seq #";\s*([^\s=;]+)\s*=\s*(?:\"((?:\\.|[^\"\\])*)\"|([^;\s]+))"
                (or value ""))))

(defn- quoted-printable-bytes [text]
  (let [text (s/replace text #"=\r?\n" "")
        output (ByteArrayOutputStream.)]
    (loop [i 0]
      (when (< i (count text))
        (if (and (= \= (nth text i))
                 (<= (+ i 3) (count text))
                 (re-matches #"[0-9a-fA-F]{2}" (subs text (inc i) (+ i 3))))
          (do (.write output (Integer/parseInt (subs text (inc i) (+ i 3)) 16))
              (recur (+ i 3)))
          (do (.write output (int (nth text i)))
              (recur (inc i))))))
    (.toByteArray output)))

(defn- decode-content [body encoding charset]
  ;; A malformed encoding or unsupported charset must not abort classification.
  (try
    (let [bytes (case (s/lower-case (s/trim (or encoding "")))
                  "base64" (.decode (Base64/getDecoder)
                                    ^String (s/replace body #"\s" ""))
                  "quoted-printable" (quoted-printable-bytes body)
                  nil)]
      (if bytes
        (String. ^bytes bytes (Charset/forName (or charset "US-ASCII")))
        body))
    (catch IllegalArgumentException _ body)))

(defn- decode-subject [subject]
  ;; Whitespace between adjacent RFC 2047 encoded words is not displayed.
  (let [subject (s/replace (or subject "") #"(\?=)[ \t]+(?==\?)" "$1")]
    (s/replace subject #"=\?([^?\s]+)\?([bBqQ])\?([^?]*)\?="
               (fn [[word charset encoding content]]
                 (try
                   (let [bytes (if (= "q" (s/lower-case encoding))
                                 (quoted-printable-bytes (s/replace content "_" " "))
                                 (.decode (Base64/getDecoder) ^String content))]
                     (String. ^bytes bytes (Charset/forName charset)))
                   (catch IllegalArgumentException _ word))))))

(defn- split-parts [body boundary]
  (let [opening (str "--" boundary)
        closing (str opening "--")]
    (loop [lines (s/split body #"\r?\n" -1), current nil, parts []]
      (if-let [line (first lines)]
        (let [delimiter (s/replace line #"[ \t]+$" "")]
          (cond
            (= delimiter closing)
            (cond-> parts (some? current) (conj (s/join "\n" current)))

            (= delimiter opening)
            (recur (next lines) []
                   (cond-> parts (some? current) (conj (s/join "\n" current))))

            :else
            (recur (next lines)
                   (when (some? current) (conj current line)) parts)))
        (cond-> parts (some? current) (conj (s/join "\n" current)))))))

(declare entity-text)

(defn- entity-text [{:keys [headers body]} depth]
  (let [content-type (get headers "content-type")
        mime (media-type content-type "text/plain")
        params (parameters content-type)
        disposition (get headers "content-disposition")]
    (cond
      ;; Bound recursion for malformed or adversarial input.
      (> depth 50) nil
      (= "attachment" (media-type disposition "")) nil

      (s/starts-with? mime "multipart/")
      (when-let [boundary (not-empty (get params "boundary"))]
        (let [parts (map split-entity (split-parts body boundary))]
          (if (= mime "multipart/alternative")
            (or (some #(when (= "text/plain" (media-type
                                             (get-in % [:headers "content-type"])
                                             "text/plain"))
                         (not-empty (entity-text % (inc depth)))) parts)
                (some #(not-empty (entity-text % (inc depth))) parts))
            (s/join "\n" (keep #(entity-text % (inc depth)) parts)))))

      (= mime "message/rfc822")
      (entity-text (split-entity (decode-content body
                                               (get headers "content-transfer-encoding")
                                               (get params "charset"))) (inc depth))

      (#{"text/plain" "text/html"} mime)
      (let [text (decode-content body (get headers "content-transfer-encoding")
                                 (get params "charset"))]
        (if (= mime "text/html") (.text (Jsoup/parse ^String text)) text))

      :else nil)))

(defn extract-text [message]
  (let [{:keys [headers] :as entity} (split-entity message)]
    ;; A blank line alone does not make free-form text an email.
    (if (some #(contains? headers %) ["subject" "from" "to" "date"
                                     "mime-version" "content-type"
                                     "content-transfer-encoding"])
      (s/join " " (remove s/blank? [(decode-subject (get headers "subject"))
                                    (entity-text entity 0)]))
      message)))
