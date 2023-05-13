#!/usr/bin/env bb
(require '[babashka.deps :as deps])

(deps/add-deps '{:deps {org.clojars.askonomm/ruuter {:mvn/version "1.3.2"}}})

(require '[org.httpkit.server :as srv]
         '[clojure.java.browse :as browse]
         '[ruuter.core :as r] 
         '[clojure.string :as str]
         '[hiccup.core :as h]
         '[cheshire.core :as json]
         '[taoensso.timbre :as timbre])

(import '[java.net URLDecoder])

(defmacro spy "Wrapper around timbre/spy" [& rest] 
  `(timbre/spy ~@rest))

(defn debug
  ([v d bmf] (debug v d bmf nil))
  ([v f fmt amf]
   (let [r (f v)]
     (spy :debug fmt v)
     (when amf (spy :debug amf r))
     r)))
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


(defn render [handler & [status]]
  {:status (or status 200)
   :body (h/html handler)})
(defn redirect [{:keys [location event]} & [status]]
  (let [location' (str location)
        trigger (when event (json/generate-string event))
        headers {"Location" location'
                 "HX-Redirect" location'}
        headers' (if trigger (assoc headers "HX-Trigger" trigger) headers)
        ]
    {:status (or status 303)
     :headers headers'
     :body nil}))

(def doctype "<!DOCTYPE html>")

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
  (spy (list doctype 
        (h/html 
         [:head [:title title] 
          htmx-script-fragment 
          [:body (spy body)]]))))

;;;
;;; post -> new -> +id -> board-markup['id]
;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cells
;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Board Stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn board-model-new [] 
  (let [row' #(vec (boolean-array 3 false))]
   {:id (random-uuid)
   :cells (vec (repeatedly 3 row'))}))
  
(defonce boards (atom {}))

(defn boards-state-new []
  (let [board' (spy (board-model-new))]
    (swap! boards #(assoc %1 (:id board') board'))
    board'))

(defn boards-state-get [id]
  (spy id)
  (get (spy @boards) id))

(defn board-fragment [{:keys [id cells]}]
  (let [cells-td #(vec [:td (str %1)])
        cells-tr #(vec [:tr (map cells-td %1)])]
   (list [:div (str "Board " id) ] 
         [:table [:tbody (map cells-tr cells)]])))

(defn board-template [model]
  (-> model
      (debug board-fragment "model" "fragment")
      h/html))

(defn boards-post [req]
  (spy req)
  (-> (boards-state-new)
      :id
      ;;; This bit is cool - we can send an event back to the UI, and have HTML send it to listeners
      (debug #(hash-map :location (str "/board/" %1) :event {:newBoard {:id %1}}) "id" "redirect")
      (debug redirect "redirect" "response")))

(defn boards-get [req]
  (render (page {:fragment "board-index" :title "Board" :body (board-template req)})))

(defn board-get [req]
  (-> req
      (debug :params "request")
      (debug :id "params")
      parse-uuid
      (debug boards-state-get "id")
      (debug board-template "board" "html") 
      render))

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
         :body (h/html (board-new-action-fragment))}))

(defn app-index [{:keys [query-string headers]}]
  (let [filter (parse-query-string query-string)
        ajax-request? (spy :info "hx-request" (get headers "hx-request"))]
    (if (and filter ajax-request?)
      (render (list []))
      (render (app-template filter)))))

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

(when (= *file* (System/getProperty "babashka.file"))
  (start)
  (@promise))
