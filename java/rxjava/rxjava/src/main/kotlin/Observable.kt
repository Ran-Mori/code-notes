import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit

/**
 * 结论
 * 1. 被观察者的行为先执行，观察者的行为后执行
 * 2. map这类操作在观察者执行后才执行，且按定义顺序执行
 * 3. subscribe内(也就是观察者)的行为最后执行
 * 4. map这类操作属于被观察者范围
 * 5. 只有所有ObservableSource中的observable都准备就绪了，才会执行接下来的map等操作
 * 6. 所有observable都执行完第一个map才会执行第二个map
 * 7. map每个observable都执行一次，但delay无论多少个都只执行一次
<<<<<<< HEAD
 * 8. 测试最后都加一个Thread.sleep(5000)，不然看不到结果进程都结束了
 */
fun main() {
//    testObservable()
    testObservableCreate()
}


private fun testObservableCreate() {
    Observable.create {
        it.onNext("onNext")
        it.onComplete()
    }
        .observeOn(Schedulers.newThread())
        .subscribeOn(Schedulers.newThread())
        .subscribe(
            {
                println("onSuccess result = $it")
            },
            {
                println("onError")
                it.printStackTrace()
            }
        )

    Thread.sleep(5000)
}

private fun testObservable() {

    println("start time = ${System.currentTimeMillis()}，thread name = ${Thread.currentThread().name}")
    val d= Observable.just(getDelayString(0), getDelayString(1), getDelayString(2))
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

    Thread.sleep(15000)
    d.dispose()
}

private fun getDelayString(index: Int): String {
    println("start to get $index string = ${System.currentTimeMillis()}，thread name = ${Thread.currentThread().name}")
    Thread.sleep(1000)
    println("get $index delay string = ${System.currentTimeMillis()}，thread name = ${Thread.currentThread().name}")
    return "$index string time ${System.currentTimeMillis()}，thread name = ${Thread.currentThread().name}"
}