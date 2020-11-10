(ns wordbee-server.core
  (:use [org.httpkit.server :only [run-server]])
  (:require ;[ring.adapter.jetty9 :as jetty]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.core :refer [POST GET defroutes]]
            [ring.middleware.reload :refer [wrap-reload]]
            [wordbee-server.db :as db]
            [clojure.walk :as walk]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]

            [wordbee-server.game :as game]
            )
  (:gen-class))

;; utils
(defn parse-module [module]
  (try (-> module (subs 1) Integer/parseInt (* 1000))
       (catch Exception ex "all")))


;; This function is not at all used
;; I am infering last used word from tracked list
(defn ignore-word [request]
  (let [word (get-in request [:body "word"])]
    ;; (reset! data/data (update @data/data :ignored-words conj word))
    (response {:result "OK"})))


(defn surrounding-words [word]
  (let [word-index (.indexOf db/ordered-words word)
        start (max 0 (- word-index 5))
        end (min (+ word-index 6) (count db/ordered-words))]
    (subvec db/ordered-words start end)))

;; get word from the database and attach surrounding words
(defn get-word [request]
  (let [word (get-in request [:body "word"])]
    (response (assoc (db/get-word word) :surrounding-words (surrounding-words word)))))

;; This is a wrapper for get-word
(defn next-word-api [request]
  (let [module (parse-module (get-in request [:body "path"]))
        word (or (get-in request [:body "word"]) (db/last-word module))
        word-defn (db/next-word word)]
    (response (assoc word-defn :surrounding-words (surrounding-words (:word word-defn))))))


;; The client should ask for a module id
;; @TODO Need to be updated
(defn get-module [request]
  (let [id (get-in request [:body "id"])
        module-words (db/module-words id)]
    (response {:word-list (map db/get-word module-words)})))


(defn save-word [request]
  (let [module (parse-module (get-in request [:body "path"]))
        word-data (:body request)
        word (get word-data "word")]
    (db/update-word (walk/keywordize-keys word-data))
    (db/add-to-module word module)
    (response {:result "OK"})))


(defn list-modules [_]
  (let [module-names (db/module-names)]
    (response {:result "OK"
               :data (map (fn [key] {:key key :words (db/module-words key)}) module-names)})))

(defn modules-info [_]
  (let [module-names (db/module-names)]
    (response {:result "OK"
               :data (reduce #(assoc %1 %2 (db/module-words %2)) {} module-names)})))


(defroutes routes
  (POST "/get-module" [] get-module)
  (POST "/save-word" [] save-word)
  (POST "/get-word" [] get-word)
  (POST "/next-word" [] next-word-api)
  (POST "/list-modules" [] list-modules)
  (POST "/modules-info" [] modules-info)

  ;; Game routes
  (GET "/new-game" req (game/new-game req))
  (GET  "/chsk" req (game/ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (game/ring-ajax-post                req))
  )

(def app
  (-> routes
      (wrap-cors    :access-control-allow-methods #{:get :post :delete :options}
                    :access-control-allow-headers #{:accept :content-type}
                    :access-control-allow-origin [#".*"])
      wrap-keyword-params
      wrap-params
      wrap-session
      wrap-json-body
      wrap-json-response
      ;; wrap-anti-forgery
      wrap-reload
      ))

(defn start-web-server! []
  ;(jetty/run-jetty app {:port 3000 :ssl? true :ssl-port 8443 :keystore "keystore.jks" :key-password "199540" :join? true}))
  (run-server app {:port 3000}))

(defn start! []
  (db/init-db!)
  (game/start-router!)
  (start-web-server!))

(defn -main
  "Entry point"
  []
  (start!)
  )
