(ns lcmap.chipmunk.chips
  "Functions for managing chip data."
  (:require [clojure.tools.logging :as log]
            [clojure.spec.alpha :as spec]
            [clojure.spec.test.alpha :as stest]
            [qbits.alia :as alia]
            [qbits.hayt :as hayt]
            [lcmap.chipmunk.db :as db]
            [lcmap.chipmunk.util :as util]
            [lcmap.chipmunk.registry :as registry]
            [lcmap.commons.chip :as commons-chip]))


(defn insert-chip
  "Build query to add chip to layer."
  [{:keys [:layer] :as chip}]
  (->> (select-keys chip [:source :x :y :acquired :data :hash])
       (hayt/values)
       (hayt/insert (keyword layer))))


(defn insert-chip!
  "Add chip to layer."
  [chip]
  (alia/execute db/db-session (insert-chip chip))
  (assoc chip :saved true))


(defn save!
  "Add all chips to layer."
  [chips]
  (into [] (map insert-chip! chips)))


(defn snap
  "Calculate chip's upper-left x/y for arbitrary x/y."
  [params layer]
  (let [x (-> params :x util/numberize)
        y (-> params :y util/numberize)
        [cx cy] (commons-chip/snap x y layer)]
    (assoc params :x cx :y cy)))


(spec/def ::x (spec/conformer util/numberizer))
(spec/def ::y (spec/conformer util/numberizer))
(spec/def ::acquired (spec/conformer util/intervalize))
(spec/def ::ubid string?)
(spec/def ::query (spec/keys :req-un [::x ::y ::acquired ::ubid]))


(defn search
  "Get chips matching query."
  [{:keys [:ubid :x :y :acquired] :as query}]
  (hayt/select (keyword ubid)
               (hayt/where [[= :x x]
                            [= :y y]
                            [>= :acquired (-> acquired bean :start str)]
                            [<= :acquired (-> acquired bean :end str)]])))


(defn search!
  "Get chips matching query; handles snapping arbitrary x/y to chip x/y."
  [params]
  (let [layer (registry/lookup! (:ubid params))
        params (snap params layer)]
    (->> (util/check! ::query params)
         (search)
         (alia/execute db/db-session))))


(spec/fdef lcmap.chipmunk.chips/search! :args (spec/cat :params ::query))
