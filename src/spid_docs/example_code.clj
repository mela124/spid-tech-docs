(ns spid-docs.example-code
  (:require [clojure.string :as str]))

(def examples
  {"email" "johnd@example.com"
   "displayName" "John"
   "name" "John Doe"
   "birthday" "1977-01-31"
   "preferredUsername" "johnd"
   "object" "User"})

(defn- replace-path-parameters [url]
  (str/replace url #"\{([^}]+)\}" (fn [[_ match]] (examples match))))

(defn- curl-example-code [{:keys [url method access_token_types]} params]
  (apply str "curl https://payment.schibsted.no" (replace-path-parameters url)
         (when (= "POST" method) " \\\n   -X POST")
         (when (seq access_token_types) " \\\n   -d \"oauth_token=[access token]\"")
         (map (fn [param] (str " \\\n   -d \"" param "=" (examples param) "\"")) params)))

(defn- clojure-example-code [{:keys [method path]} params]
  (let [sdk-invocation (str "  (sdk/" method " \"/" (replace-path-parameters path) "\"" (when (seq params) " {"))
        param-map-indentation (apply str (repeat (count sdk-invocation) " "))]
    (str "(ns example\n  (:require [spid-sdk-clojure.core :as sdk]))\n\n(-> (sdk/create-client \"[client-id]\" \"[secret]\")\n"
         sdk-invocation
         (when (seq params)
           (str (str/join (str "\n" param-map-indentation) (map #(str "\"" % "\" \"" (examples %) "\"") params)) "}"))
         "))")))

(defn create-example-code [endpoint]
  (let [req-params (:required endpoint)
        all-params (concat req-params (:optional endpoint))]
    {:curl {:minimal (curl-example-code endpoint req-params)
            :maximal (when (seq all-params) (curl-example-code endpoint all-params))}
     :clojure {:minimal (clojure-example-code endpoint req-params)
               :maximal (when (seq all-params) (clojure-example-code endpoint all-params))}}))