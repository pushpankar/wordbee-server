(ns wordbee-server.core
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.body-params :refer [body-params]]
            [clojure.data.json :as json]
            [io.pedestal.http.route :as route]
            [wordbee-server.db :as db]
            [io.pedestal.test :as test]
            [wordbee-server.ws :as ws]
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

(defn surrounding-words [word]
  (let [word-index (.indexOf db/ordered-words word)
        start (max 0 (- word-index 5))
        end (min (+ word-index 6) (count db/ordered-words))]
    (subvec db/ordered-words start end)))

(def next-word
  {:name :next-word
   :enter (fn [context]
            (let [word (get-in context [:request :path-params :word])
                  new-word (if (= word "dummy") (db/last-word "all") word)]
              (assoc context :result (db/get-word new-word))))})

(def wrap-context
  {:name :wrap-context
   :leave (fn [context]
            (let [word (get-in context [:result :word])]
              (assoc-in context [:result :surrounding-words] (surrounding-words word))))})

(def get-word
  {:name :get-word
   :enter (fn [context]
            (let [word (get-in context [:request :path-params :word])]
              (assoc context :result (db/get-word word))))})

(def get-module
  {:name :get-module
   :enter (fn [context]
            (let [module-id (get-in context [:request :path-params :id])
                  module-words (db/module-words module-id)]
              (assoc context :result (map db/get-word module-words))))})

(def update-word
  {:name :update-word
   :enter (fn [context]
            (let [data (get-in context [:request :json-params])]
              (db/update-word data)
              (db/add-to-module (:word data) "all")
              (assoc context :result (created (:word data)))))})


(def list-modules
  {:name :list-modules
   :enter (fn [context]
            (assoc context :result (db/modules-with-words)))})

(def routes
  (route/expand-routes
   #{["/echo"         :get  echo                                                                    :route-name :echo]
     ["/echo"         :post [(body-params) echo]                                                    :route-name :echo-post]
     ["/word"         :post [(body-params) coerce-body content-neg-intc entity-render update-word]  :route-name :update-word]
     ["/word/:word"   :get  [coerce-body content-neg-intc entity-render get-word]                   :route-name :query-word]
     ["/module/:id"   :get  [coerce-body content-neg-intc entity-render get-module]                 :route-name :get-module]
     ["/modules"      :get  [coerce-body content-neg-intc entity-render list-modules]               :route-name :list-modules]

     ;; Dev apis
     ["/module/:id"   :post echo         :route-name :create-module] ;; I am doing this manually
     ["/next-word/:word"    :get  [coerce-body content-neg-intc entity-render next-word wrap-context] :route-name :next-word]
     }))



(def service-map
   {::http/routes routes
    ::http/type   :jetty
    ::http/port   3000
    ::http/host   "0.0.0.0"
    ::http/container-options {:context-configurator ws/context-configurator-fn}})

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
