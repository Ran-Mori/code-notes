import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit


fun main() {
    testSingle()
}

private fun testSingle() {

    println("start time = ${System.currentTimeMillis()}，thread name = ${Thread.currentThread().name}")
    val d= Single.just(getDelayString())
        .map {
            println("first map time = ${System.currentTimeMillis()}，thread name = ${Thread.currentThread().name}")
            "first_map_$it"
        }
        .delay(2000, TimeUnit.MILLISECONDS)
        .map {
            println("second map time = ${System.currentTimeMillis()}，thread name = ${Thread.currentThread().name}")
            "second_map_$it"
        }
        .observeOn(Schedulers.newThread())
        .subscribeOn(Schedulers.newThread())
        .subscribe(
            {
                println("onSuccess string = $it onSuccess time = ${System.currentTimeMillis()}，thread name = ${Thread.currentThread().name}")
            },
            {
                println("onError time = ${System.currentTimeMillis()}，thread name = ${Thread.currentThread().name}")
                it.printStackTrace()
            }
        )

    Thread.sleep(8000)
    d.dispose()
}

private fun getDelayString(): String {
    println("start to get string = ${System.currentTimeMillis()}，thread name = ${Thread.currentThread().name}")
    Thread.sleep(1000)
    println("get delay string = ${System.currentTimeMillis()}，thread name = ${Thread.currentThread().name}")
    return "string time ${System.currentTimeMillis()}，thread name = ${Thread.currentThread().name}"
}

