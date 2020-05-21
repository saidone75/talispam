(ns talispam.config
  (:gen-class))

(require '[clojure.string :as s]
         '[clojure.walk :as w]
         '[talispam.utils :as utils]
         '[immuconf.config :as immu])

(def config (atom {}))

(def version nil)

(defn- adjust-paths [c]
  (w/postwalk #(if (string? %) (utils/expand-home %) %) c))

(defn load-config [f]
  (reset! config
          (adjust-paths (immu/load f))))
