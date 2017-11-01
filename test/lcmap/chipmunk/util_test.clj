(ns lcmap.chipmunk.util-test
  (:require [clojure.test :refer :all]
            [lcmap.chipmunk.util :refer :all]))


(deftest re-mapper-test

  (testing "trivial example"
    (let [re #"(?x)
               (?<foo>    [0-9]{4})_
               (?<bar>    [A-Z]{4})
              "
          s "1234_WXYZ"
          rm (re-mapper re [:foo :bar] s)]
      (is (= (:foo rm) "1234"))
      (is (= (:bar rm) "WXYZ"))))

  (testing "real world name"
    (let [re #"(?x) # ignore whitespace and comments in reg-ex definition
               (?<mission>       [(LC08)|(LE07)|(LT05)|(LT04)]{4})_
               (?<projection>    [(CU|AK|HI)]{2})_
               (?<tile>          [0-9]{6})_
               (?<acquired>      [0-9]{8})_
               (?<produced>      [0-9]{8})_
               C(?<collection>   [0-9]{2})_
               V(?<version>      [0-9]{2})_
               (?<band>          [0-9A-Z]+)
               (?<extension>     .*)"
            s "LC08_CU_027009_20130701_20170729_C01_V01_PIXELQA.tif"
            rm (re-mapper re [:mission :band] s)]
      (is (= (:mission rm) "LC08"))
      (is (= (:band rm) "PIXELQA"))
      (is (= (:tile rm) nil #_"because it's not a listed key...")))))


(deftest re-grouper-test
  (testing "is this a better expression of the same function?"
    (let [actual (-> (re-matcher #"(?<foo>[0-9]{4})_(?<bar>[A-Z]{4})" "1234_WXYZ")
                     (re-grouper [:foo :bar]))]
      (is (= (:foo actual) "1234"))
      (is (= (:bar actual) "WXYZ")))))