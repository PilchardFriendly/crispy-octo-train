(ns gol.app 
  (:require
   [malli.core :as m]
   [dev.monitoring :as mon]
   [gol.htmx :as htmx]
   [gol.board :as board :refer [board-new-action-fragment]]
   [gol.schemas :as schemas]))

(mon/unstrument)
;;;;;;;;;;;;;;;;;;;;;;;;;;
;; App Stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;
(def Filter :int)


#_{:clj-kondo/ignore [:unused-binding]}
(defn app-template  [_ & {:keys [model]}]
  {:title "Htmx Game of Life3"
   :content (list (board-new-action-fragment)
                  [:div#boards-master {:hx-trigger "load, newBoard from:body"
                                       :hx-get "/board"}]
                  [:div#board-detail])})
(comment m/=> app-template (schemas/view-template Filter))


(defn app-index [_] 
  {:template app-template
   :model 5})

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(def app-routes [{:path "/"
                  :method :get
                  :response (htmx/handle-view app-index)}])
(comment m/=> app-index (schemas/view Filter))
(mon/instrument)