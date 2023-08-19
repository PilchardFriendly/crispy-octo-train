(ns pilchardfriendly.kaocha.plugins.portal
  (:require [kaocha.plugin :as plugin]
            [kaocha.hierarchy :as hierarchy]
            [portal.api :as p]
            [taoensso.timbre :as log]
            [lambdaisland.tools.namespace.repl :refer (disable-unload!)]))


(declare report->portal)
(def default-test-reports  (with-meta (vector) {:portal.viewer/default
                                                      ;; :portal.viewer/inspector
                                              :portal.viewer/test-report}))
(defn system [_] {:test-taps (atom default-test-reports)
                  :portal    (atom nil)
                  :submit    (atom nil)})

(defn submit* "The reloadable implementation of submit"
  [ref v]
  (let [m (try (meta v) (catch Exception _))]
    (when (::report m)
      (swap! ref conj (report->portal v)))))
  
(defn start [system]
  (log/debug system)
  (let [tap' (:test-taps system)]
    (-> system :portal (reset! (p/open {:launcher :vs-code
                                        :portal.colors/theme :portal.colors/zerodark
                                        :portal.launcher/window-title "** tests **"
                                        :value tap'})))
    (-> system :submit (reset! (fn [v] (submit* tap' v))))
    (-> system :submit deref add-tap))
  system)

(defn stop [system]
  (when-let [submit' (:submit system)]
    (-> submit' deref remove-tap)
    (-> submit' (reset! nil)))
  (when-let [portal' (:portal system)]
    (-> portal' deref  (p/close))
    (-> portal' (reset! nil)))
  (when-let [tap' (:test-taps system)]
    (-> tap' (reset! default-test-reports)))
  system)


(defn report->tap> [v] (tap> (vary-meta v assoc ::report true)))
(defn report->portal [v]
  ;; (log/debug v)
  (vary-meta v dissoc ::report))


(defmulti portal-reporter* (fn [_ e] (:type e)) :hierarchy #'hierarchy/hierarchy)
(defmethod portal-reporter* :default [state e]
  (let [e'' (-> e (select-keys [:type :file :line :expected :actual :message :var :ns]))
        e' (cond-> e''
             (:ns e'') (update-in [:ns] ns-name))]
    (swap! state conj e'))
  e)
(defmethod portal-reporter* :begin-test-suite [_ e] e)
(defmethod portal-reporter* :end-test-suite [_ e] e)
(defmethod portal-reporter* :summary [state e]
  ;; (log/debug e) 
  ;; (log/debug @state) 
  
  (let [[old _] (reset-vals! state nil)]
    ;; (log/debug old) 
    (report->tap> old))
  e)

(defn portal-reporter [ref e] 
  ;; (log/debug @ref)
  (portal-reporter* ref e))

(plugin/defplugin pilchardfriendly.kaocha.plugins/portal 
  #_{:clj-kondo/ignore [:unresolved-symbol]}
  
  (config "Add a reporter to the reporter stack" [cfg]
          ;; (log/debug cfg)  
          (-> cfg (update-in [:kaocha/reporter] 
                             (fn [rs] (conj (or rs [])
                                            (partial portal-reporter 
                                                     (atom  default-test-reports))))))))

  (defonce ^:dynamic *system* (-> {} system start)) 
  (disable-unload!)  