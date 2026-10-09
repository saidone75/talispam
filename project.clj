(defproject talispam "0.4.1-SNAPSHOT"
  :description "a Bayesian mail filter"
  :url "https://github.com/saidone75/talispam"
  :license {:name "MIT"
            :url "https://github.com/saidone75/talispam/blob/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.12.6"]
                 [russellwhitaker/immuconf "0.3.0"]
                 [com.cognitect/transit-clj "1.1.363"]
                 [cli-matic "0.5.4"]
                 [org.jsoup/jsoup "1.23.2"]
                 [com.stuartsierra/frequencies "0.1.0"]
                 [tlight/spin "0.0.4"]
                 [com.github.clj-easy/graal-build-time "1.0.6"]]
  :plugins [[io.taylorwood/lein-native-image "0.3.1"]]
  :main ^:skip-aot talispam.core
  :target-path "target/%s"
  :native-image {:name "talispam"
                 :opts ["-J-Xmx3g"
                        "-Os"
                        "-march=compatibility"
                        "-H:ReflectionConfigurationFiles=./reflectconfig.json"
                        "--features=clj_easy.graal_build_time.InitClojureClasses"]}
  :profiles {:uberjar
             {:aot :all
              :jvm-opts ["-Dclojure.compiler.direct-linking=true"
                         "-Dclojure.spec.skip-macros=true"]}})
