package main

fun funcNeedBlockParameter(block: () -> Unit) {
    block.invoke()
}

inline fun inlineFuncWithParaBlock(block: () -> Unit) {
    funcNeedBlockParameter(block)
}

