(ns wordbee-server.ws
  (:require [clojure.core.async :as async :refer [go go-loop <! >!]]
            [io.pedestal.log :as log]
            [io.pedestal.http.jetty.websockets :as ws]
            [clojure.edn :as edn]
            [clojure.core.match :refer [match]]
            [nano-id.core :refer [nano-id]]))


(def ws-clients (atom {}))
(def pairs (atom {}))

(def random-pool (async/chan 10))

;; utils
(defn send-msg
  "Wrapper function for finding client and sending message"
  [userid msg]
  (async/put! (get @ws-clients userid) (pr-str msg)))

;; Pairs users for the game
(go-loop []
  (let [user1 (<! random-pool)
        user2 (<! random-pool)]
    (swap! pairs assoc user1 user2 user2 user1)
    (send-msg user1 [:paired {:opponent user2}])
    (send-msg user2 [:paired {:opponent user1}])))

;; Connection clean
(defn close-later
  "A cleanup function without this global pairs will keep
  increasing in size. There will be nothing to worry about
  even if user disconnects without notification."
  [session-id send-ch]
  (go
    (<! (async/timeout 300000))
    (swap! ws-clients dissoc session-id)
    (swap! pairs dissoc session-id)))

(defn new-ws-client
  "New session handler"
  [ws-session send-ch]
  (let [sessionid (nano-id)]
    (async/put! send-ch (pr-str [:connected sessionid]))
    (swap! ws-clients assoc sessionid send-ch)
    (close-later sessionid send-ch)))

(defmulti msg-handler first)

(defmethod msg-handler :default [params]
  (println params))

(defmethod msg-handler :pair-me [[event userid msg]]
  (async/put! random-pool userid))

(defmethod msg-handler :reconnect [[event userid msg]]
  (swap! ws-clients update (:old-id msg) (get @ws-clients userid)))

(defmethod msg-handler :ping-pong [[_ userid msg]]
  (async/go
    (println "Ping-pong")
    (<! (async/timeout 3000))
    (send-msg userid  [:ping-pong {:msg "Stop ping pong"}])))

(defn close-handler [code msg]
  (match [(edn/read-string msg)]
         [{:userid userid}] (swap! ws-clients dissoc userid)
         :else nil)
  (log/info :msg "WS closed: " :reason msg :code code)
  )

(def ws-paths
  {"/ws" {:on-connect (ws/start-ws-connection new-ws-client)
          :on-text    #(-> % edn/read-string msg-handler)
          :on-binary  (fn [payload offset length] (log/info :msg "Binary Message!" :bytes payload))
          :on-error   (fn [t] (log/error :msg "WS Error happened" :exception t))
          :on-close   close-handler}})


(defn context-configurator-fn [inp]
  (ws/add-ws-endpoints inp ws-paths))
