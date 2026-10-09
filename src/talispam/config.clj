(ns talispam.config
  (:gen-class))

(require '[clojure.walk :as w]
         '[talispam.utils :as utils]
         '[immuconf.config :as immu])

(def config (atom {}))

;; embed project metadata during compilation
(defmacro ^:private project-metadata []
  (let [[_ project-name version & options]
        (binding [*read-eval* false]
          (read-string (slurp "project.clj")))
        project (apply hash-map options)]
    {:name (name project-name)
     :version version
     :description (:description project)}))

(def ^:private metadata (project-metadata))
(def program-name (:name metadata))
(def program-version (:version metadata))
(def program-description (:description metadata))

(defn- adjust-paths [c]
  (w/postwalk #(if (string? %) (utils/expand-home %) %) c))

(defn load-config [f]
  (reset! config
          (adjust-paths (immu/load f))))