(ns muuntaja.format.jsonista
  (:require [jsonista.core :as jsonista]
            [muuntaja.protocols :as protocols])
  (:import (java.io ByteArrayInputStream
                    InputStream
                    InputStreamReader
                    OutputStreamWriter
                    OutputStream)))

(defn ^:no-doc make-json-decoder [{:keys [keywords?]}]
  (let [mapper (jsonista/object-mapper {:keywordize? keywords?})]
    (fn [x ^String charset]
      (if (string? x)
        (jsonista/read-value x mapper)
        (jsonista/read-value (InputStreamReader. ^InputStream x charset) mapper)))))

(defn ^:no-doc make-json-encoder [options]
  ;; TODO: map options
  (let [mapper (jsonista/object-mapper)]
    (fn [data ^String charset]
      (ByteArrayInputStream.
        (if (.equals "utf-8" charset)
          (jsonista/write-value-as-bytes data mapper)
          (.getBytes ^String (jsonista/write-value-as-string data mapper) charset))))))

(defn make-streaming-json-encoder [{:keys [keywords?]}]
  (let [mapper (jsonista/object-mapper {:keywordize? keywords?})]
    (fn [data ^String charset]
      (protocols/->StreamableResponse
       (fn [^OutputStream output-stream]
         (jsonista/write-value (OutputStreamWriter. output-stream charset) data mapper))))))

;;;
;;; format
;;;

;; type

(def json-type "application/json")

;; formats

(def json-format
  {:decoder [make-json-decoder {:keywords? true}]
   :encoder [make-json-encoder]})

(def streaming-json-format
  (assoc json-format :encoder [make-streaming-json-encoder]))

;; options

(defn with-json-format [options]
  (assoc-in options [:formats json-type] json-format))

(defn with-streaming-json-format [options]
  (assoc-in options [:formats json-type] streaming-json-format))