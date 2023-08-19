(ns dev.babashka 
  (:require [clojure.string :as string]
            [babashka.fs :as fs]))
            ;; [dev.debug :refer [spy]]))

(defn- file->ns
  [file]
  (as-> file $
    (fs/components $)
    (drop 1 $)
    (mapv str $)
    (string/join "." $)
    (string/replace $ #"_" "-")
    (string/replace $ #".clj$" "")
    (symbol $)))


(comment #_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
         (defn test-namespaces []
  (as->
   (fs/glob "test" "**/*_test.clj") $
    (mapv file->ns $))))