(ns talispam.whitelist
  (:gen-class))

(set! *warn-on-reflection* true)

(require '[clojure.string :as s]
         '[clojure.java.io :as io]
         '[talispam.config :as c]
         '[talispam.corpus :as corpus]
         '[talispam.utils :as utils])

(def whitelist (atom nil))

(defn- load-whitelist []
  (if (.exists (io/file
                (:location (:whitelist @c/config))))
    (reset! whitelist
            (set (remove s/blank? (s/split-lines (slurp (:location (:whitelist @c/config)))))))
    (reset! whitelist #{})))

(defn whitelisted? [address]
  (when (nil? @whitelist)
    (load-whitelist))
  (and (some? address)
       (or
        ;; full address
        (contains? @whitelist address)
        ;; host or "domain"
        (not-every?
         nil?
         (map
          #(re-matches (re-pattern (str ".*" % "$")) address)
          @whitelist)))))

(defn- whitelist-file ^java.io.File []
  (io/file (get-in @c/config [:whitelist :location])))

(defn print-whitelist [_]
  (load-whitelist)
  (when (seq @whitelist)
    (println (s/join "\n" (sort @whitelist)))))

(defn ham [_]
  (load-whitelist)
  (let [addresses (keep utils/get-sender (corpus/ham))
        entries (into @whitelist addresses)
        file (whitelist-file)]
    (io/make-parents file)
    (spit file (if (seq entries) (str (s/join "\n" (sort entries)) "\n") ""))
    (reset! whitelist entries)
    (println (str "Saved " (count entries) " whitelist entries to " file))))

(defn edit-whitelist [_]
  (let [file (whitelist-file)
        editor (or (not-empty (System/getenv "VISUAL"))
                   (not-empty (System/getenv "EDITOR")) "vi")]
    (io/make-parents file)
    (.createNewFile file)
    (let [process (-> (ProcessBuilder.
                      ^"[Ljava.lang.String;" (into-array String ["/bin/sh" "-c" (str "exec " editor " \"$1\"")
                                       "talispam-editor" (.getAbsolutePath file)]))
                     (.inheritIO)
                     (.start))
          status (.waitFor process)]
      (reset! whitelist nil)
      (when-not (zero? status)
        (throw (ex-info (str "Editor exited with status " status) {:status status}))))))
