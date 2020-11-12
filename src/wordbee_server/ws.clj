(ns wordbee-server.ws
  (:require [clojure.core.async :as async :refer [go <! >!]]
            [io.pedestal.log :as log]
            [io.pedestal.http.jetty.websockets :as ws]
            [clojure.edn :as edn]
            [nano-id.core :refer [nano-id]]))


(def ws-clients (atom {}))

(defn new-ws-client
  [ws-session send-ch]
  (let [sessionid (nano-id)]
    (async/put! send-ch  (pr-str [:connected sessionid]))
    (swap! ws-clients assoc sessionid send-ch)))


(defmulti msg-handler #(first %))

(defmethod msg-handler :default [params]
  (println params))

(defmethod msg-handler :hello [params]
  (println "hello")
  (println params))

(defmethod msg-handler :ping-pong [[_ userid msg]]
  (async/go
    (println "Ping-pong")
    (<! (async/timeout 3000))
    (async/put! (get @ws-clients userid) (pr-str [:ping-pong {:msg "Stop ping pong"}]))))

(def ws-paths
  {"/ws" {:on-connect (ws/start-ws-connection new-ws-client)
          :on-text   #(-> % edn/read-string msg-handler)
          :on-binary (fn [payload offset length] (log/info :msg "Binary Message!" :bytes payload))
          :on-error (fn [t] (log/error :msg "WS Error happened" :exception t))
          :on-close (fn [num-code reason-text]
                      (log/info :msg "WS Closed:" :reason reason-text))}})


(defn context-configurator-fn [inp]
  (ws/add-ws-endpoints inp ws-paths))
