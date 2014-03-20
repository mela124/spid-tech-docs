(ns spid-docs.cultivate.endpoints-test
  (:require [midje.sweet :refer :all]
            [spid-docs.cultivate.content-shells :as cs]
            [spid-docs.cultivate.endpoints :refer :all]))

(def raw-content
  "Test data with relevant raw content when cultivating endpoints.
   Look for these values in tests below."
  {:sample-responses {"terms-get.json" "terms json"
                      "terms-get.jsonp" "terms jsonp"}
   :endpoint-descriptions {"terms-get.md" "terms desc"}
   :pagination-descriptions {:limit  "limit desc"
                             :offset "offset desc"
                             :since  "since desc"
                             :until  "until desc"}
   :filter-descriptions {:merchant "merchant desc"}})

(defn cultivate
  "Helper function to create and cultivate endpoints."
  [& {:as m}]
  (cultivate-endpoint (cs/endpoint m) raw-content))

(fact
 "Number of returned endpoints depends on the given http methods."

 (-> (cultivate :httpMethods {}) count) => 0
 (-> (cultivate :httpMethods {:GET (cs/http-method)}) count) => 1
 (-> (cultivate :httpMethods {:GET (cs/http-method)
                              :POST (cs/http-method)}) count) => 2)

(fact
 "The path and method is used to generate a friendly identifier. This
  is used to look up information about the endpoint in other sources."

 (-> (cultivate :path "terms"
                :httpMethods {:GET (cs/http-method)})
     first :id)
 => :terms-get

 (-> (cultivate :path "subscription"
                :httpMethods {:POST (cs/http-method)})
     first :id)
 => :subscription-post

 (-> (cultivate :path "describe/{object}"
                :httpMethods {:GET (cs/http-method)})
     first :id)
 => :describe-object-get)

(fact
 "Descriptions are pulled out of the raw-content based on the endpoint id."

 (-> (cultivate :path "terms"
                :httpMethods {:GET (cs/http-method)})
     first :description)
 => "terms desc")

(fact
 "All paths are prefixed with a slash."

 (-> (cultivate :path "terms") first :path) => "/terms"
 (-> (cultivate :path "terms") first :api-path) => "/api/2/terms")

(fact
 "We generate one endpoint for each http verb, so the method is straight on the endpoint."

 (-> (cultivate :httpMethods {:GET (cs/http-method)}) first :method) => :GET)

(fact
 "Names are cut apart and stiched together to fit the httpMethod."

 (-> (cultivate :name "Create subscription") first :name)
 => "Create subscription"

 (->> (cultivate :name "Get or create a product"
                 :httpMethods {:GET (cs/http-method)
                               :POST (cs/http-method)})
      (map (juxt :method :name)) set)
 => #{[:GET "Get a product"]
      [:POST "Create a product"]}

 (->> (cultivate :name "Create, get or delete an order"
                 :httpMethods {:GET (cs/http-method)
                               :POST (cs/http-method)
                               :DELETE (cs/http-method)})
      (map (juxt :method :name)) set)
 => #{[:GET "Get an order"]
      [:POST "Create an order"]
      [:DELETE "Delete an order"]})

(fact
 "Category is more pleasant to use if the levels are named."

 (-> (cultivate :category ["Insight" "KPI API"]) first :category)
 => {:section "Insight" :api "KPI API"})

(fact
 "Path parameters are included in the general :parameters list.
  They're all required."

 (-> (cultivate :pathParameters ["id"]
                :parameter_descriptions {:id "The user ID"}
                :alias {:id "user_id"})
     first :parameters)
 => [{:name "id"
      :aliases ["user_id"]
      :description "The user ID"
      :type :path
      :required? true}])

(fact
 "Query parameters are included in the general :parameters list too."

 (-> (cultivate :httpMethods {:GET (cs/http-method {:required ["email"]})}
                :parameter_descriptions {:email "The email"})
     first :parameters)
 => [{:name "email"
      :description "The email"
      :type :query
      :required? true}]

 (-> (cultivate :httpMethods {:POST (cs/http-method {:optional ["name"]})}
                :parameter_descriptions {:name "The name"})
     first :parameters)
 => [{:name "name"
      :description "The name"
      :type :query
      :required? false}])

