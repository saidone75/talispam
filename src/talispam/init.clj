(ns talispam.init
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [talispam.utils :as utils])
  (:import [java.net URI]
           [java.nio.file Files CopyOption]))

(def source-url "https://raw.githubusercontent.com/saidone75/talispam-config/master/")

(defn download! [name file]
  (let [connection (.openConnection (.toURL (URI. (str source-url name))))]
    (.setConnectTimeout connection 15000)
    (.setReadTimeout connection 30000)
    (with-open [in (.getInputStream connection)]
      (io/copy in file))))

(defn- install! [^java.io.File file write!]
  (if (.exists file)
    (println "Keeping" (.getPath file))
    (do
      (io/make-parents file)
      (let [temp (java.io.File/createTempFile ".talispam-" ".tmp" (.getParentFile file))]
        (try
          (write! temp)
          (Files/move (.toPath temp) (.toPath file) (make-array CopyOption 0))
          (println "Created" (.getPath file))
          (finally (.delete temp)))))))

(defn initialize [_]
  (let [root (io/file (utils/expand-home "~/.talispam"))
        cfg (io/file root "talispam.cfg.edn")]
    (install! cfg #(download! "talispam.cfg.edn" %))
    (let [settings (edn/read-string (slurp cfg))]
      (doseq [path (get-in settings [:dictionary :files])]
        (let [file (io/file (utils/expand-home path))
              name (.getName file)]
          (install! file #(if (#{"en_20k.txt" "it_30k.txt"} name)
                            (download! name %)
                            (throw (ex-info (str "Unknown dictionary: " name) {:file path}))))))
      (install! (io/file (utils/expand-home (get-in settings [:whitelist :location])))
                #(spit % ""))
      (doseq [path (concat (get-in settings [:training-corpus :ham-dirs])
                          (get-in settings [:training-corpus :spam-dirs]))]
        (let [dir (io/file (utils/expand-home path))]
          (when-not (or (.isDirectory dir) (.mkdirs dir))
            (throw (ex-info (str "Cannot create corpus directory: " dir) {}))))))
    (println "Configuration ready. Configure your ham/spam corpus before running talispam learn.")))
