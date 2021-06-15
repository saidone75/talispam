(ns talispam.core-test
  (:require [clojure.test :refer :all]
            [talispam.core :refer :all]
            [com.stuartsierra.frequencies :as freq]))

(set! *warn-on-reflection* true)

(let [config (talispam.config/load-config (talispam.utils/expand-home "~/.talispam/talispam.cfg.edn"))
      dictionary (talispam.dictionary/load-dictionary!)
      db (talispam.db/load-db)
      message (rand-nth (talispam.corpus/ham))]
  ;; generic filter test
  (deftest filter-test
    (let [score (talispam.filter/score message)]
      (is (<= 0 score 1)))))
