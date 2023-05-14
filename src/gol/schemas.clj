(ns gol.schemas)

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

(def Board [:map [:id :uuid]
             [:cells [:vector [:vector :boolean]]]
             [:version {:optional true} :string]])

(def Boards [:map-of :uuid Board])

(def BoardFragment [:map [:id :uuid] [:cells [:vector [:vector :boolean]]]])

(def RedirectionForBoard [:map [:id :uuid]])

(def GetBoardRequest [:map [:params [:map [:id :string]]]])
