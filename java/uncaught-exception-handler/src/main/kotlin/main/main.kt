package main

import java.lang.RuntimeException

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { t, e ->
        println("t = ${t}, e = ${e}")
        println("end handle a exception")
    }
    println("start sleep")
    Thread.sleep(3000)
    println("end sleep")
    throw RuntimeException("throw a runtime exception")
}