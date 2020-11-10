(defproject wordbee_server "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [ring/ring-json "0.5.0"]
                 [ring/ring-anti-forgery "1.3.0"]
                 [ring-cors "0.1.13"]
                 [org.clojure/data.json "1.0.0"]
                 [com.novemberain/monger "3.5.0"]
                 [com.fzakaria/slf4j-timbre "0.3.20"]
                 [org.clojure/core.async "1.3.610"]
                 ;; [org.slf4j/slf4j-api "1.7.14"]
                 [http-kit "2.5.0"]
                 [com.taoensso/timbre "5.1.0"]
                 [info.sunng/ring-jetty9-adapter "0.14.0"]
                 [com.taoensso/sente "1.16.0"]
                 [compojure "1.5.2"]
                 [ring "1.8.1"]]
  :repl-options {:init-ns wordbee-server.core}
  :aot [wordbee-server.core]
  :main wordbee-server.core)
