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
  (mc/update db "dictionary" {:word (:word dictionary)} {$set dictionary}))


(defn next-word [word]
  (let [word-index (+ (.indexOf ordered-words word) 1)
        next-word (get ordered-words word-index)]
    (get-word next-word)))


(defn module-words [k]
  (:words (mc/find-one-as-map db "added" {:key k})))

(defn add-to-module [word]
  (mc/update db "added" {:key "all"} {$set {:words (conj (module-words "all") word)}}))
