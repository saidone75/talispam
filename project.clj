(defproject talispam "0.3.0-SNAPSHOT"
  :description "a software held to act as a charm to avert spam and bring good messages"
  :url "https://github.com/saidone75/talispam"
  :license {:name "MIT"
            :url "https://github.com/saidone75/talispam/blob/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [russellwhitaker/immuconf "0.3.0"]
                 [com.cognitect/transit-clj "1.0.324"]
                 [org.clojure/tools.cli "1.0.194"]
                 [org.jsoup/jsoup "1.13.1"]
                 [com.stuartsierra/frequencies "0.1.0"]]
  :main ^:skip-aot talispam.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :dev {:plugins [[lein-shell "0.5.0"]]}}
  :aliases {"native"
            ["shell"
             "native-image"
             "--report-unsupported-elements-at-runtime"
             "--initialize-at-build-time"
             "-J-Dclojure.compiler.direct-linking=true"
             "-jar" "./target/uberjar/${:uberjar-name:-${:name}-${:version}-standalone.jar}"
             "-H:Name=./target/${:name}"
             "-H:ReflectionConfigurationFiles=./reflectconfig.json"]})
