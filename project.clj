(defproject talispam "0.3.1-SNAPSHOT"
  :description "a Bayesian mail filter"
  :url "https://github.com/saidone75/talispam"
  :license {:name "MIT"
            :url "https://github.com/saidone75/talispam/blob/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [russellwhitaker/immuconf "0.3.0"]
                 [com.cognitect/transit-clj "1.0.329"]
                 [cli-matic "0.4.3"]
                 [org.jsoup/jsoup "1.14.3"]
                 [com.stuartsierra/frequencies "0.1.0"]
                 [tlight/spin "0.0.4"]
                 [jp.ne.tir/project-clj "0.1.7"]
                 [com.github.clj-easy/graal-build-time "0.1.4"]]
  :plugins [[io.taylorwood/lein-native-image "0.3.1"]]
  :main ^:skip-aot talispam.core
  :target-path "target/%s"
  :native-image {:name "talispam"
                 :opts ["-J-Xmx3g"
                        "--report-unsupported-elements-at-runtime"
                        "-H:ReflectionConfigurationFiles=./reflectconfig.json"]}
  :profiles {:uberjar
             {:aot :all
              :jvm-opts ["-Dclojure.compiler.direct-linking=true"
                         "-Dclojure.spec.skip-macros=true"]}})
