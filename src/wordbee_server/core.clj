(ns wordbee-server.core)

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))


(defn handler [request]
  {:status 200
   :header {"Content-Type" "text/html"}
   :body (:remote-addr request)})
