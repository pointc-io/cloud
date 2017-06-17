package move.action

import com.google.common.base.Throwables
import com.netflix.hystrix.exception.HystrixTimeoutException
import io.vertx.rxjava.core.Context
import io.vertx.rxjava.core.Vertx
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.rx1.await
import move.*
import move.rx.MoreSingles
import move.rx.SingleOperatorOrderedZip
import rx.Observable
import rx.Scheduler
import rx.Single
import rx.functions.*
import java.util.*
import java.util.concurrent.TimeoutException
import kotlin.coroutines.experimental.CoroutineContext
import io.vertx.circuitbreaker.CircuitBreakerOptions
import io.vertx.rxjava.circuitbreaker.CircuitBreaker
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay


fun isActionTimeout(exception: Throwable): Boolean {
    return when (Throwables.getRootCause(exception)) {
        is HystrixTimeoutException -> true
        is TimeoutException -> true
        else -> false
    }
}

fun main(args: Array<String>) {
    val vertx = Vertx.vertx()
    val breaker = CircuitBreaker.create("my-circuit-breaker", vertx,
            CircuitBreakerOptions()
                    .setMaxFailures(5) // number of failure before opening the circuit
                    .setTimeout(100) // consider a failure if the operation does not succeed in time
                    .setFallbackOnFailure(true) // do we call the fallback on failure
                    .setResetTimeout(10000) // time spent in open state before attempting to re-try
    )



    while (true) {
        for (i in 0..100) {
            breaker.rxExecuteCommand<String> { future ->
                async(VertxContextDispatcher(null, vertx.orCreateContext)) {
                    delay(90)
                    future.complete(Thread.currentThread().name)
                }
                //            async(Unconfined) {
//                delay(0)

//            }
            }.subscribe(
                    { println(it) },
                    { it.printStackTrace() }
            )
        }
        Thread.sleep(500)
    }
}