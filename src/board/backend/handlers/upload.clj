(ns board.backend.handlers.upload
  (:require
    [board.util.exception-format :refer [map->invalid-req]]
    [board.util.pformat :refer [pformat]]
    [clojure.java.io :as io]
    [clojure.math :as math]
    [clojure.tools.logging :as log]
    [farolero.core :as far]
    [fmnoise.flow :as flow :refer [then]]
    [malli.core :as malli]
    [malli.error :as malli-error]
    [malli.util :as malli-util])
  (:import
    (java.io
      OutputStream)
    (java.security
      DigestInputStream
      MessageDigest)))


;; seprate this into config
(def directory
  "/home/oxy/Documents/")


(defn get-hash
  [path]
  (try
    (let [digest (MessageDigest/getInstance
                   "MD5")]
      (with-open
        [input-stream (io/input-stream path)
         digest-stream (DigestInputStream.
                         input-stream digest)
         output-stream (OutputStream/nullOutputStream)]
        (io/copy digest-stream output-stream))
      (format "%032x" (BigInteger. 1 (.digest digest))))
    (catch Exception e
      (far/error ; deal with this
       ::fail-to-hash))))


(defn save-file!
  [temp-file-path save-file-path]
  (try
    (io/copy (io/file temp-file-path)
             (io/file save-file-path))
    (catch Exception e
      (far/signal
        ::fail-to-save)))) ; and this


(def request-schema
  [:map
   ["title" :string]
   ["description" :string]
   ["tags" [:vector :string]]])


(defn bit->mb
  [n]
  (* n 1.25 (math/pow 10 -7)))


(defn check-request-format
  [request]
  (or (malli/validate
        request-schema
        request)
      (far/error
        ::invalid-request-format
        (map->invalid-req
          {:success false
           :message "bad request format"
           :details
           (-> request-schema
               malli-util/closed-schema
               (malli/explain request)
               malli-error/with-spell-checking
               malli-error/humanize)}))))


(defn file-not-too-large?
  "size is in bit, make sure file is not larger than 500 mb"
  [data]
  (or (< (bit->mb (:size data)) 500)
      (far/error ::file-too-large)))


(defn upload-handler
  [request]
  (log/info (pformat request))
  (far/handler-case
    (let [data (:multipart-params request)]
      (log/info (pformat data))
      (->> (check-request-format data)
           (then (fn [_] (file-not-too-large? data)))))
    (::invalid-request-format
      [_condition return]
      (log/warn "invalid-request-format")
      return)))
