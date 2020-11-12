(ns wordbee-server.ws
  (:require [clojure.core.async :as async]
            [io.pedestal.log :as log]
            [io.pedestal.http.jetty.websockets :as ws]))


(def ws-clients (atom {}))

(defn new-ws-client
  [ws-session send-ch]
  (async/put! send-ch "You are connected")
  (swap! ws-clients assoc ws-session send-ch))

(def ws-paths
  {"/ws" {:on-connect (ws/start-ws-connection new-ws-client)
          :on-text    (fn [msg]
                        (log/info :msg (str "A client sent - " msg)))
          :on-binary (fn [payload offset length] (log/info :msg "Binary Message!" :bytes payload))
          :on-error (fn [t] (log/error :msg "WS Error happened" :exception t))
          :on-close (fn [num-code reason-text]
                      (log/info :msg "WS Closed:" :reason reason-text))}})


(defn context-configurator-fn [inp]
  (ws/add-ws-endpoints inp ws-paths))
