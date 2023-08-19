(ns gol.registry
  (:require [malli.core :as m]
            [malli.registry :as mr]
            [malli.util :as mu]
            [dev.debug :refer [spy]]
            [clojure.core :refer [merge]]
            [clojure.algo.generic.functor :refer [fmap]]
            [dev.meta :as dev]))
(comment def spy identity)
(defonce ^:private ^:dynamic -ns-registry (atom {}))
(defonce ^:private -builtin-registry 
  (mr/fast-registry (merge (m/default-schemas) (mu/schemas))))

(spy -builtin-registry)
;; (spy (->> (m/default-schemas) (map m/type)))


(defn ^:private -rebind-registries! [nss] 
  (let [args (into [-builtin-registry] (vals nss))
        composite' (apply mr/composite-registry
                          args)]
    (mr/set-default-registry! composite')))

(defn register! [myns ?reg]
  (let [reg (mr/registry ?reg)
        ;; _ (spy (mr/registry? reg))
        ;; _ (-> reg spy)
        ;; _ (-> myns spy)
        ;; _ (-> @-ns-registry spy)
        go (fn [m]
            ;;  (spy m)
             (assoc m myns reg))
        nss' (swap! -ns-registry go)]
        ;; _ (-> nss' keys spy)]
    (-rebind-registries! nss')
    (let [nss'' (into nss' [[:default m/default-registry]])
          counts (fmap #(-> %1 mr/schemas count) nss'')]
      (spy counts))
    ?reg))

(dev/annotate)