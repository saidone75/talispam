(ns talispam.whitelist
  (:gen-class))

(require '[clojure.string :as s]
         '[talispam.config :as c]
         '[talispam.corpus :as corpus]
         '[talispam.utils :as utils])

(def whitelist (atom false))

(defn- load-whitelist []
  (if (.exists (clojure.java.io/file
                (:location (:whitelist @c/config))))
    (reset! whitelist
            (set (s/split (slurp (:location (:whitelist @c/config))) #"\n")))
    (reset! whitelist #{})))

(defn whitelisted? [address]
  (if (false? @whitelist)
    (load-whitelist))
  (contains? @whitelist address))

(defn- add-to-whitelist [address]
  (if (and (not (empty? address)) (not (whitelisted? address)))
    (swap! whitelist conj address)))

(defn print-whitelist-from-corpus []
  (load-whitelist)
  (doall
   (map
    #(add-to-whitelist (utils/get-sender %))
    (corpus/ham)))
  (println (s/join "\n" (sort@whitelist))))
