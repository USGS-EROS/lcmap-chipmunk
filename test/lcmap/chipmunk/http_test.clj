(ns lcmap.chipmunk.http-test
  (:require [clojure.test :refer :all]
            [lcmap.chipmunk.shared :as shared]
            [lcmap.chipmunk.fixtures :as fixtures]
            [lcmap.chipmunk.config :as config]
            [lcmap.chipmunk.http :refer :all]
            [org.httpkit.client :as http]))


(use-fixtures :once fixtures/all-fixtures)


(deftest get-base-url-test
  (testing "it exists, and it's nuts"
    (let [resp (shared/go-fish {:url ""})]
      (is (= 200 (:status resp)))
      (is (= ["Chipmunk. It's nuts!"] (-> resp :body))))))


(deftest get-healthy-test
  (testing "it looks good right now"
    (let [resp (shared/go-fish {:url "/healthy"})]
      (is (= 200 (:status resp))))))


(deftest get-registry-test
  (testing "GET /registry"
    (let [resp (shared/go-fish {:url "/registry"})]
      (is (= 200 (:status resp)))
      (is (< 1 (-> resp :body count))))))


(deftest post-registry-test
  (testing "POST /registry"
    (let [layer [{:ubid "LC08_SRB1" :tags ["LC08" "SRB1" "aerosol"]}]
          resp (shared/go-fish {:url "/registry" :method :post :body layer})]
      (is (= 201 (:status resp))))))


(deftest post-source-then-get-results
  (testing "POST /inventory"
    (let [path "/inventory"
          body {:url (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")}
          resp (shared/go-fish {:url path :method :post :body body})
          result (get-in resp [:body])]
      (is (= "027009" (result :tile)))
      (is (= "LC08_SRB1" (result :layer)))
      (is (= "LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif" (result :source)))
      (is (some? (result :extra)))
      (is (some? (result :chips)))
      (is (= 2500 (count (result :chips))))))
  (testing "GET /inventory for source"
    (let [path   "/inventory"
          source "LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif"
          query  {"source" source}
          resp   (shared/go-fish {:url path :query-params query})]
      (is (= 200 (:status resp)))
      (is (= 1   (-> resp :body count)))))
  (testing "GET /chips that were ingested"
    (let [path  "/chips"
          query {"ubid" "LC08_SRB1" "x" "1526415" "y" "1946805" "acquired" "2013/2014"}
          resp  (shared/go-fish {:url "/chips" :query-params query})
          chip  (-> resp :body first)]
      ;; There should only be one chip because only a single source was ingested.
      (is (= 1 (-> resp :body count)))
      ;; The hash value should not vary -- if this test fails, either the source
      ;; data was modified or ingest is saving a different value.
      (is (= "42eaf57aaf20aac1ae04f539816614ae" (-> resp :body first :hash)))
      ;; The point values are tested to ensure the point was properly snapped.
      (is (= 1526415 (chip :x)))
      (is (= 1946805 (chip :y)))
      ;; The time value retrieved from the ARD XML metadata does not have a
      ;; time component. The default behavior is to assume a time of midnight,
      ;; because it is *obviously* not the actual acquisition time.
      (is (= "2013-07-01T00:00:00Z" (chip :acquired))))))
