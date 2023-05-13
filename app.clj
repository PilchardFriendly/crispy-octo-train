#!/usr/bin/env bb
(require '[babashka.deps :as deps])

(deps/add-deps '{:deps {org.clojars.askonomm/ruuter {:mvn/version "1.3.2"}
                        metosin/malli {:mvn/version "0.11.0"}}})

(require '[org.httpkit.server :as srv]
         '[clojure.java.browse :as browse]
         '[ruuter.core :as r]
         '[clojure.string :as str]
         '[hiccup.core :as h]
         '[cheshire.core :as json]
         '[taoensso.timbre :as timbre]
         '[malli.core :as m]
         '[malli.dev :as dev])

(import '[java.net URLDecoder])

(defmacro spy "Wrapper around timbre/spy" [& rest]
  `(timbre/spy ~@rest))

(let [f (resolve 'unstrument)]
  (when f
        (@f)))

;; (defn debug
;;   ([v d bmf] (debug v d bmf nil))
;;   ([v f fmt amf]
;;    (let [r (f v)]
;;      (spy :debug fmt v)
;;      (when amf (spy :debug amf r))
;;      r)))


;;;;;;;;;;;;;;;;;;;;;;;;;;
;; http serving
;;;;;;;;;;;;;;;;;;;;;;;;;;
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

(def Html :string)
(def HttpStackSuccess [:map [:body :string] [:status :int]])
(def HttpStatus :int)
(defn with-hiccup [f]
  [:schema {:registry {::hiccup [:orn
                                 [:node [:catn
                                         [:name keyword?]
                                         [:props [:? [:map-of keyword? any?]]]
                                         [:children [:* [:schema [:ref ::hiccup]]]]]]
                                 [:primitive [:orn
                                              [:nil nil?]
                                              [:boolean boolean?]
                                              [:number number?]
                                              [:text string?]]]]}}
   (f)])


(defn with-renderable [f] (with-hiccup (fn [] [:schema {:registry {::renderable [:ref ::hiccup]}} (f)])))
(def RenderContent (with-renderable 
                     (fn [] [:or [:sequential [:ref ::renderable]] [:ref ::renderable]])))


(defn render [handler & [status]]
  {:status (or status 200)
   :body handler})
(m/=> render [:function
              [:=> [:cat RenderContent] HttpStackSuccess]
              [:=> [:cat RenderContent HttpStatus] HttpStackSuccess]])

(def ClientEvent [:map
                  [:newBoard [:map [:id :uuid]]]])
(def UrlPath [:string])
(def Redirection [:map
                  [:location UrlPath]
                  [:event {:optional true} ClientEvent]])
(def RedirectionStatus [:enum {:optional true} 302 303])
(def HttpStackRedirection [:map
                           [:status {:default 303} :int]
                           [:body nil?]
                           [:headers [:map ["Location" UrlPath] ["HX-Redirect" UrlPath] ["HX-Trigger" {:optional true} :string]]]])

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


(def doctype  "<!DOCTYPE html>")

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
  (h/html (list 
           [:head [:title title] htmx-script-fragment]
           [:body body])))
(m/=> page [:=> [:cat [:map [:title :string] [:body RenderContent]]] :string])


(defn fragment [content]
  (h/html [:div (list content)]))
(m/=> fragment [:=> [:cat RenderContent] Html])
;;;
;;; post -> new -> +id -> board-markup['id]
;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cells
;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Board Stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;
(def Board (m/schema
            [:map [:id :uuid]
             [:cells [:vector [:vector :boolean]]]
             [:version {:optional true} :string]]))

(m/=> board-model-new [:=> [:cat] Board])
(defn board-model-new []
  (let [row' #(vec (boolean-array 3 false))
        model {:id (random-uuid)
               :cells (vec (repeatedly 3 row'))}]
    model))



(def Boards (m/schema [:map-of :uuid Board]))

(defonce boards (atom {}))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}

(m/=> boards-state-new [:=> [:cat] Board])
(defn boards-state-new []
  (let [board' (spy (board-model-new))]
    (swap! boards #(assoc %1 (:id board') board'))
    board'))

(m/=> boards-state-get [:=> [:cat :uuid] Board])
(defn boards-state-get [id]
  (get @boards id))

(def BoardFragment [:map [:id :uuid] [:cells [:vector [:vector :boolean]]]])

(m/=> board-fragment [:=> [:cat BoardFragment] RenderContent])
(defn board-fragment [{:keys [id cells]}]
  (let [cells-td (fn [c] [:td c])
        cells-tr (fn [cs] (->> cs
                              (map cells-td)
                              (into [:tr])))
        cells-trs (->> cells (map cells-tr) spy)]
    (list [:div (str "Board " id)]
          [:table (into [:tbody] cells-trs)])))

(m/=> board-template [:=> [:cat BoardFragment] RenderContent])
(defn board-template [model]
  (-> model
      board-fragment))

(def RedirectionForBoard [:map [:id :uuid]])

; This bit is cool - we can send an event back to the UI, and have HTML send it to listeners

(m/=> redirection-for-board [:=> [:cat RedirectionForBoard] Redirection])
(defn redirection-for-board [{:keys [id]} ]
  {:location (str "/board/" id) :event {:newBoard {:id id}}})

(m/=> boards-post [:=> [:cat :any] Redirection])
(defn boards-post [_]
  (-> (boards-state-new)
      redirection-for-board
      redirect ))

(m/=> boards-get [:=> [:cat BoardFragment] Html])
(defn boards-get [req]
  (-> {:fragment "board-index" :title "Boards" :body (board-template req)}
      page
      render))

(def GetBoardRequest [:map [:params [:map [:id :string]]]])

(defn board-get [req]
  (let [mk-page (fn [f] {:fragment "board-get" :title "Board" :body f})]
   (-> req
      (get-in [:params :id])
      parse-uuid
      boards-state-get
      board-template 
      mk-page
      page
      render)))



(defn- board-new-action-fragment []
  [:button {:hx-post "/board" :hx-swap "outerHTML"} "New game"])

(def board-routes [{:path "/board"
                    :method :get
                    :name :board-index
                    :response boards-get}
                   {:path "/board"
                    :method :post
                    :name :board-post
                    :response boards-post}
                   {:path "/board/:id"
                    :method :get
                    :name :board-get
                    :response board-get}])

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; App Stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn app-template  [_]
  (page {:title "Htmx Game of Life3"
         :body (board-new-action-fragment)}))

(defn app-index [{:keys [query-string headers]}]
  (let [filter (parse-query-string query-string)
        ajax-request? (get headers "hx-request")]
    (render 
     (if (and filter ajax-request?)
      (fragment (list []))
      (app-template filter)))))

(def app-routes [{:path "/"
                  :method :get
                  :response app-index}])
;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Routes
;;;;;;;;;;;;;;;;;;;;;;;;;;

(def routes (flatten [app-routes board-routes]))



;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Config
;;;;;;;;;;;;;;;;;;;;;;;;;;

(def port 3000)

;;;;;;;;;;;;;;;;;;;;;;;;;;ß
;; Server
;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce server (atom nil))


(defn start []
  (when (nil? @server)
    (let [url (str "http://localhost:" port "/")
          shutdown (srv/run-server #(r/route routes %) {:port port})]
      (reset! server shutdown)
      (println "serving" url)
      (browse/browse-url url)
      {:server shutdown})))


(defn stop []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn restart []
  (stop)
  (swap! boards (constantly {}))
  (start))

(defn instrument [] (dev/start!))
(defn unstrument [] (dev/stop!))
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn restrument [] (unstrument) (instrument))

(when (= *file* (System/getProperty "babashka.file"))
  (start)
  (@promise))

(instrument)
