(ns gol.board
  (:require [malli.core :as m]
            [malli.transform :as mt]
            [gol.htmx :as htmx :refer [hx-request?]]
            [dev.monitoring :as mon]
            [dev.meta :as dev]))

(mon/unstrument)
;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Schemas
;;;;;;;;;;;;;;;;;;;;;;;;;

(def BoardRef [:map [:id {:decode/string parse-uuid} :uuid]])

(def Board [:map [:id :uuid]
            [:cells [:vector [:vector :boolean]]]
            [:version {:optional true} :string]])

(def Boards [:map-of :uuid Board])


;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Board Stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn  board-model-new []
  (let [row' #(vec (boolean-array 3 false))
        model {:id (random-uuid)
               :cells (vec (repeatedly 3 row'))}]
    model))

;; (m/=> board-model-new [:=> [:cat] Board])

(defonce ^:private ^:dynamic repository (atom {}))
(defonce ^:private ^:dynamic factory (atom board-model-new))

(defn swap-repository! [f] (swap! repository f))
(defn swap-factory! [f] (swap! factory f))
(defn current-boards [] @repository)

(defmacro with-repository 
  [& body]
  `(let [bd# (atom {})]
        (binding [repository bd#]
          ~@body)))

(defmacro with-factory
  "Used to replace the binding for "
  [& body]
  `(let [fct# (atom board-model-new)]
        (binding [factory fct#]
          ~@body)))

(def board-path "/board")

(defn map->board-uri [{:keys [id]}]
  (str board-path "/" id))


(defn request->board-key [{:keys [params]}]
  (:id (m/decode BoardRef params mt/string-transformer)))

(defn ^:private make-board [] (@factory))

(defn boards-state-new []
  (let [board' (make-board)]
    (swap! repository #(assoc %1 (:id board') board'))
    board'))
;; (m/=> boards-state-new [:=> [:cat] Board])

(defn boards-state [] @repository)
;; (m/=> boards-state [:=> [:cat] Boards])


(defn boards-state-get [id]
  (get @repository id))
;; (m/=> boards-state-get [:=> [:cat :uuid] Board])


;; (m/=> board-fragment (schemas/view-fragment BoardModel))
(defn board-fragment [{:keys [id cells]}]
  (let [cells-td (fn [c] [:td c])
        cells-tr (fn [cs] (->> cs
                               (map cells-td)
                               (into [:tr])))
        cells-trs (->> cells (map cells-tr))]
    (list [:div (str "Board " id)]
          [:table (into [:tbody] cells-trs)])))

;; (m/=> board-template (schemas/view-template BoardModel))
(defn board-template [_ & {:keys [model]}]
  {:content (board-fragment model)})


; This bit is cool - we can send an event back to the UI, and have HTML send it to listeners

;; (m/=> redirection-for (schemas/view Redirection))
(defn redirection-for [{:keys [uri]}]
  {:location uri
   :status 302})

;; (m/=> board-get (schemas/request-handler [:orn 
;;                                           [:success (schemas/view-result Board)]
;;                                           [:failure (ring/not-found-response)]]))
(defn board-get [req]
  
  (let [found (-> req request->board-key boards-state-get)]
    (if found
      {:template board-template
       :model found}
      {:status 404})))

;; (m/=> route-new-board (schemas/view-action BoardModel BoardEvent [BoardModel]))
(defn route-new-board [req {:keys [id] :as model}]
  (let [req' (-> req
                 (assoc :request-method :get)
                 (assoc :uri (map->board-uri model))
                 (assoc :params {:id (str id)}))
        event {:newBoard {:id id}}]
    (cond
      (hx-request? req) (-> req' board-get (assoc :event event))
      :else             (-> req' redirection-for (assoc :event event)))))

;;  (m/=> boards-post (schemas/view-action BoardModel schemas/BoardEvent))
(defn boards-post [req]
  (->> (boards-state-new)
       (route-new-board req)))

;; (m/=> boards-fragment (schemas/view-fragment Boards))
(defn boards-fragment [model]
  (let [title (fn [board] (->> board :id (str "Board: ")))
        tr (fn [[_ board]]
             [:tr
              [:td
               [:button {:hx-get (map->board-uri board)
                         :hx-target "#board-detail"} (title board)]]])
        tbody (->> model (map tr))]
    (list [:table (into [:tbody] tbody)])))

;; (m/=> boards-template (schemas/view-template Boards))
(defn boards-template [_ & {:keys [model]}]
  {:content (boards-fragment model)})

;; (m/=> boards-get (schemas/view Boards))
(defn boards-get [_]
  {:template boards-template
   :model (boards-state)})

(defn board-new-action-fragment []
  [:button {:hx-post board-path :hx-swap "inner" :hx-target "#board-detail"} "New game"])

(def board-routes [{:path "/board"
                    :method :get
                    :name :boards-index
                    :response (htmx/handle-view boards-get)}
                   {:path "/board"
                    :method :post
                    :name :board-post
                    :response (htmx/handle-view boards-post)}
                   {:path "/board/:id"
                    :method :get
                    :name :board-get
                    :response (htmx/handle-view board-get)}])


(comment mon/instrument)

(dev/annotate {:dev.test/testable-id 'gol.board-test})
