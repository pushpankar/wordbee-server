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

;; Or maybe I should already organize words by its similarity
;; Database should have a list of words in order with a heirarchical
;; clustering
(defn similar-words [request]
  (let [word (:word (:params request))]
    ;; Make a query some where
    ;; It need to return only from the word which has not been added yet
    ;; When I press delete word on the word I shall track that
    ;; return one words or list of words
    ))

(defn next-word [request]
  ;; Maybe this is not required
  ;; or it can return a random word which has not been added to the
  ;; list any module yet
  ;; will first filter words by difficulty keeping the order intact then give the next word in the list
  )

;; The client should ask for a module id
(defn get-module [request]
  (let [id (get-in request [:params :id])]
    ;; @TODO return data with words => IN V2
    (response {:word-list (get (:module data/data) id)})))

;; Create a module and update words definitions
(defn add-module [request]
  (let [update-words (fn [module]
                       ;; Take every word map, find it location and update it
                       (map #(update-in data/data [:database (:word %)] %) module))
        new-module (:body request)
        new-words (map :word new-module)]
    (swap! data/data (update data/data :module conj new-words)) ;; Need to ensure that a word had not been sent for editing twice
    (swap! data/data (update-words new-module))
    (swap! data/data (update data/data :tracked-words into new-words))
    (response {:result "OK"})))

;; This function is required since I can't know from add-module fn
;; which word had been ignored
(defn ignore-word [request]
  (let [word (get-in request [:params :word])]
    (swap! data/data (update data/data :ignored-words conj word))))

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
