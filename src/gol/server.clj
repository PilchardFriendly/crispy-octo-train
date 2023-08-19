(ns gol.server
  (:require [ruuter.core :as r]
            [org.httpkit.server :as srv]
            [clojure.java.browse :as browse]))

;;;;;;;;;;;;;;;;;;;;;;;;;;ß
;; Server
;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce server (atom nil))


(defn start [& {:keys [port routes]}]
  (when (nil? @server)
    (let [url (str "http://localhost:" port "/")
          shutdown (srv/run-server #(r/route routes %) {:port port})]
      (reset! server shutdown)
      (println "serving" url)
      (browse/browse-url url)
      {:server shutdown}))) 


(defn stop []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))