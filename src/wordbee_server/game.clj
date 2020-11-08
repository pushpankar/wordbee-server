(ns wordbee-server.game)

;; pool of users
(def global-pool (atom []))

(defn add-user [userid]
  (swap! global-pool (conj @global-pool userid)))
