(ns pilchardfriendly.kaocha.plugins.portal.system
  (:require [pilchardfriendly.portal.system :as p]))

(defn report->tap> [v]
  (tap> (vary-meta v assoc ::report true)))

(def ^:private  empty-sink  (with-meta (list) {:portal.viewer/default
                                               :portal.viewer/test-report}))
(defn system [_] (p/system {:init-val empty-sink
                            :tag      ::report
                            :portal/config {:portal.launcher/window-title "** tests **"}}))

(def start p/start)
(def stop p/stop)