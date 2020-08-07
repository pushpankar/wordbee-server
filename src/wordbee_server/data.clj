(ns wordbee-server.data
  (:require [clojure.data.json :as json]))

(def id->sent
  (json/read-str (slurp "resources/id2sent.json")))

(def word->sentids
  (json/read-str (slurp "resources/word2sentid.json")))

(defn word->sent [word]
  (let [sent_ids (get word->sentids word)
        sent_id (rand-nth sent_ids)]
    (get id->sent (str sent_id))))
