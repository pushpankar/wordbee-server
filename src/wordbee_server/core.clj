(ns wordbee-server.core
  (:require [ring.adapter.jetty9 :as jetty]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.core :refer [POST GET defroutes]]
            [ring.middleware.reload :refer [wrap-reload]]
            [wordbee-server.db :as db]
            [clojure.walk :as walk]
            [taoensso.sente :as sente]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [taoensso.sente.server-adapters.jetty9 :refer (get-sch-adapter)])
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


;;; Add this: --->
(let [{:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket! (get-sch-adapter) {})]

  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids) ; Watchable, read-only atom
  )



(defroutes routes
  (POST "/get-module" [] get-module)
  (POST "/save-word" [] save-word)
  (POST "/get-word" [] get-word)
  (POST "/next-word" [] next-word-api)
  (POST "/list-modules" [] list-modules)
  (POST "/modules-info" [] modules-info)
  ;;; Add these 2 entries: --->
  (GET  "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post                req))
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
      wrap-json-response))

(def reloadable-app
  (wrap-reload #'app))

(defn -main
  "Entry point"
  []
  (jetty/run-jetty reloadable-app {:port 3000 :ssl? true :ssl-port 8443 :keystore "keystore.jks" :key-password "199540" :join? true}))
