(ns http.ring 
    (:require
     [malli.core :as m]
     [malli.registry :as mr]
     [gol.registry :as reg]))

(defn- request-schema ([] (m/schema [:map
                                     [:uri :string]
                                     [:request-method [:enum :get :post]]
                                     [:params [:map-of :keyword :any]]])))

(def request ::request-schema)
(def not-found-response ::not-found)

(def ^:private registry
  (mr/registry
   {request (m/schema (request-schema))
    not-found-response (m/schema [:map [:status [:and :int [:= 404]]]])}))

(reg/register! ::schema registry)