(ns wordbee-server.ws
  (:require [clojure.core.async :as async :refer [go <! >!]]
            [io.pedestal.log :as log]
            [io.pedestal.http.jetty.websockets :as ws]
            [clojure.edn :as edn]))


(def ws-clients (atom {}))

(defn new-ws-client
  [ws-session send-ch]
  (println ws-session send-ch)
  (async/put! send-ch "Hello from server. You are connected")
  ;; You are connect, Here is your session id
  ;; User sends pair me
  ;; On pair send user you have been paired
  (swap! ws-clients assoc ws-session send-ch))


(defmulti msg-handler #(-> % edn/read-string first))

(defmethod msg-handler :hello [params]
  (println "hello")
  (println params))

(defmethod msg-handler :ping-pong [_ data]
  (async/go
    (<! (async/timeout 3000))
    (println "After a second")))

(def ws-paths
  {"/ws" {:on-connect (ws/start-ws-connection new-ws-client)
          :on-text   msg-handler
          :on-binary (fn [payload offset length] (log/info :msg "Binary Message!" :bytes payload))
          :on-error (fn [t] (log/error :msg "WS Error happened" :exception t))
          :on-close (fn [num-code reason-text]
                      (log/info :msg "WS Closed:" :reason reason-text))}})


(defn context-configurator-fn [inp]
  (ws/add-ws-endpoints inp ws-paths))
