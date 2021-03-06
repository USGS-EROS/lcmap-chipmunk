(ns lcmap.chipmunk.inventory-test
  (:require [clojure.test :refer :all]
            [lcmap.chipmunk.fixtures :as fixtures]
            [lcmap.chipmunk.shared :as shared]
            [lcmap.chipmunk.inventory :as inventory]
            [lcmap.chipmunk.ingest :as ingest]))


(use-fixtures :once fixtures/all-fixtures)


(deftest identify-test
  (testing "generate an ID for a URL"
    (let [url (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")
          actual (inventory/identify url)
          expected "LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif"]
      (is (= actual expected)))))


(deftest search-test
  (let [url (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")
        summary (ingest/save url)]
    (testing "search"
      (testing "by source id"
        (let [query {:source "LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif"}
              result (inventory/search query)]
          (is (= 1 (count result)))))
      (testing "by url"
        (let [query {:url "http://guest:guest@localhost:9080/LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif"}
              result (inventory/search query)]
          (is (= 1 (count result))))))))


(deftest tile->sources-test
  (let [url (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")
        summary (ingest/save url)]
    (testing "tile->sources"
      (testing "by tile"
        (let [query {:tile "027009"}
              result (inventory/tile->sources query)]
          (is (= 1 (count result)))))
      (testing "by tile miss"
        (let [query {:tile "007123"}
              result (inventory/tile->sources query)]
          (is (= 0 (count result))))))))
