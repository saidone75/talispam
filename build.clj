;
; talispam — A charm to avert spam and bring good messages.
;
; Copyright (c) 2020-2026 Saidone
; SPDX-License-Identifier: MIT
;
; See LICENSE in the project root for the full license text.
;

(ns build
  (:require [clojure.edn :as edn]
            [clojure.tools.build.api :as b]))

(def metadata (edn/read-string (slurp "resources/talispam.edn")))
(def lib 'talispam/talispam)
(def class-dir "target/classes")
(def uber-file (format "target/uberjar/talispam-%s-standalone.jar" (:version metadata)))
(def native-file (format "target/talispam-%s" (:version metadata)))
(def basis (delay (b/create-basis {:project "deps.edn" :user nil})))

(defn clean
  "Remove build artifacts."
  [_]
  (b/delete {:path "target"}))

(defn uber
  "Build an AOT-compiled executable uberjar."
  [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src" "resources"] :target-dir class-dir})
  (b/write-pom {:class-dir class-dir :lib lib :version (:version metadata)
                :basis @basis :src-dirs ["src"]
                :pom-data [[:description (:description metadata)]
                           [:url (:url metadata)]
                           [:licenses [:license
                                       [:name (get-in metadata [:license :name])]
                                       [:url (get-in metadata [:license :url])]]]]})
  (b/compile-clj {:basis @basis :class-dir class-dir
                  :compile-opts {:direct-linking true}
                  :java-opts ["-Dclojure.spec.skip-macros=true"]})
  (b/uber {:class-dir class-dir :uber-file uber-file :basis @basis
           :main 'talispam.core})
  (println "Created" uber-file))

(defn native-image
  "Build a native executable (requires GraalVM native-image on PATH)."
  [{:keys [optimization march] :or {optimization "s" march "compatibility"}}]
  (uber nil)
  (let [result (b/process {:command-args
                           ["native-image" "-J-Xmx3g"
                            (str "-O" optimization) (str "-march=" march)
                            "--features=clj_easy.graal_build_time.InitClojureClasses"
                            "-jar" uber-file native-file]})]
    (when-not (zero? (:exit result))
      (throw (ex-info "native-image failed" result)))))
