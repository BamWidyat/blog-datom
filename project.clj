(defproject blog-datom "0.0.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 ;;[org.eclipse.jetty/jetty-client "9.4.0.v20161208"]
                 [io.pedestal/pedestal.service "0.5.2"]

                 ;; Remove this line and uncomment one of the next lines to
                 ;; use Immutant or Tomcat instead of Jetty:

                 [io.pedestal/pedestal.jetty "0.5.2"]
                 ;; [io.pedestal/pedestal.immutant "0.5.2"]
                 ;; [io.pedestal/pedestal.tomcat "0.5.2"]

                 [ch.qos.logback/logback-classic "1.1.8" :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.22"]
                 [org.slf4j/jcl-over-slf4j "1.7.22"]
                 [org.slf4j/log4j-over-slf4j "1.7.22"]
                 [org.slf4j/slf4j-simple "1.7.21"]
                 [hiccup "1.0.5"]
                 [com.datomic/datomic-pro "0.9.5561"]
                 #_[com.datomic/clj-client "0.8.606" :exclusions [org.eclipse.jetty/jetty-client
                                                                org.eclipse.jetty/jetty-http
                                                                org.eclipse.jetty/jetty-util]]
                 ]

  :min-lein-version "2.0.0"
  :resource-paths ["config", "resources"]
  ;; If you use HTTP/2 or ALPN, use the java-agent to pull in the correct alpn-boot dependency
  ;:java-agents [[org.mortbay.jetty.alpn/jetty-alpn-agent "2.0.5"]]
  :profiles {:dev {:aliases {"run-dev" ["trampoline" "run" "-m" "blog-datom.server/run-dev"]}
                   :dependencies [[io.pedestal/pedestal.service-tools "0.5.2"]]}
             :uberjar {:aot [blog-datom.server]}}
  :main ^{:skip-aot true} blog-datom.server)
