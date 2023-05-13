#!/usr/bin/env bb
(require '[babashka.deps :as deps])

(deps/add-deps '{:deps {org.clojars.askonomm/ruuter {:mvn/version "1.3.2"}}})

(require '[org.httpkit.server :as srv]
         '[clojure.java.browse :as browse]
         '[ruuter.core :as ruuter]
         '[clojure.pprint :refer [cl-format]]
         '[clojure.string :as str]
         '[hiccup.core :as h])

(import '[java.net URLDecoder])

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

(def doctype "<!DOCTYPE html>")
(def htmx-script
            ;; <script src= "https://unpkg.com/htmx.org@1.9.2" 
            ;;       integrity= "sha384-L6OqL9pRWyyFU3+/bjdSri+iIphTN/bvYyM37tICVyOJkWZLpP2vGn6VUEXgzg6h" 
            ;;       crossorigin= "anonymous" ></script>
  (let [src "https://unpkg.com/htmx.org@1.9.2"
        integrity "sha384-L6OqL9pRWyyFU3+/bjdSri+iIphTN/bvYyM37tICVyOJkWZLpP2vGn6VUEXgzg6h"
        cross-origin "anonymous"]
    [:script {:src src
              :integrity integrity
              :crossorigin cross-origin}]))

(defn app-template  [filter]
  (list doctype
        (h/html
         [:head [:title "Htmx Game of Life3"]
          htmx-script]
         [:body [:button {:hx-post "/board" :hx-swap= "outerHTML"}]])))

(defn app-index [{:keys [query-string headers]}]
  (let [filter (parse-query-string query-string)
        ajax-request? (get headers "hx-request")]
    (if (and filter ajax-request?)
      (render (list []))
      (render (app-template filter)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Routes
;;;;;;;;;;;;;;;;;;;;;;;;;;

(def routes [{:path     "/"
              :method   :get
              :response app-index}])



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
          shutdown (srv/run-server #(ruuter/route routes %) {:port port})]
      (reset! server shutdown)
      (println "serving" url)
      (browse/browse-url url)
      {:server shutdown})))


(defn stop []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))


(when (= *file* (System/getProperty "babashka.file"))
  (start)
  (@promise))
