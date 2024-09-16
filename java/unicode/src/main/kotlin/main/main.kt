package main

fun main() {
    rainbowFlag()
    shakeHands()
}


private fun rainbowFlag() {
    val firstPart = "\uD83C\uDFF3" // 0x1F3F3
    val secondPart = "\u200D" // 0x200D
    val thirdPart = "\uD83C\uDF08" // 0x1F308

    val firstSecondThird = firstPart + secondPart + thirdPart
    val firstSecond = firstPart + secondPart

    println("start test rainbowFlag")
    println("firstPart = $firstPart, secondPart = $secondPart, thirdPart = $thirdPart")
    println("firstSecondThird = $firstSecondThird, firstSecond = $firstSecond, first = $firstPart")
    println("length of firstSecondThird = ${firstSecondThird.length}")
}

private fun shakeHands() {
    val firstPart = "\uD83E\uDEF1" // 0x1FAF1
    val secondPart = "\uD83C\uDFFB" // 0x1F3FB
    val thirdPart = "\u200D" // 0x200D
    val fourthPart = "\uD83E\uDEF2" // 0x1FAF2
    val fifthPart = "\uD83C\uDFFF" // 0x1F3FC

    val firstSecondThirdFourthFifth = firstPart + secondPart + thirdPart + fourthPart + fifthPart
    val firstSecondThirdFourth = firstPart + secondPart + thirdPart + fourthPart
    val firstSecondThird = firstPart + secondPart + thirdPart
    val firstSecond = firstPart + secondPart

    println("start test shakeHands")
    println("firstPart = $firstPart, secondPart = $secondPart, thirdPart = $thirdPart, fourthPart = $fourthPart, fifthPart = $fifthPart")
    println("firstSecondThirdFourthFifth = $firstSecondThirdFourthFifth, " +
            "firstSecondThirdFourth $firstSecondThirdFourth, " +
            "firstSecondThird = $firstSecondThird, " +
            "firstSecond = $firstSecond, " +
            "first = $firstPart")
    println("length of firstSecondThirdFourthFifth = ${firstSecondThirdFourthFifth.length}")
}