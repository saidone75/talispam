(ns talispam.core-test
  (:require [clojure.test :refer :all]
            [talispam.core :refer :all]
            [com.stuartsierra.frequencies :as freq]))

(let [config (talispam.config/load-config (talispam.utils/expand-home "~/.talispam/talispam.cfg.edn"))
      dictionary (talispam.dictionary/load-dictionary!)
      db (talispam.db/load-db)
      message (rand-nth (talispam.corpus/ham))]
  ;; generic filter test
  (deftest filter-test
    (let [score (talispam.filter/score message)]
      (is (<= 0 score))
      (is (>= 1 score))))
  ;; stats
  (deftest stats
    (def f-ham (freq/stats (frequencies (map talispam.filter/score (talispam.corpus/ham)))))
    (def f-spam (freq/stats (frequencies (map talispam.filter/score (talispam.corpus/spam)))))
    (println "HAM stats:")
    (clojure.pprint/pprint f-ham)
    (println "SPAM stats:")
    (clojure.pprint/pprint f-spam)))
