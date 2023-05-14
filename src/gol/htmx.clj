(ns gol.htmx
  (:use [gol.schemas])
  (:require [cheshire.core :as json]
            [malli.core :as m]
            [hiccup2.core :as h]
            [hiccup.compiler :as hc]
            [clojure.string :as str]))
(import '[java.net URLDecoder])

(defn redirect [{:keys [location event]}  & [status]]
  (let [location' (str location)
        trigger (when event (json/generate-string event))
        headers {"Location" location'
                 "HX-Redirect" location'}
        headers' (if trigger (assoc headers "HX-Trigger" trigger) headers)]
    {:status (or status 303)
     :headers headers'
     :body nil}))
(m/=> redirect
      [:function
       [:=> [:cat Redirection] HttpStackRedirection]
       [:=> [:cat Redirection RedirectionStatus] HttpStackRedirection]])

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

(defn page [{:keys [title body]}]
  (-> (list
       [:head [:title title] htmx-script-fragment]
       [:body body])
      h/html
      hc/render-html))
(m/=> page [:=> [:cat [:map [:title :string] [:body RenderContent]]] :string])

(defn render [handler & [status]]
  {:status (or status 200)
   :body handler})
(m/=> render [:function
              [:=> [:cat RenderContent] HttpStackSuccess]
              [:=> [:cat RenderContent HttpStatus] HttpStackSuccess]])

(defn fragment [content]
  (hc/render-html [:div (list content)]))
(m/=> fragment [:=> [:cat RenderContent] Html])

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn parse-body [body]
  (-> body
      slurp
      (str/split #"=")
      second
      URLDecoder/decode))

(defn parse-query-string [query-string]
  (when query-string
    (-> query-string
        (str/split #"=")
        second)))