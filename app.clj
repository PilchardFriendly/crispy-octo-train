#!/usr/bin/env bb
(require '[org.httpkit.server :as srv]
         '[clojure.java.browse :as browse]
         '[ruuter.core :as r]
         '[malli.dev :as dev]
         '[babashka.classpath :as cp])

(cp/add-classpath "src:test")

(require
         '[gol.board :as board]
         '[gol.app :as app])


(let [f (resolve 'unstrument)]
  (when f
        (@f)))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Routes
;;;;;;;;;;;;;;;;;;;;;;;;;;

(def routes (flatten [app/app-routes board/board-routes]))

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
  (swap! board/boards (constantly {}))
  (start))

(defn instrument [] (dev/start!))
(defn unstrument [] (dev/stop!))
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn restrument [] (unstrument) (instrument))

(when (= *file* (System/getProperty "babashka.file"))
  (start)
  (@promise))

(instrument)
