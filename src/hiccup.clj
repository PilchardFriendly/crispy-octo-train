(ns hiccup
  (:require
   [clojure.test.check.generators :as gen]
   [malli.core :as m]
   [malli.registry :as mr]
   [gol.registry :as reg]))

(declare hiccup)
;;accept the in the recursion reference
(defn- hiccup-elem [mu]
  [:orn
   [:node [:catn
           [:name {:gen/gen gen/keyword} keyword?]
           [:props [:? [:map-of :keyword any?]]]
           [:children [:* mu]]]]
   [:primitive [:orn
                [:nil nil?]
                [:boolean boolean?]
                [:number number?]
                [:text string?]]]])
;; invokes the recursive definition
(defn- hiccup-registry
  ([] (hiccup-registry "hiccup"))
  ([k]
   {:registry
    (-> {} (assoc k (hiccup-elem [:schema [:ref k]])))}))

(defn- hiccup-schema
  ([] (hiccup-schema hiccup))
  ([k] [:schema (hiccup-registry k) k]))

(def hiccup ::hiccup)

(def ^:private registry
  (mr/registry
   {::hiccup (m/schema (hiccup-schema "hiccup"))}))

(reg/register! ::schema registry)