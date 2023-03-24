import io.reactivex.rxjava3.subjects.*

fun main() {
    testAllSubject()
}

private fun testAllSubject() {
    testSingleSubject(PublishSubject.create())
    testSingleSubject(AsyncSubject.create())
    testSingleSubject(BehaviorSubject.create())
    testSingleSubject(ReplaySubject.create())
    testSingleSubject(UnicastSubject.create())
}

private fun testSingleSubject(subject: Subject<String>) {
    println("_______________test_${subject.javaClass.name}_start_______________")

    subject.subscribe ({
        println("first subscriber, result = $it")
    }) {

    }

    subject.onNext("onNext 1")
    subject.onNext("onNext 2")
    subject.onNext("onNext 3")

    subject.subscribe ({
        println("second subscriber, result = $it")
    }) {

    }

    subject.onNext("onNext 4")
    subject.onNext("onNext 5")
    subject.onNext("onNext 6")

    subject.subscribe ({
        println("third subscriber, result = $it")
    }) {

    }

    subject.onNext("onNext 7")
    subject.onNext("onNext 8")

    subject.onComplete()

    subject.subscribe ({
        println("forth subscriber, result = $it")
    }) {

    }

    println("_______________test_${subject.javaClass.name}_end_______________")
    repeat(5) { println() }
}