(ns wordbee-server.db
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.operators :refer :all]))

(def conn (mg/connect))
(def db (mg/get-db conn "wordbee"))

(def ordered-words (:words (mc/find-one-as-map db "ordered" {})))


(defn get-word [word]
  (update (mc/find-one-as-map db "dictionary" {:word word}) :_id str))


(defn update-word [dictionary]
  (let [data (select-keys dictionary [:word :meanings :synonyms :examples :difficulty])]
    (mc/update db "dictionary" {:word (:word dictionary)} {$set data})))


(defn next-word [word]
  (let [word-index (+ (.indexOf ordered-words word) 1)
        next-word (get ordered-words word-index)]
    (get-word next-word)))


(defn module-words [k]
  (:words (mc/find-one-as-map db "added" {:key k})))

(defn add-to-module [word module]
  (mc/update db "added" {:key module} {$set {:words (conj (module-words module) word)}}))

(defn last-word [module]
  (let [words (module-words module)]
    (println module)
    (or (-> words last) (get ordered-words (-> module (subs 1) Integer/parseInt (* 1000))))))
