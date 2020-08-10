(ns wordbee-server.core
  (:require [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.core :refer [GET POST defroutes]]
            [ring.middleware.reload :refer [wrap-reload]]
            [wordbee-server.data :as data]))

(defn get-word [request]
  (let [word (:word (:params request))]
    (get-in data/data [:database word])))

(defn get-module [_]
  (response (first (get (data/load-data) "raw-modules"))))

(defn add-module [request]
  (let [new-module (:body request)
        new-words (map :word new-module)]
    (update data/data :module conj new-words)
    (map #(update-in data/data [:database (:word %)] %) new-module) ;; Take every word map, find it location and update it
    (response {:result "OK"})))

(defroutes routes
  (POST "/get-module" [] get-module)
  (POST "/add-module" [] add-module))

(def app
  (-> routes
      (wrap-cors    :access-control-allow-methods #{:get :post :delete :options}
                    :access-control-allow-headers #{:accept :content-type}
                    :access-control-allow-origin [#".*"])
      wrap-params
      wrap-json-body
      wrap-json-response))

(def reloadable-app
  (wrap-reload #'app))
