(ns pgbot.test.connection
  (:require [clojure.test :refer [use-fixtures deftest is]])
  (:use pgbot.connection))

(def connection (create "irc.freenode.net" 6667 "pgbot"))

(deftest create-returns-a-map-with-socket-in-out-and-nick
  (is (= [true true true true]
         (map #(contains? connection %) [:socket :in :out :nick]))))

(deftest connection-has-a-socket
  (is (= java.net.Socket
         (class (connection :socket)))))

(deftest connection-has-a-reader
  (is (= java.io.BufferedReader
         (class (connection :in)))))

(deftest connection-has-a-writer
  (is (= java.io.BufferedWriter
         (class (connection :out)))))

(deftest connection-has-a-nick
  (is (= "pgbot"
         (connection :nick))))

(.close (connection :socket))
