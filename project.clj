(defproject wordbee_server "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.json "1.0.0"]
                 [com.novemberain/monger "3.5.0"]
                 [org.clojure/core.async "1.3.610"]
                 [io.pedestal/pedestal.service       "0.5.8"]
                 ;; [io.pedestal/pedestal.service-tools "0.5.8"] ;; Only needed for ns-watching; WAR tooling
                 [io.pedestal/pedestal.route         "0.5.8"]
                 [io.pedestal/pedestal.jetty         "0.5.8"]
                 ;; [io.pedestal/pedestal.aws           "0.5.8"] ;; API-Gateway, Lambda, and X-Ray support
                 [org.slf4j/slf4j-simple             "1.7.28"]
                 ]
  :repl-options {:init-ns wordbee-server.core}
  :aot [wordbee-server.core]
  :main wordbee-server.core)
