(ns talispam.config
  (:gen-class))

(require '[immuconf.config :as immu])

(def config (immu/load (str (System/getProperty "user.home") "/" ".talispam/talispam.cfg.edn")))
