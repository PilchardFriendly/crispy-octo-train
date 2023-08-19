#!/usr/bin/env bb
;; (require '[org.httpkit.server :as srv]
;;          '[clojure.java.browse :as browse]
;;          '[gol.monitoring :as mon]
;;          '[ruuter.core :as r]
;;)

(require '[dev.monitoring :as mon])
(mon/unstrument)

#_{:clj-kondo/ignore [:unused-namespace]}
(require
 '[gol.board :as board]
 '[gol.app :as app]
 '[gol.schemas :as schemas]
 '[gol.server :as server]
 )

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Routes
;;;;;;;;;;;;;;;;;;;;;;;;;;

(def routes
  (flatten [app/app-routes
            board/board-routes]))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Config
;;;;;;;;;;;;;;;;;;;;;;;;;;

(def port 3000)

(defn start [] 
  (server/start :port port :routes routes))
(defn stop []
  (server/stop))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn restart []
  (stop)
  (board/swap-repository! (constantly {}))
  (start))

(require 
 '[dev.meta :as dev])

(if (= *file* (System/getProperty "babashka.file"))
  (do
    (start) 
    @(promise))
  (do
    (mon/instrument)  
    ;; (mon/check)
    (dev/set-repl! true))) 