(fact
 "Pagination parameters are treated specially, bundled together and
  removed from the proper parameters list. This is so that readers of
  the documentation can learn pagination once, and then not have to
  sift through it for every endpoint.

  Required pagination parameters are not treated this way."

 (let [endpoint (-> (cultivate :httpMethods {:GET (cs/http-method {:optional ["limit" "offset" "since" "until"]})})
                    first)]

   (:parameters endpoint) => []
   (:pagination endpoint) => [{:name "limit"
                               :description "limit desc"
                               :type :query, :required? false}
                              {:name "offset"
                               :description "offset desc"
                               :type :query, :required? false}
                              {:name "since"
                               :description "since desc"
                               :type :query, :required? false}
                              {:name "until"
                               :description "until desc"
                               :type :query, :required? false}]))

(fact
 "If there are no pagination parameters, the :pagination key is
  removed, instead of it pointing to an empty list."

 (-> (cultivate) first :pagination) => nil)

(fact
 "The filter parameter is also treated specially, since there is a
  list of valid filters for a given endpoint. This too is removed from
  the proper parameters list (if optional)."

 (let [endpoint (-> (cultivate :httpMethods {:GET (cs/http-method {:optional ["filters"]
                                                                   :filters ["merchant"]
                                                                   :default_filters []})})
                    first)]

   (:parameters endpoint) => []
   (:filters endpoint) => [{:name "merchant"
                            :description "merchant desc"
                            :default? false}]))

(fact
 "If there is no filter parameter, the :filters key is removed,
  instead of it pointing to an empty list."

 (-> (cultivate) first :filters) => nil)

(fact
 "We prefer response-formats over valid-output-formats since
  'response' is more accurate than 'output', and there is no list of
  'invalid' formats, so the validity is implicit."

 (-> (cultivate :valid_output_formats ["json" "jsonp"])
     first :response-formats)
 => [:json :jsonp])

(fact
 "Likewise, there's a default-response-format instead of a
  default-output-format."

 (-> (cultivate :default_output_format "json")
     first :default-response-format)
 => :json)

(fact
 "We represent the access token types as a set of keys, instead of a
  list, since the data isn't inherently ordered."

 (-> (cultivate :httpMethods {:GET (cs/http-method {:access_token_types ["server" "user"]})})
     first :access-token-types)
 => #{:server :user})

(fact
 "Authentication is required if there is any access token types
  listed."

 (-> (cultivate :httpMethods {:GET (cs/http-method {:access_token_types ["server"]})})
     first :requires-authentication?)
 => true

 (-> (cultivate :httpMethods {:GET (cs/http-method {:access_token_types []})})
     first :requires-authentication?)
 => false)

(fact
 "Responses are sorted into :success and :failures."

 (-> (cultivate :httpMethods
                {:GET (cs/http-method {:responses [{:status 200
                                                    :description "OK"
                                                    :type "string"}
                                                   {:status 422
                                                    :description "NO!"
                                                    :type "string"}
                                                   {:status 500
                                                    :description "OH NO!"
                                                    :type "error"}]})})
     first :responses)
 => {:success {:status 200
               :description "OK"
               :type :string}
     :failures [{:status 422
                 :description "NO!"
                 :type :string}
                {:status 500
                 :description "OH NO!"
                 :type :error}]})

(fact
 "The success response contains a :samples if it is present
  in :sample-responses."

 (-> (cultivate :httpMethods {:GET (cs/http-method {:responses [{:status 200, :description "", :type "string"}]})}
                :path "terms")
     first :responses)
 => {:success {:status 200
               :description ""
               :type :string
               :samples {:json "terms json"
                         :jsonp "terms jsonp"}}
     :failures []})

(fact (-> (cultivate :deprecated "1.0") first :deprecated) => "1.0")
