(ns pilchardfriendly.kaocha.plugins.portal.system 
  (:require [portal.api :as p]))

(defn report->tap> [v]
  (tap> (vary-meta v assoc ::report true)))
(defn report->portal [v]
  ;; (log/debug v)
  (vary-meta v dissoc ::report))

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
;;   (log/debug system)
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