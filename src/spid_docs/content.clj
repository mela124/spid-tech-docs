(ns spid-docs.content
  "Functions for loading and 'cultivating' (i.e. processing, cross-linking, etc)
   content from the various sources."
  (:require [spid-docs.core :as spid]
            [spid-docs.cultivate.apis :as ca]
            [spid-docs.cultivate.endpoints :as ce]
            [stasis.core :as stasis]))

(defn load-content
  "Loads content from .edn and .md files in resources/. See the readme for a
  run-down of various kinds of contents and where/how they are stored."
  []
  {:endpoints (:data (spid/load-edn "cached-endpoints.edn"))
   :articles (stasis/slurp-directory "resources/articles" #"\.md$")
   :concepts (stasis/slurp-directory "resources/concepts" #"\.md$")
   :sample-responses (stasis/slurp-directory "resources/sample-responses" #".+\..+$")
   :endpoint-descriptions (stasis/slurp-directory "resources/endpoints" #"\.md$")
   :pagination-descriptions (spid/load-edn "pagination.edn")
   :filter-descriptions (spid/load-edn "filters.edn")
   :types (spid/load-edn "types.edn")
   :apis (spid/load-edn "apis.edn")})

(defn cultivate-content
  "Combine and cross-link content"
  [raw-content]
  (assoc raw-content
    :endpoints (mapcat ce/cultivate-endpoint (:endpoints raw-content))))
