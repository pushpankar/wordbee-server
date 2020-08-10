(ns wordbee-server.core
  (:require [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.core :refer [GET POST defroutes]]
            [ring.middleware.reload :refer [wrap-reload]]
            [wordbee-server.data :as data]))


(defn get-module [_]
  (response (first (get (data/load-data) "raw-modules"))))

(defn add-module [request]
  (let [data (data/load-data)]
    (data/dump-data {"edited-modules" (conj (get data "edited-modules") (:body request))
                     "raw-modules" (rest (get data "raw-modules"))})
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
