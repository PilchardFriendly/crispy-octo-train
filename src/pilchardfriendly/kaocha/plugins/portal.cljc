(ns pilchardfriendly.kaocha.plugins.portal
  (:require [kaocha.plugin :as plugin]
            [pilchardfriendly.kaocha.plugins.portal.reporter :as reporter]
            )
  #?(:bb (:require [taoensso.timbre :as log])
     :clj (:require [taoensso.timbre :as log] 
                    [objection.core :as obj]
                    [pilchardfriendly.kaocha.plugins.portal.system :as system])))

 
#?(:bb (comment (system/system))
   :clj (do
          (obj/status)
          (let [alias [:portal-client]
                status (-> alias obj/describe)]
            (log/debug status)
            (when-not (-> status :registered?)
              (log/debug "Starting...")
              (-> (system/system {})
                  system/start
                  (obj/register  {:name (str "Portal Client")
                                  :alias alias
                                  :data {}
                                  :stopfn (fn [svc] (log/debug "Stopping....") (system/stop svc))}))
              (-> (Runtime/getRuntime)
                  (.addShutdownHook
                   (Thread. (fn [] (obj/stop! alias)))))) 
            (obj/status))))
  

#_{:clj-kondo/ignore [:unresolved-namespace]}  
(plugin/defplugin pilchardfriendly.kaocha.plugins/portal
  #_{:clj-kondo/ignore [:unresolved-symbol]}
  
  (config [cfg]
          ;; (log/debug cfg)   
          (reporter/install cfg))
  #_{:clj-kondo/ignore [:unresolved-symbol]}
  (main [cfg] (log/debug "main") cfg))  
