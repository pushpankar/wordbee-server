(ns wordbee-server.data
  (:require [clojure.data.json :as json]
            [clojure.string :as str]))


(defn load-data [path]
  (json/read-str (slurp path)))

(defn prep []
  (let [difficulty (load-data "resources/difficulty.json")
        ordered-by-meaning (str/split (slurp "resources/ordered_words.txt") #"\n")
        synonyms (load-data "resources/synonyms.json")
        examples (load-data "resources/examples.json")
        meanings (json/read-str (slurp "resources/word-meaning.json"))
        data {:module [[]]
              :difficulty difficulty
              :ingored-words #{}
              :all-words ordered-by-meaning
              :database {}}]
    (reduce #(assoc-in %1 [:database %2] {:word %2
                                          :meaning (get meanings %2)
                                          :synonyms (get synonyms %2)
                                          :examples (get examples %2)})
            data
            ordered-by-meaning)))


(def data (atom {:module [[:word]] ; is a list of list
                 :ignored-words #{:words}
                 :all-words [] ;; It is ordered by similarity
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


;; (defn word->sent [word]
;;   (let [sent_ids (get word->sentids word)
;;         sent_id (rand-nth sent_ids)]
;;     (get id->sent (str sent_id))))
