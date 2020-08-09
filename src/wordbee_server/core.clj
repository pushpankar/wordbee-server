(ns wordbee-server.core
  (:require [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.core :refer [GET POST defroutes]]
            [ring.middleware.reload :refer [wrap-reload]]
            [wordbee-server.data :as data]))


(defn get-module [request]
  (println request))

(defn add-module [request]
  ;; (data/dump (:body request))
  (println request)
  (response {:res "Ok!"}))

(defroutes routes
  (GET "/module/:id" [] get-module)
  (POST "/add-module" [] add-module))

;; (defn get-sent [word]
;;   (word->sent word))

;; (defn handler [request]
;;   (let [word (get-in request [:params "word"])
;;         sent (get-sent word)]
;;   (response {:sentence sent})))

(def app
  (-> routes
      (wrap-cors    :access-control-allow-methods #{:get :post :delete :options}
                    :access-control-allow-headers #{:accept :content-type}
                    :access-control-allow-origin [#".*"])
      ;; wrap-params
      wrap-json-body
      wrap-json-response))

(def reloadable-app
  (wrap-reload #'app))
