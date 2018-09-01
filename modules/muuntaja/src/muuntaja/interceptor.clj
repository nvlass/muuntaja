(ns muuntaja.interceptor
  (:refer-clojure :exclude [format])
  (:require [muuntaja.core :as m]))

(def params-interceptor
  "Interceptor that merges request `:body-params` into `:params`."
  {:enter (fn [ctx]
            (letfn [(set-params
                      ([request]
                       (let [params (:params request)
                             body-params (:body-params request)]
                         (cond
                           (not (map? body-params)) request
                           (empty? body-params) request
                           (empty? params) (assoc request :params body-params)
                           :else (update request :params merge body-params)))))]
              (let [request (:request ctx)]
                (assoc ctx :request (set-params request)))))})

(defn create-format-interceptor
  "Interceptor that negotiates a request body based on accept, accept-charset
  and content-type headers and decodes the body with an attached Muuntaja
  instance into `:body-params`. Encodes also the response body with the same
  Muuntaja instance based on the negotiation information or override information
  provided by the handler.

  Takes a pre-configured Muuntaja or options maps an argument.
  See https://github.com/metosin/muuntaja for all options and defaults."
  ([]
   (create-format-interceptor m/instance))
  ([prototype]
   (let [m (m/create prototype)]
     {:name ::format
      :enter (fn [ctx]
               (let [request (:request ctx)]
                 (assoc ctx :request (m/negotiate-and-format-request m request))))
      :leave (fn [ctx]
               (let [request (:request ctx)
                     response (:response ctx)]
                 (assoc ctx :response (m/format-response m request response))))})))

(defn create-format-negotiate-interceptor
  "Interceptor that negotiates a request body based on accept, accept-charset
  and content-type headers with an attached Muuntaja instance. Injects negotiation
  results into request for `format-request` interceptor to use.

  Takes a pre-configured Muuntaja or options maps an argument.
  See https://github.com/metosin/muuntaja for all options and defaults."
  ([]
   (create-format-negotiate-interceptor m/instance))
  ([prototype]
   (let [m (m/create prototype)]
     {:name ::format-negotiate
      :enter (fn [ctx]
               (let [request (:request ctx)]
                 (assoc ctx :request (m/negotiate-request-response m request))))})))

(defn create-format-request-interceptor
  "Interceptor that decodes the request body with an attached Muuntaja
  instance into `:body-params` based on the negotiation information provided
  by `format-negotiate` interceptor.

  Takes a pre-configured Muuntaja or options maps an argument.
  See https://github.com/metosin/muuntaja for all options and defaults."
  ([]
   (create-format-request-interceptor m/instance))
  ([prototype]
   (let [m (m/create prototype)]
     {:name ::format-request
      :enter (fn [ctx]
               (let [request (:request ctx)]
                 (assoc ctx :request (m/format-request m request))))})))

(defn create-format-response-interceptor
  "Interceptor that encodes also the response body with the attached
  Muuntaja instance, based on request negotiation information provided by
  `format-negotiate` interceptor or override information provided by the handler.

  Takes a pre-configured Muuntaja or options maps an argument.
  See https://github.com/metosin/muuntaja for all options and defaults."
  ([]
   (create-format-response-interceptor m/instance))
  ([prototype]
   (let [m (m/create prototype)]
     {:name ::format-response
      :leave (fn [ctx]
               (let [request (:request ctx)
                     response (:response ctx)]
                 (assoc ctx :response (m/format-response m request response))))})))
