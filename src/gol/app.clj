(ns gol.app 
  (:require [gol.htmx :as html :refer [render fragment page parse-query-string]]
            [gol.board :as board :refer [board-new-action-fragment]]))


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