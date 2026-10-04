(ns talispam.filter-test
  (:require [clojure.test :refer :all]
            [talispam.config :as config]
            [talispam.corpus :as corpus]
            [talispam.db :as db]
            [talispam.filter :as filter]))

(use-fixtures :each
  (fn [test-fn]
    (let [words @db/words, hams @db/total-hams, spams @db/total-spams
          settings @config/config]
      (try
        (db/clear-db)
        (swap! config/config assoc :dictionary {:use false})
        (test-fn)
        (finally
          (reset! db/words words)
          (reset! db/total-hams hams)
          (reset! db/total-spams spams)
          (reset! config/config settings))))))

(deftest concurrent-and-repeatable-training
  (with-redefs [corpus/ham (constantly (repeat 500 "hello hello shared"))
                corpus/spam (constantly (repeat 500 "offer offer shared"))]
    (dotimes [_ 2]
      (filter/learn)
      (is (= {:hello [500 0] :offer [0 500] :shared [500 500]} @db/words))
      (is (= 500 @db/total-hams @db/total-spams)))))

(deftest scoring-uses-distinct-known-words
  (reset! db/words {:hello [10 0] :offer [0 10]})
  (reset! db/total-hams 10)
  (reset! db/total-spams 10)
  (is (= 0.5 (filter/score "")))
  (is (= 0.5 (filter/score "unknown")))
  (is (= (filter/score "offer") (filter/score "offer offer offer")))
  (is (< (filter/score "hello") 0.5 (filter/score "offer"))))

(deftest stable-chi-square-and-fisher
  (let [chi @#'talispam.filter/inverse-chi-square
        fisher @#'talispam.filter/fisher]
    (is (< (Math/abs (- (Math/exp -1.0) (chi 2.0 2))) 1e-12))
    (is (< (Math/abs (- (* 2.0 (Math/exp -1.0)) (chi 2.0 4))) 1e-12))
    (is (< 0.49 (chi 2000.0 2000) 0.51))
    (is (= 0.0 (chi Double/POSITIVE_INFINITY 2)))
    (is (< 0.99 (fisher (repeat 2000 0.5) 2000) 1.0000001))
    (is (= 0.0 (fisher (repeat 2000 0.01) 2000)))))
