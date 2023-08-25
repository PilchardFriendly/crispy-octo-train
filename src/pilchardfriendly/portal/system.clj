(ns pilchardfriendly.portal.system
  (:require [pilchardfriendly.prelude :refer [flip]]
            [portal.api :as p]
            [taoensso.timbre :as log]))

(defn tag-of [sys v]
  (let [tagged (::tag sys)
        m (try (meta v) (catch Exception _))]
    (tagged m)))

(defn untag [sys v]
  (vary-meta v dissoc (::tag sys)))

(defn system [{:keys [tag init-val :portal/config]}]
  {::init-val     init-val
   ::sink         (atom init-val)
   ::portal       (atom nil)
   ::submit       (atom nil)
   ::tag          tag
   :portal/config config})

(defn submit* "The reloadable implementation of submit"
  [sys v]

  (when (tag-of sys v)
    (swap! (::sink sys) conj (untag sys v))))

(def ^:dynamic default-portal-config
  {:launcher :vs-code
   :portal.colors/theme :portal.colors/zerodark
   :portal.launcher/window-title "** default **"})


(defn start [system]
;;   (log/debug system)
  (let [sink-ref (::sink system)
        portal-cfg (-> system
                       :portal/config
                       ((flip merge) default-portal-config)
                       (assoc :value sink-ref))]
    (-> system ::portal (reset! (p/open portal-cfg)))
    (-> system ::submit (reset! (fn [v] (submit* system v))))
    (-> system ::submit deref add-tap))
  system)


(defn stop [system]
  (letfn [(debug-submit [v] (log/debug "Removing tap...") v)
          (debug-portal [v] (log/debug "Closing portal...") v)
          (debug-sink [v] (log/debug "...sink reset") v)]
    (some-> system ::submit (reset-vals! nil) first debug-submit  remove-tap)
    (some-> system ::portal (reset-vals! nil) first debug-portal  p/close)
    (some-> system ::sink (reset-vals! (::init-val system)) first debug-sink)
    system))