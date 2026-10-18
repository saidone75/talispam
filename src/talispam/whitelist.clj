;
; talispam — A charm to avert spam and bring good messages.
;
; Copyright (c) 2020-2026 Saidone
; SPDX-License-Identifier: MIT
;
; See LICENSE in the project root for the full license text.
;

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

(defn- normalize-sender [address]
  (when address
    (s/replace address #"(?i)^(?:prvs=[^=]+|msprvs\d*=[^=]+)=" "")))

(defn- matches-entry? [entry address]
  (if (s/starts-with? entry "@")
    ;; Explicit domain entries match only this exact host, literally.
    (= (s/lower-case (subs entry 1))
       (some-> (re-find #"@([^@]+)$" address) second s/lower-case))
    (or (= (normalize-sender entry) (normalize-sender address))
        (some? (re-matches (re-pattern (str ".*" entry "$")) address)))))

(defn whitelisted? [address]
  (when (nil? @whitelist)
    (load-whitelist))
  (boolean (and address (some #(matches-entry? % address) @whitelist))))

(defn- address-domain [entry]
  (some-> (re-matches #"[^@\s]+@([A-Za-z0-9.-]+)" entry) second s/lower-case))

(defn- normalize-entry [entry]
  (let [entry (normalize-sender (s/trim entry))]
    (if-let [domain (address-domain entry)]
      (str (subs entry 0 (s/last-index-of entry "@")) "@" domain)
      (if (re-matches #"@[A-Za-z0-9.-]+" entry)
        (s/lower-case entry)
        entry))))

(defn compact-entries
  "Normalize senders and merge only explicitly approved exact domains."
  ([entries] (compact-entries entries #{}))
  ([entries approved-domains]
   (let [entries (into #{} (comp (map normalize-entry) (remove s/blank?)) entries)
         domains (into (set (filter #(s/starts-with? % "@") entries))
                       (map #(str "@" (s/lower-case %))) approved-domains)]
     (into domains
           (remove (fn [entry]
                     (when-let [domain (address-domain entry)]
                       (contains? domains (str "@" domain)))))
           entries))))

(defn- confirm-domain? [domain examples]
  (println (str "\nDomain: " domain " (" (count examples) " addresses)"))
  (doseq [address (take 3 (sort examples))]
    (println (str "  " address)))
  (when (> (count examples) 3)
    (println (str "  ... and " (- (count examples) 3) " more")))
  (println (str "Proposed entry: @" domain " (all senders on this exact host)"))
  (loop []
    (print "Merge? [y/N] (q to cancel): ")
    (flush)
    (let [answer (some-> (read-line) s/trim s/lower-case)]
      (cond
        (or (nil? answer) (#{"q" "quit"} answer))
        (throw (ex-info "Compaction cancelled; whitelist was not saved." {}))
        (#{"y" "yes" "s" "si" "sì"} answer) true
        (#{"" "n" "no"} answer) false
        :else (do (println "Please answer y or n, or q to cancel.") (recur))))))

(defn- choose-domains [entries]
  (let [normalized (compact-entries entries)
        groups (group-by #(address-domain (normalize-entry %)) entries)]
    (reduce (fn [approved [domain examples]]
              (if (confirm-domain? domain examples)
                (conj approved domain)
                approved))
            #{}
            (sort-by key
                     (filter (fn [[domain examples]]
                               (and domain
                                    (> (count (set (map normalize-entry examples))) 1)
                                    (not (contains? normalized (str "@" domain)))))
                             groups)))))

(defn- whitelist-file ^java.io.File []
  (io/file (get-in @c/config [:whitelist :location])))

(defn- save-whitelist! [entries]
  (let [file (whitelist-file)]
    (io/make-parents file)
    (let [temp (java.io.File/createTempFile ".whitelist-" ".tmp"
                                             (.getParentFile (.getAbsoluteFile file)))]
      (try
        (spit temp (if (seq entries) (str (s/join "\n" (sort entries)) "\n") ""))
        (java.nio.file.Files/move
         (.toPath temp) (.toPath file)
         (into-array java.nio.file.CopyOption [java.nio.file.StandardCopyOption/REPLACE_EXISTING]))
        (reset! whitelist entries)
        (finally (.delete temp))))))

(defn compact [_]
  (load-whitelist)
  (let [before @whitelist
        after (compact-entries before (choose-domains before))
        file (whitelist-file)]
    (if (= before after)
      (println "No whitelist changes.")
      (let [backup (java.io.File/createTempFile
                    (str (.getName file) "-") ".bak"
                    (.getParentFile (.getAbsoluteFile file)))]
        (io/copy file backup)
        (save-whitelist! after)
        (println (str "Compacted whitelist: " (count before) " -> " (count after) " entries."))
        (println (str "Backup: " backup))))))

(defn print-whitelist [_]
  (load-whitelist)
  (when (seq @whitelist)
    (println (s/join "\n" (sort @whitelist)))))

(defn ham [_]
  (load-whitelist)
  (let [addresses (keep utils/get-sender (corpus/ham))
        entries (into @whitelist addresses)
        file (whitelist-file)]
    (save-whitelist! entries)
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
