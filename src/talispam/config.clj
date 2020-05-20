(ns talispam.config
  (:gen-class))

(require '[clojure.string :as s]
         '[clojure.walk :as w]
         '[immuconf.config :as immu])

(def config (atom {}))

(def version nil)

(defn expand-home [path]
  (if (s/starts-with? path "~/")
    (s/replace path #"^~" (System/getProperty "user.home"))
    path))

(defn- adjust-paths [c]
  (w/postwalk #(if (string? %) (expand-home %) %) c))

(defn load-config [f]
  (reset! config
          (adjust-paths (immu/load f))))
