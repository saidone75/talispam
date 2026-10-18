;
; talispam — A charm to avert spam and bring good messages.
;
; Copyright (c) 2020-2026 Saidone
; SPDX-License-Identifier: MIT
;
; See LICENSE in the project root for the full license text.
;

(ns talispam.config
  (:gen-class))

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.walk :as w]
         '[talispam.utils :as utils]
         '[immuconf.config :as immu])

(def config (atom {}))

;; Embed metadata during AOT compilation, including native-image builds.
(defmacro ^:private project-metadata []
  (edn/read-string (slurp (io/resource "talispam.edn"))))

(def ^:private metadata (project-metadata))
(def program-name (:name metadata))
(def program-version (:version metadata))
(def program-description (:description metadata))

(defn- adjust-paths [c]
  (w/postwalk #(if (string? %) (utils/expand-home %) %) c))

(defn load-config [f]
  (reset! config
          (adjust-paths (immu/load f))))
