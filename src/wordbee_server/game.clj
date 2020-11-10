(ns wordbee-server.game
  (:require
   [clojure.core.async :as async :refer (<! >! chan go go-loop)]
   [taoensso.sente :as sente]
   [taoensso.sente.server-adapters.jetty9 :refer (get-sch-adapter)]
   ))


(def active-pool (atom {}))
(def request-pool (chan))

(defn new-game
  "Pairs user"
  [req]
  (go (>! (:user-id req) request-pool))
  (println req)
  {:res "You will be paired"})


;; The pairing function
(go-loop []
  (let [u1 (<! request-pool)
        u2 (<! request-pool)]
    (swap! active-pool (assoc @active-pool u1 u2 u2 u1))))


;; Fetch id of each user from the database
(defn user-id-fn [req]
  (println req)
  1)

(defn disconnect
  "Drop user from the game"
  [_]
  :ok)

;;; Add this: --->
(let [{:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket-server! (get-sch-adapter) {:user-id-fn      user-id-fn
                                                     :csrf-token-fn   nil})]

  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids) ; Watchable, read-only atom
  )


;; Sente event handlers

(defmulti -event-msg-handler
  "Multimethod to handle clojure events"
  :id ; dispatch on event-id
  )

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg) ; Handle event-msgs on a single thread
  ;; (future (-event-msg-handler ev-msg)) ; Handle event-msgs on a thread pool
  )

(defmethod -event-msg-handler
  :default
  [{:as ev-msg :keys [event id ?reply-fn]}]
    (when ?reply-fn
      (?reply-fn {:umatched-event-as-echoed-from-server event})))

(defmethod -event-msg-handler
  :ping-pong
  [{:keys [event id ring-req]}]
  (let [uid (get-in ring-req [:session :ui])
        friend-uid (uid active-pool)]
    (println event id)
    (chsk-send! friend-uid [:ping-pong])))

(defmethod -event-msg-handler :chsk/uidport-open [{:keys [uid client-id]}]
  (println "New connection:" uid client-id))

(defmethod -event-msg-handler :chsk/uidport-close [{:keys [uid]}]
  (println "Disconnected:" uid))


(defonce router_ (atom nil))
(defn  stop-router! [] (when-let [stop-fn @router_] (stop-fn)))
(defn start-router! []
  (stop-router!)
  (reset! router_
    (sente/start-server-chsk-router!
      ch-chsk event-msg-handler)))
