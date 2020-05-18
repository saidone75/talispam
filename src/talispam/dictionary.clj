(ns talispam.dictionary
  (:gen-class))

(require '[clojure.string :as s]
         '[talispam.config :as c])

(def dictionary (atom #{}))

(defn init-dictionary []
  (reset! dictionary (set
                      (if (:use (:dictionary c/config))
                        (let [dictionary
                              (map
                               #(s/split (slurp %) #"\n")
                               (map
                                #(str (System/getProperty "user.home") "/" %)
                                (:files (:dictionary c/config))))]
                          (map s/trim-newline (flatten dictionary)))))))
