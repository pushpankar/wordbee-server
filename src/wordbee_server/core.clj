(ns wordbee-server.core
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.core :refer [POST defroutes]]
            [ring.middleware.reload :refer [wrap-reload]]
            [wordbee-server.db :as db]
            [clojure.walk :as walk])
  (:gen-class))


(defn get-word [request]
  (let [word (:word (:params request))]
    (db/get-word word)))

;; This function is required since I can't know from add-module fn
;; which word had been ignored
(defn ignore-word [request]
  (let [word (get-in request [:body "word"])]
    ;; (reset! data/data (update @data/data :ignored-words conj word))
    (println word)
    (response {:result "OK"})))


(defn surrounding-words [word]
  (let [word-index (.indexOf db/ordered-words word)
        start (max 0 (- word-index 5))
        end (min (+ word-index 6) (count db/ordered-words))]
    (subvec db/ordered-words start end)))


(defn next-word-api [request]
  (let [word (or (get-in request [:body "word"]) (-> (db/module-words "all") last))
        word-defn (db/next-word word)]
    (response (assoc word-defn :surrounding-words (surrounding-words (:word word-defn))))))


;; The client should ask for a module id
;; @TODO Need to be updated
(defn get-module [request]
  (let [id (get-in request [:body "id"])
        module-words (db/module-words "all")]
    (println id)
    (println module-words)
    (response {:word-list []})))
    ;; (response {:word-list (map #(get-in @data/data [:database (keyword %)]) module-words)})))


(defn save-word [request]
  (let [word-data (:body request)
        word (get word-data "word")]
    (db/update-word (walk/keywordize-keys word-data))
    (db/add-to-module word)
    (response {:result "OK"})))


(defn list-modules [_]
  (response {:result "OK"
             :data (db/module-words "all")}))

(defroutes routes
  (POST "/get-module" [] get-module)
  (POST "/save-word" [] save-word)
  (POST "/next-word" [] next-word-api)
  (POST "/list-modules" [] list-modules))

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

(defn -main
  "Entry point"
  []
  (jetty/run-jetty app {:port 3000}))
