(ns pilchardfriendly.kaocha.plugins.portal.reporter 
  (:require [kaocha.hierarchy :as hierarchy]
            [pilchardfriendly.kaocha.plugins.portal.system :refer [report->tap>]]))

(defmulti portal-reporter* (fn [_ e] (:type e)) :hierarchy #'hierarchy/hierarchy)
(defmethod portal-reporter* :default [state e]
  (let [e'' (-> e (select-keys [:type :file :line :expected :actual :message :var :ns]))
        e' (cond-> e''
             (:ns e'') (update-in [:ns] ns-name))]
    (swap! state conj e'))
  e)
(defmethod portal-reporter* :begin-test-suite [_ e] e)
(defmethod portal-reporter* :end-test-suite [_ e] e)
(defmethod portal-reporter* :summary [state e]
  (-> state (reset-vals! nil) first report->tap>)
  e)

(defn portal-reporter
  ([]
   (partial portal-reporter (atom (vec nil))))
  ([ref e]
   (portal-reporter* ref e)))

(defn install "Add a reporter to the reporter stack"
  [cfg] (-> cfg (update-in [:kaocha/reporter]
                           (fn [rs] (conj (or rs [])
                                          (portal-reporter))))))
