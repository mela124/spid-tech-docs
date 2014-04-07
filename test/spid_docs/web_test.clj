(ns spid-docs.web-test
  (:require [clojure.string :as str]
            [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as enlive]
            [spid-docs.web :refer :all]))

(defn link-valid? [link page-url pages]
  (let [href (-> link :attrs :href)
        [path hash] (str/split href #"#")]
    (if (or
         (when (= "#?" href) ; use #? to postpone writing a link. We'll nag about it, tho.
           (do (println "TODO: The" (:content link) "at" page-url "needs to point somewhere")
               true))
         (and (empty? path) (not (empty? hash))) ; inpage hash navigation
         (.startsWith path "http://")            ; external link
         (.startsWith path "https://")
         (contains? pages path)            ; known page
         (contains? pages (str path "index.html")))
      :link-valid
      :link-to-unknown-page)))

(fact
 :slow
 "Integration tests. Avoid running them with:

 lein with-profile test midje :autotest :filter -slow"

 (let [pages (get-pages)]
   (doseq [url (keys pages)]
     (let [response (app {:uri url})
           status (:status response)]

       ;; Check that the pages respond with 200 OK.
       [url status] => [url 200]

       ;; Check that all links point to existing pages
       (doseq [link (-> response
                        :body
                        java.io.StringReader.
                        enlive/html-resource
                        (enlive/select [:a]))]
         (let [href (get-in link [:attrs :href])]
           [url href (link-valid? link url pages)] => [url href :link-valid]))))))
