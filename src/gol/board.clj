(ns gol.board 
  (:require [malli.core :as m]
            [gol.htmx :as htmx :refer [redirect page render]]
            [gol.schemas :as schemas :refer
             [Board BoardFragment Boards GetBoardRequest 
              Html Redirection RedirectionForBoard 
              RenderContent]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Board Stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce boards (atom {}))

(m/=> board-model-new [:=> [:cat] Board])
(defn board-model-new []
  (let [row' #(vec (boolean-array 3 false))
        model {:id (random-uuid)
               :cells (vec (repeatedly 3 row'))}]
    model))

(m/=> boards-state-new [:=> [:cat] Board])
(defn boards-state-new []
  (let [board' (board-model-new)]
    (swap! boards #(assoc %1 (:id board') board'))
    board'))

(defn boards-state [] @boards)
(m/=> boards-state [:=> [:cat] Boards])


(defn boards-state-get [id]
  (get @boards id))
(m/=> boards-state-get [:=> [:cat :uuid] Board])


(m/=> board-fragment [:=> [:cat BoardFragment] RenderContent])
(defn board-fragment [{:keys [id cells]}]
  (let [cells-td (fn [c] [:td c])
        cells-tr (fn [cs] (->> cs
                               (map cells-td)
                               (into [:tr])))
        cells-trs (->> cells (map cells-tr))]
    (list [:div (str "Board " id)]
          [:table (into [:tbody] cells-trs)])))

(m/=> board-template [:=> [:cat BoardFragment] RenderContent])
(defn board-template [model]
  (-> model
      board-fragment))

; This bit is cool - we can send an event back to the UI, and have HTML send it to listeners

(m/=> redirection-for-board [:=> [:cat RedirectionForBoard] Redirection])
(defn redirection-for-board [{:keys [id]}]
  {:location (str "/board/" id) :event {:newBoard {:id id}}})

(m/=> boards-post [:=> [:cat :any] Redirection])
(defn boards-post [_]
  (-> (boards-state-new)
      redirection-for-board
      redirect))

(m/=> boards-get [:=> [:cat BoardFragment] Html])
(defn boards-get [req]
  (-> {:fragment "board-index" :title "Boards" :body (board-template req)}
      page
      render))


(m/=> board-get [:=> [:cat GetBoardRequest] Html])
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

(defn board-new-action-fragment []
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
