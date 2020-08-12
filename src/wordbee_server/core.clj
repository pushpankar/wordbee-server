(ns wordbee-server.core
  (:require [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.core :refer [GET POST defroutes]]
            [ring.middleware.reload :refer [wrap-reload]]
            [wordbee-server.data :as data]))

(data/init)

(defn get-word [request]
  (let [word (:word (:params request))]
    (get-in data/data [:database word])))


(defn next-word [request]
  (let [word (get-in request [:params "word"])
        level2-words (filterv #(= (get-in @data/data [:difficulty (keyword %)]) 2) (:all-words @data/data))
        word-index (.indexOf level2-words word)]
    (response {:word (get level2-words (+ 1 word-index))
               :sorrounding (subvec level2-words (- word-index 5) (+ word-index 5))})))

;; I also need to return sorrounding words

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
  (let [word (get-in request [:params "word"])]
    (reset! data/data (update @data/data :ignored-words conj word))
    (response {:result "OK"})))

(defroutes routes
  (POST "/get-module" [] get-module)
  (POST "/add-module" [] add-module)
  (POST "/next-word" [] next-word)
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
