(ns talispam.config
  (:gen-class))

(require '[clojure.string :as s]
         '[clojure.walk :as w]
         '[talispam.utils :as utils]
         '[immuconf.config :as immu]
         '[project-clj.core :as project-clj])

(def config (atom {}))

(def program-name (project-clj/get :name))

(def program-version (project-clj/get :version))

(defn- adjust-paths [c]
  (w/postwalk #(if (string? %) (utils/expand-home %) %) c))

(defn load-config [f]
  (reset! config
          (adjust-paths (immu/load f))))
