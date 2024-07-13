import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers

fun main() {
    testCreate()
//    testFlatMap()
}

private fun testCreate() {
    val disposable: Disposable = Observable.create {
        it.onNext("onNext")
        it.onComplete()
    }
        .observeOn(Schedulers.newThread())
        .subscribeOn(Schedulers.newThread())
        .map {
            it + "1"
        }
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
    disposable.dispose()
}

private fun testFlatMap() {
    Observable.create<String> {
        println("create thread = ${Thread.currentThread().name}, time = ${System.currentTimeMillis()}")
        Thread.sleep(2000L)
        throw RuntimeException("") // 验证抛异常会被下面onErrorReturn给拦住
        it.onNext("create onNext")
    }
        .map {
            println("map thread = ${Thread.currentThread().name}, time = ${System.currentTimeMillis()}")
            throw RuntimeException("") // 验证抛异常会被下面onErrorReturn给拦住
            "${it}map"
        }
        .onErrorReturn {
            println("onErrorReturn thread = ${Thread.currentThread().name}, time = ${System.currentTimeMillis()}")
            "error"
        }
        .flatMap { result ->
            Observable.create<String> {
                println("flatMap thread = ${Thread.currentThread().name}, time = ${System.currentTimeMillis()}")
                throw RuntimeException("") // 验证抛异常会被下面subscribe()的onError拦住
                it.onNext("createSecond + ${result}")
                it.onComplete()
            }
        }
        .subscribeOn(Schedulers.io()) // Observable.create<String>的方法块都会在io线程执行
        .observeOn(Schedulers.newThread()) // 下面所有的Observer都在new thread
        .subscribe(
            {
                println("subscribe onNext thread = ${Thread.currentThread().name}, result = $it, time = ${System.currentTimeMillis()}")
            },
            {
                println("subscribe onError thread = ${Thread.currentThread().name}, time = ${System.currentTimeMillis()}")
                it.printStackTrace()
            }
        )
}