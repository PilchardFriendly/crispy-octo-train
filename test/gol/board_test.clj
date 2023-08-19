(ns gol.board-test
  (:require [clojure.test :as t :refer [testing is]]
            [gol.board :as board]
            [malli.core :as m]
            [gol.generator :refer [generator]]
            [dev.meta :as dev]))

(t/deftest board-test
  (testing "boards"
    (board/with-repository
      (board/with-factory
        (testing "web form"
          (let [id (random-uuid)
                input {:params {:id (str id)}}
                actual (board/request->board-key input)
                expected id]
            (testing "request -> key"
              (is (= expected actual) (str expected " should equal " actual)))))
        (testing "name-binding"
          (let [board' (board/board-model-new)
                id' (:id board')
                expected {id' board'}
                current (board/current-boards)]
            (board/with-repository
              (board/swap-repository! (constantly expected))
              (testing "Swapping Repository"
                (let [actual (board/current-boards)]
                  (is (= expected actual) "Temporary change the boards in flight")
                  (is (not= expected current)))))
            (let [actual (board/current-boards)]
              (testing "Restoring Repository"
                (is (= current actual) "Restore the change")
                (is (not= expected actual))))))
        (testing "creation"
          (testing "of board is valid"
            (let [board' (board/board-model-new)]
              (is (m/validate board/Board board') "new board validates as a BoardFragment")))
          (testing "POST->REDIRECT->GET"
            (let [board' (board/board-model-new)
                  id' (:id board')
                  factory' (generator board' (constantly board/board-model-new)) ;; prime the first response
                  expected {:location (str "/board/" id'),
                            :status 302
                            :event {:newBoard {:id id'}}}]

              (board/with-factory
                (board/swap-factory! (constantly factory'))
                (let [req {:request-method :post :uri "/post" :params {}}
                      actual (board/boards-post req)]
                  (is (= expected actual)))))))))))

(dev/annotate)
