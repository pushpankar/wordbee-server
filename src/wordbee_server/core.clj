(ns wordbee-server.core
  (:require [ring.middleware.json :refer [wrap-json-response]]
            [ring.util.response :refer [response]]
            [ring.middleware.params :refer [wrap-params]]
            ;; [compojure.core :refer [GET defroutes]]
            [ring.middleware.reload :refer [wrap-reload]]
            [wordbee-server.data :refer [word->sent]]))


(defn get-sent [word]
  (word->sent word))

(defn handler [request]
  (let [word (get-in request [:params "word"])
        sent (get-sent word)]
  (response {:sentence sent})))

(def app
  (-> handler
      wrap-params
      wrap-json-response))

(def reloadable-app
  (wrap-reload #'app))
