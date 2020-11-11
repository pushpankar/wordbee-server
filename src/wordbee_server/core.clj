(ns wordbee-server.core
  (:require [io.pedestal.http :as http]
            [clojure.data.json :as json]
            [io.pedestal.http.route :as route]
            [wordbee-server.db :as db]
            [io.pedestal.test :as test]
            [io.pedestal.http.content-negotiation :as conneg])

  (:gen-class))

(defn response [status body & {:as headers}]
  {:status status :body body :headers headers})


(def ok       (partial response 200))
(def created  (partial response 201))
(def accepted (partial response 202))

(defn not-found []
  (response 404 "Not found \n"))


(def entity-render
  {:name :entity-render
   :leave (fn [context]
            (if-let [item (:result context)]
              (assoc context :response (ok item))))})

(def supported-types ["text/html" "application/edn" "application/json" "text/plain"])

(def content-neg-intc (conneg/negotiate-content supported-types))

(defn accepted-type
  [context]
  (get-in context [:request :accept :field] "text/plain"))

(defn transform-content
  [body content-type]
  (case content-type
    "text/html"        body
    "text/plain"       body
    "application/edn"  (pr-str body)
    "application/json" (json/write-str body)))

(defn coerce-to
  [response content-type]
  (-> response
      (update :body transform-content content-type)
      (assoc-in [:headers "Content-Type"] content-type)))

(def coerce-body
  {:name ::coerce-body
   :leave
   (fn [context]
     (cond-> context
       (nil? (get-in context [:response :headers "Content-Type"]))
       (update-in [:response] coerce-to (accepted-type context))))})

;; Routes
(def echo
  {:name :echo
   :enter (fn [context]
            (let [request (:request context)
                  response (ok context)]
              (assoc context :response response)))})

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


(defn surrounding-words [word]
  (let [word-index (.indexOf db/ordered-words word)
        start (max 0 (- word-index 5))
        end (min (+ word-index 6) (count db/ordered-words))]
    (subvec db/ordered-words start end)))

;; get word from the database and attach surrounding words
;; (defn get-word [request]
;;   (let [word ]
;;     (println (:request request))
;;     (ok (assoc (db/get-word word) :surrounding-words (surrounding-words word)))))

(def get-word
  {:name :get-word
   :enter (fn [context]
            (let [word (get-in context [:request :path-params :word])]
              (assoc context :result (db/get-word word))))})


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
;;     {:status 200
;;      :body (map db/get-word module-words)}))

(def get-module
  {:name :get-module
   :enter (fn [context]
            (let [module-id (get-in context [:request :path-params :id])
                  module-words (db/module-words module-id)]
              (assoc context :result (map db/get-word module-words))))})

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

(def list-modules
  {:name :list-modules
   :enter (fn [context]
            (assoc context :result (db/modules-with-words)))})


(def routes
  (route/expand-routes
   #{["/echo"         :get [coerce-body content-neg-intc echo]          :route-name :echo]

     ["/word"         :post echo                                                      :route-name :update-word]
     ["/word/:word"   :get  [coerce-body content-neg-intc entity-render get-word]     :route-name :query-word]
     ["/module/:id"   :get  [coerce-body content-neg-intc entity-render get-module]   :route-name :get-module]
     ["/module/:id"   :post echo         :route-name :create-module]
     ["/modules"      :get  [coerce-body content-neg-intc entity-render list-modules] :route-name :list-modules]
     }))

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
  (db/init-db!)
  (http/start (http/create-server service-map)))

(defonce server (atom nil))

(defn start-dev []
  (db/init-db!)
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
  (start!))
