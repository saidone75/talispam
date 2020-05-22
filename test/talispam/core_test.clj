(ns talispam.core-test
  (:require [clojure.test :refer :all]
            [talispam.core :refer :all]))

(let [config (talispam.config/load-config (talispam.utils/expand-home "~/.talispam/talispam.cfg.edn"))
      dictionary (talispam.dictionary/load-dictionary!)
      db (talispam.db/load-db)
      message (rand-nth (talispam.corpus/ham))]
  (deftest filter
    (let [score (talispam.filter/score message)]
      (is (<= 0 score))
      (is (>= 1 score)))))

