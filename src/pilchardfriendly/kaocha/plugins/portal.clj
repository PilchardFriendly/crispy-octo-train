(ns pilchardfriendly.kaocha.plugins.portal
  (:require [kaocha.plugin :as plugin]
            [lambdaisland.tools.namespace.repl :refer (disable-unload!)]
            [pilchardfriendly.kaocha.plugins.portal.reporter :refer [install]]
            [pilchardfriendly.kaocha.plugins.portal.system :refer [start system]]))


(defonce ^:dynamic *system* (-> {} system start))
(disable-unload!)

#_{:clj-kondo/ignore [:unresolved-namespace]}
(plugin/defplugin pilchardfriendly.kaocha.plugins/portal
  #_{:clj-kondo/ignore [:unresolved-symbol]}

  (config "Add a reporter to the reporter stack" [cfg]
          ;; (log/debug cfg)   
          (install cfg)))
