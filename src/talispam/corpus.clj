(ns talispam.corpus
  (:gen-class))

(set! *warn-on-reflection* true)

(require '[clojure.string :as s]
         '[talispam.config :as c])

(defn- from-dir [type]
  (map
   slurp
   (reduce
    #(concat %1 (drop 1 (file-seq (clojure.java.io/file %2))))
    []
    ((keyword (str type "-dirs")) (:training-corpus @c/config)))))

(defn- from-mbox [type]
  (flatten
   (map #(s/split % #"\n\n(?=From )")
        (map
         #(slurp %)
         ((keyword (str type "-mboxes")) (:training-corpus @c/config))))))

(defn ham []
  (concat
   (from-dir 'ham)
   (from-mbox 'ham)))

(defn spam []
  (concat
   (from-dir 'spam)
   (from-mbox 'spam)))

