(ns wordbee-server.data
  (:require [clojure.data.json :as json]
            [clojure.string :as str]))


(defn load-data [path]
  (json/read-str (slurp path) :key-fn keyword))

(defn prep []
  (let [difficulty (load-data "resources/difficulty.json")
        ordered-by-meaning (str/split (slurp "resources/ordered_words.txt") #"\n")
        synonyms (load-data "resources/synonyms.json")
        examples (load-data "resources/examples.json")
        meanings (json/read-str (slurp "resources/word-meaning.json"))
        data {:module [[]]
              :difficulty difficulty
              :all-words ordered-by-meaning
              :level-2-words (filterv #(= (get difficulty (keyword %)) 2) ordered-by-meaning)
              :database {}}]
    (reduce #(assoc-in %1 [:database %2] {:word %2
                                          :meaning (set (get meanings %2))
                                          :synonyms (set (get synonyms (keyword %2)))
                                          :examples (set (get examples (keyword %2)))})
            data
            ordered-by-meaning)))


(def data (atom {:module [[]] ; is a list of list
                 :difficulty {}
                 :level-2-words #{}
                 :all-words [] ;; It is ordered by similarity
                 ;; But this will be very convenient
                 :database {:word {:word :word ;; This is for convienence
                                   :meanings [:the-meanings] ;; This should be a set but api can't transmit set
                                   :synonyms [:the-synonyms]
                                   :examples [:the-examples]}}}))

(defn init []
    (reset! data (load-data "resources/database.json")))

(defn dump-data [data]
  (spit "resources/modules.json" (json/write-str data)))
