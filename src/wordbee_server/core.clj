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


(defn next-word [word]
  (let [level2-words (:level-2-words @data/data)
        word-index (+ (.indexOf level2-words word) 1)
        data (get-in @data/data [:database (keyword (get level2-words word-index))])]
    (update data :examples #(sort-by count %))))


;; This function is required since I can't know from add-module fn
;; which word had been ignored
(defn ignore-word [request]
  (let [word (get-in request [:body "word"])]
    (reset! data/data (update @data/data :ignored-words conj word))
    (response {:result "OK"})))


(defn surrounding-words [word]
  (let [level2-words (:level-2-words @data/data)
        word-index (+ (.indexOf level2-words word) 1)
        start (max 0 (- word-index 5))
        end (min (+ word-index 6) (count level2-words))]
    (subvec level2-words start end)))


(defn next-word-api [request]
  (let [word (or (get-in request [:body "word"]) (-> (:module @data/data) last last))]
    (response (assoc (next-word word) :surrounding-words (surrounding-words word)))))


;; The client should ask for a module id
(defn get-module [request]
  (let [id (get-in request [:body "id"])
        module-words (get (:module @data/data) id)]
    (println id)
    (response {:word-list (map #(get-in @data/data [:database (keyword %)]) module-words)})))


(defn save-word [request]
  (let [word-data (:body request)
        word (get word-data "word")]
    (reset! data/data (update @data/data :module conj word))
    (reset! data/data (assoc-in @data/data [:database (keyword word)] word-data))
    (response {:result "OK"})))


(defn list-modules [_]
  (response {:result "OK"
             :data (:module @data/data)}))

(defroutes routes
  (POST "/get-module" [] get-module)
  (POST "/save-word" [] save-word)
  ;; (POST "/add-module" [] add-module)
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
