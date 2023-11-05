package main

fun lambdaFun1(postAction: () -> Unit) {
    postAction()
}


inline fun lambdaFun(crossinline action: (() -> Unit)) {
    lambdaFun1 {
        action()
    }
}