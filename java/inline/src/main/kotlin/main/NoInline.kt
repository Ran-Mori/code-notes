package main

inline fun withNoInlineFunc(noinline block: () -> Unit) {
    println("before call")
    block.invoke()
    println("after call")
}

fun testWithNoInline() {
    withNoInlineFunc {
        println("calling")
    }
}

inline fun withoutNoInlineFunc(block: () -> Unit) {
    println("before call")
    block.invoke()
    println("after call")
}

fun testWithoutNoInline() {
    withoutNoInlineFunc {
        println("calling")
    }
}