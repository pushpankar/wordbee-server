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
              :ignored-words #{}
              :all-words ordered-by-meaning
              :level-2-words (filterv #(= (get difficulty (keyword %)) 2) ordered-by-meaning)
              :database {}}]
    (reduce #(assoc-in %1 [:database %2] {:word %2
                                          :meaning (get meanings %2)
                                          :synonyms (get synonyms (keyword %2))
                                          :examples (get examples (keyword %2))})
            data
            ordered-by-meaning)))


(def data (atom {:module [[]] ; is a list of list
                 :ignored-words #{}
                 :difficulty {}
                 :level-2-words #{}
                 :all-words [] ;; It is ordered by similarity
                 :tracked-words #{} ;; This can be inferred from module list
                 ;; But this will be very convenient
                 :database {:word {:word :word ;; This is for convienence
                                   :meanings [:the-meanings] ;; This should be a set but api can't transmit set
                                   :synonyms [:the-synonyms]
                                   :examples [:the-examples]}}}))

(defn init []
  (let [mapping (load-data "resources/database.json")]
    (reset! data (reduce #(update %1 %2 set) mapping [:ignored-words :tracked-words :level-2-words]))))

(defn dump-data [data]
  (spit "resources/modules.json" (json/write-str data)))
