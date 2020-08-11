(ns wordbee-server.data
  (:require [clojure.data.json :as json]))

;; Assuming here that data is saved as
;; {:edited_modules: [],
;;  :raw_modules: []}

;; (def id->sent
;;   (json/read-str (slurp "resources/id2sent.json")))

;; (def word->sentids
;;   (json/read-str (slurp "resources/word2sentid.json")))


(def data (atom {:module [[:word]] ; is a list of list
                 :ignored-words #{:words}
                 :tracked-words #{:words} ;; This can be inferred from module list
                 ;; But this will be very convenient
                 :database {:word {:word :word ;; This is for convienence
                                   :meanings [:the-meanings] ;; This should be a set but api can't transmit set
                                   :synonyms [:the-synonyms]
                                   :examples [:the-examples]}}}))

(defn reset [new-data]
  (swap! data new-data))


(defn dump-data [data]
  (spit "resources/modules.json" (json/write-str data)))

(defn load-data []
  (json/read-str (slurp "resources/modules.json")))

;; (defn word->sent [word]
;;   (let [sent_ids (get word->sentids word)
;;         sent_id (rand-nth sent_ids)]
;;     (get id->sent (str sent_id))))
