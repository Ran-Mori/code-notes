package main


var value: Int = 3
fun <T> T.applyWithoutInline(block: T.() -> Unit): T {
    block()
    return this
}

fun callWithoutInline(): Int {
    return value.applyWithoutInline { println("callWithoutInline") }
}

inline fun <T> T.applyWithInline(block: T.() -> Unit): T {
    block()
    return this
}

fun callWithInline(): Int {
    return value.applyWithInline { println("callWithInline") }
}



