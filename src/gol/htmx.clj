(ns gol.htmx
  (:require [cheshire.core :as json]
            [dev.debug :as debug :refer [spy]]
            [malli.core :as m]
            [hiccup2.core :as h]
            [hiccup.compiler :as hc]
            [dev.monitoring :as mon]
            [gol.schemas :as schemas :refer
             [Html
              HttpStackRedirection 
              Redirection 
              RedirectionOptions
              RenderContent]]))


(def ^:private hx-request-header "hx-request")
(def ^:private http-location-header "Location")
(def ^:private hx-redirect-header "HX-Redirect")
(def ^:private hx-trigger-header "HX-Trigger")

(defn hx-request? [{:keys [headers]}] (get headers hx-request-header))
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn redirect [{:keys [location]}  & {:keys [status event] :or {status 303 event nil}}]
  (let [location' (str location)
        trigger (-> (when event (json/generate-string event)) spy)
        headers {http-location-header location'
                 hx-redirect-header location'}
        headers' (if trigger (assoc headers hx-trigger-header trigger) headers)]
    {:status (or status 303)
     :headers headers'
     :content nil}))
;; (m/=> redirect
;;       [:function
;;        [:=> [:cat Redirection RedirectionOptions] HttpStackRedirection]])
    ;;    [:=> [:cßat Redirection RedirectionOptions] HttpStackRedirection]])

(def htmx-script-fragment
            ;; <script src= "https://unpkg.com/htmx.org@1.9.2" 
            ;;       integrity= "sha384-L6OqL9pRWyyFU3+/bjdSri+iIphTN/bvYyM37tICVyOJkWZLpP2vGn6VUEXgzg6h" 
            ;;       crossorigin= "anonymous" ></script>
  (let [src "https://unpkg.com/htmx.org@1.9.2"
        integrity "sha384-L6OqL9pRWyyFU3+/bjdSri+iIphTN/bvYyM37tICVyOJkWZLpP2vGn6VUEXgzg6h"
        cross-origin "anonymous"]
    [:script {:src src
              :integrity integrity
              :crossorigin cross-origin}]))

(defn page [{:keys [title content]}]
  (-> (list
       [:head [:title title] htmx-script-fragment]
       [:body content])
      h/html
      hc/render-html))
(comment m/=> page [:=> [:cat [:map [:title {:optional true} :string] [:content RenderContent]]] :string])

(defn html-layout [_ model]
  (page model))

(defn render [handler & {:keys [status event]
                         :or {status 200 event nil}}]
  (let [trigger (when event (json/generate-string event))
        init (if trigger {:headers {"HX-Trigger" trigger}} {})]
    (into init 
               {:status status
                :body handler})))

;; (m/=> render [:function
;;               [:=> [:cat RenderContent] HttpStackSuccess]
;;               [:=> [:cat RenderContent RenderFlags] HttpStackSuccess]])

(defn fragment [content]
  (hc/render-html [:div (list content)]))

(comment m/=> fragment [:=> [:cat RenderContent] Html])

;; (m/=> handle-view [:=> [:cat (schemas/view :any)] [(schemas/request-handler Html)]])
(defn handle-view [view] 
  (fn [req]
    (let [{:keys [template model]} (view req)
          model' (template req {:model model})]
      (-> (html-layout req model')
          render))))
(mon/instrument)