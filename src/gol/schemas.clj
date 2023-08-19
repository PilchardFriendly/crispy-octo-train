(ns gol.schemas
  (:require [dev.monitoring :as mon]
            [http.ring :as ring]
            [hiccup :as h :refer [hiccup]]
            [dev.meta :as dev]))

(mon/unstrument)

(defn options-schema [options]
  (let [kv->key-schema (fn [[k v]] [k {:optional true} v])
        kv->arg-schema (fn [[k v]] [:cat [:= k] v])]
    [:altn
     [:map (into [:map] (map kv->key-schema options))]
     [:args
      [:* (into [:alt] (map kv->arg-schema options))]]]))


(def RenderContent [:or
                    [:sequential hiccup]
                    [hiccup]])

(def NewBoardEvent  [:map [:id :uuid]])
(def BoardEvent [:map
                  [:newBoard NewBoardEvent]])

(def UrlPath [:string])
(def Redirection [:map
                  [:location UrlPath]])

(def RedirectionStatus [:int {:default 303}])


(def RedirectionOptions (options-schema {:status RedirectionStatus 
                                         :event [:map-of :any :any]}))

(def HttpStackRedirection [:map
                           [:status :int]
                           [:content nil?]
                           [:headers [:map 
                                      ["Location" UrlPath] 
                                      ["HX-Redirect" UrlPath] 
                                      ["HX-Trigger" {:optional true} :string]]]])

;; (def GetBoardRequest [:map [:params [:map [:id :string]]] [:event {:optional true} BoardEvent]])

(def Html :string)
(def ViewRedirectStatus [:status [:enum 302 303]])
;; (def HttpStackSuccess [:map 
;;                        [:content :string] 
;;                        [:status :int]
;;                        [:headers [:map :string :string]]])
;; (def HttpStatus :int)
;; (def RenderFlags [:map
;;                   [:status {:optional true :default 200} HttpStatus]
;;                   [:event {:optional true}]])
(def HttpStackRequest ring/request)
(defn request-handler [resultSchema & [argSchema]]
  [:function [:=> (into [:cat HttpStackRequest] (or argSchema [])) resultSchema]])

(defn template-result [& eventSpec] 
  [:map
   [:content RenderContent]
   [:status {:optional true} [:enum 200]]
   [:event {:optional true} eventSpec]])

(defn view-template [modelSpec & eventSpec] 
  (request-handler 
   (apply template-result (if eventSpec [eventSpec] []))
   [[:map [:model modelSpec]]]))

(def FragmentResponse RenderContent)
(defn view-fragment [modelSpec]
  [:function [:=> [:cat modelSpec] FragmentResponse]])

(defn view-result [modelSpec & eventSpec] 
  [:orn
   [:template
    [:map
     [:template (view-template modelSpec)]
     [:model modelSpec]]]
   
   [:redirect
    [:map
     ViewRedirectStatus
     [:location UrlPath]
     [:event {:optional true} (if eventSpec eventSpec :any)]]]
   [:content
     (apply template-result (if eventSpec [eventSpec] []))]])

(defn view [modelSpec]
  (request-handler (view-result modelSpec)))

(defn view-action-result [modelSpec eventSpec]
  (:and (view-result modelSpec) [:map [:event eventSpec]]))

;; [:forall :a?]
(defn view-action [modelSpec eventSpec & [argsSchema]]
  (request-handler (view-action-result modelSpec eventSpec) argsSchema))

;; (def registry 
;;   (mr/registry
;;    {
;;     }))
;; (reg/register! ::schema registry)

(dev/annotate)
(mon/instrument)