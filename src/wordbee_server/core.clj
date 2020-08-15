(ns wordbee-server.core
  (:require [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.core :refer [GET POST defroutes]]
            [ring.middleware.reload :refer [wrap-reload]]
            [wordbee-server.data :as data]
            [clojure.set :as set]))

(data/init)

(defn get-word [request]
  (let [word (:word (:params request))]
    (get-in data/data [:database word])))

;; returns
;; (defn level2-new-words []
;;   (let [ignored-words (:ignored-words @data/data)
;;         tracked-words (:tracked-words @data/data)]
;;     (filterv #(complement (contains? (set/union ignored-words tracked-words) %))
;;              (:level-2-words @data/data))))

(defn next-word [word]
  (let [level2-words (:level-2-words @data/data)
        word-index (+ (.indexOf level2-words word) 1)]
    (get-in @data/data [:database (keyword (get level2-words word-index))])))

(defn surrounding-words [word]
  (let [level2-words (:level-2-words @data/data)
        word-index (+ (.indexOf level2-words word) 1)
        start (max 0 (- word-index 5))
        end (min (+ word-index 5) (count level2-words))]
    (subvec level2-words start end)))

(defn next-word-api [request]
  (let [word (or (get-in request [:body "word"]) (:last-queried @data/data))]
    (response (assoc (next-word word) :surrounding-words (surrounding-words word)))))

;; The client should ask for a module id
(defn get-module [request]
  (let [id (get-in request [:params :id])]
    ;; @TODO return data with words => IN V2
    (response {:word-list (get (:module @data/data) id)})))

;; Create a module and update words definitions
(defn add-module [request]
  (let [update-words (fn [module]
                       ;; Take every word map, find it location and update it
                       (map #(update-in @data/data [:database (:word %)] %) module))
        new-module (:body request)
        new-words (map :word new-module)]
    (reset! data/data (update @data/data :module conj new-words)) ;; Need to ensure that a word had not been sent for editing twice
    (reset! data/data (update-words new-module))
    (reset! data/data (update @data/data :tracked-words into new-words))
    (response {:result "OK"})))

;; This function is required since I can't know from add-module fn
;; which word had been ignored
(defn ignore-word [request]
  (let [word (get-in request [:body "word"])]
    (reset! data/data (update @data/data :ignored-words conj word))
    (response {:result "OK"})))

(defroutes routes
  (POST "/get-module" [] get-module)
  (POST "/add-module" [] add-module)
  (POST "/next-word" [] next-word-api)
  (POST "/ignore-word" [] ignore-word))

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
