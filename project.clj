(defproject talispam "0.3.1-SNAPSHOT"
  :description "a Bayesian mail filter"
  :url "https://github.com/saidone75/talispam"
  :license {:name "MIT"
            :url "https://github.com/saidone75/talispam/blob/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [russellwhitaker/immuconf "0.3.0"]
                 [com.cognitect/transit-clj "1.0.324"]
                 [cli-matic "0.4.3"]
                 [org.jsoup/jsoup "1.14.1"]
                 [com.stuartsierra/frequencies "0.1.0"]
                 [tlight/spin "0.0.4"]
                 [jp.ne.tir/project-clj "0.1.7"]]
  :plugins [[io.taylorwood/lein-native-image "0.3.1"]]
  :main ^:skip-aot talispam.core
  :target-path "target/%s"
  :native-image {:name "talispam"
                 :opts ["--no-server"
                        "-J-Xmx3g"
                        "--report-unsupported-elements-at-runtime"
                        "--initialize-at-build-time"
                        "-H:ReflectionConfigurationFiles=./reflectconfig.json"]}
  :profiles {:uberjar
             {:aot :all
              :jvm-opts ["-Dclojure.compiler.direct-linking=true"
                         "-Dclojure.spec.skip-macros=true"]}})
