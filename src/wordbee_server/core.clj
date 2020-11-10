(ns wordbee-server.core
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route])

  (:gen-class))

(defn respond-hello [request]
  {:status 200 :body "Hello"})

;; utils
;; (defn parse-module [module]
;;   (try (-> module (subs 1) Integer/parseInt (* 1000))
;;        (catch Exception ex "all")))


;; ;; This function is not at all used
;; ;; I am infering last used word from tracked list
;; (defn ignore-word [request]
;;   (let [word (get-in request [:body "word"])]
;;     ;; (reset! data/data (update @data/data :ignored-words conj word))
;;     (response {:result "OK"})))


;; (defn surrounding-words [word]
;;   (let [word-index (.indexOf db/ordered-words word)
;;         start (max 0 (- word-index 5))
;;         end (min (+ word-index 6) (count db/ordered-words))]
;;     (subvec db/ordered-words start end)))

;; ;; get word from the database and attach surrounding words
;; (defn get-word [request]
;;   (let [word (get-in request [:body "word"])]
;;     (response (assoc (db/get-word word) :surrounding-words (surrounding-words word)))))

;; ;; This is a wrapper for get-word
;; (defn next-word-api [request]
;;   (let [module (parse-module (get-in request [:body "path"]))
;;         word (or (get-in request [:body "word"]) (db/last-word module))
;;         word-defn (db/next-word word)]
;;     (response (assoc word-defn :surrounding-words (surrounding-words (:word word-defn))))))


;; ;; The client should ask for a module id
;; ;; @TODO Need to be updated
;; (defn get-module [request]
;;   (let [id (get-in request [:body "id"])
;;         module-words (db/module-words id)]
;;     (response {:word-list (map db/get-word module-words)})))


;; (defn save-word [request]
;;   (let [module (parse-module (get-in request [:body "path"]))
;;         word-data (:body request)
;;         word (get word-data "word")]
;;     (db/update-word (walk/keywordize-keys word-data))
;;     (db/add-to-module word module)
;;     (response {:result "OK"})))


;; (defn list-modules [_]
;;   (let [module-names (db/module-names)]
;;     (response {:result "OK"
;;                :data (map (fn [key] {:key key :words (db/module-words key)}) module-names)})))

;; (defn modules-info [_]
;;   (let [module-names (db/module-names)]
;;     (response {:result "OK"
;;                :data (reduce #(assoc %1 %2 (db/module-words %2)) {} module-names)})))

(def routes
  (route/expand-routes
   #{["/greet" :get respond-hello :route-name :greet]}))

(def service-map
   {::http/routes routes
    ::http/type   :jetty
    ::http/port   3000})

;; (defroutes routes
;;   (POST "/get-module" [] get-module)
;;   (POST "/save-word" [] save-word)
;;   (POST "/get-word" [] get-word)
;;   (POST "/next-word" [] next-word-api)
;;   (POST "/list-modules" [] list-modules)
;;   (POST "/modules-info" [] modules-info)

;;   ;; Game routes
;;   (GET "/new-game" req (game/new-game req))
;;   (GET  "/chsk" req (game/ring-ajax-get-or-ws-handshake req))
;;   (POST "/chsk" req (game/ring-ajax-post                req))
;;   )

(defn start! []
  ;; (db/init-db!)
  (http/start (http/create-server service-map)))

(defonce server (atom nil))

(defn start-dev []
  (reset! server (http/start (http/create-server
                              (assoc service-map ::http/join? false)))))

(defn stop-dev []
  (http/stop @server))

(defn restart []
  (stop-dev)
  (start-dev))

(defn -main
  "Entry point"
  []
  (start!)
  )
