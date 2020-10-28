(ns wordbee-server.db
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.operators :refer :all]))

(def conn (mg/connect))
(def db (mg/get-db conn "wordbee"))

(def ordered-words (:words (mc/find-one-as-map db "ordered" {})))

(defn sort-examples
  "Takes word instance and sorts its examples by length"
  [datum]
  (update datum :examples #(sort-by count %)))

(defn get-word [word]
  (sort-examples (update (mc/find-one-as-map db "dictionary" {:word word}) :_id str)))

(defn update-word [dictionary]
  (let [data (select-keys dictionary [:word :meanings :synonyms :examples :difficulty])]
    (mc/update db "dictionary" {:word (:word dictionary)} {$set data})))

(defn next-word [word]
  (let [word-index (+ (.indexOf ordered-words word) 1)
        next-word (get-word (get ordered-words word-index))]
    (println (:difficulty next-word))
    (if (>(:difficulty next-word) 1)
      next-word
      (recur (:word next-word)))))

(defn module-words [k]
  (:words (mc/find-one-as-map db "modules" {:key k})))

(defn module-names []
  (map #(:key %) (mc/find-maps db "modules")))

(defn add-to-module [word module]
  (mc/update db "added" {:key module} {$set {:words (conj (module-words module) word)}} {:upsert true}))

(defn last-word [module]
  (let [words (module-words module)]
    (or (-> words last) (get ordered-words module))))
