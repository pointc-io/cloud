package move.action

import com.google.common.base.Throwables
import kotlinx.coroutines.experimental.delay
import move.common.WireFormat
import move.rx.ordered
import move.rx.parallel
import javax.inject.Inject

/**
 *
 */
@ActionConfig(maxExecutionMillis = 1500000)
@InternalAction
class KAllocateInventory @Inject
constructor() :
        KAction<KAllocateInventory.Request, KAllocateInventory.Reply>() {
    override fun isFallbackEnabled() = true

    suspend override fun recover(caught: Throwable, cause: Throwable, isFallback: Boolean): Reply {
        if (isFallback)
            throw cause

        return reply { code = cause.javaClass.simpleName }
    }

    suspend override fun execute(request: Request): Reply {
//        if (true) {
//            throw RuntimeException("Hahahaha")
//        }

        // Inline blocking block being run asynchronously
        val s = blocking {
            javaClass.simpleName + ": WORKER = " + Thread.currentThread().name
        }
        println(s)

        val blockingParallel = parallel(
                worker {
                    delay(1000)
                    println("Worker 1")
                    Thread.currentThread().name
                },
                worker {
                    delay(1000)
                    println("Worker 2")
                    Thread.currentThread().name
                },
                worker {
                    delay(1000)
                    println("Worker 3")
                    Thread.currentThread().name
                }
        )

        println(blockingParallel)

        val blockingOrdered = ordered(
                worker {
                    delay(1000)
                    println("Worker 1")
                    Thread.currentThread().name
                },
                worker {
                    delay(1000)
                    println("Worker 2")
                    Thread.currentThread().name
                },
                worker {
                    delay(1000)
                    println("Worker 3")
                    Thread.currentThread().name
                }
        )

        println(blockingOrdered)

        val asyncParallel =
                parallel(
                        single {
                            delay(1000)
                            println("Async 1")
                            Thread.currentThread().name
                        },
                        single {
                            delay(1000)
                            println("Async 2")
                            Thread.currentThread().name
                        },
                        single {
                            delay(1000)
                            println("Async 3")
                            Thread.currentThread().name
                        }
                )

        println(asyncParallel)

        val asyncOrdered = ordered(
                single {
                    delay(1000)
                    println("Async 1")
                    Thread.currentThread().name
                },
                single {
                    delay(1000)
                    println("Async 2")
                    Thread.currentThread().name
                },
                single {
                    delay(1000)
                    println("Async 3")
                    Thread.currentThread().name
                }
        )

        println(asyncOrdered)


        val x = WireFormat.parse(Reply::class.java, "{\"code\":\"TEST\"}")
        println(x)

        return reply {
            code = "Back At Cha!"
        }
    }

    class Request @Inject constructor()

    class Reply @Inject constructor() {
        var code: String? = null

        override fun toString(): String {
            return "Reply(code=$code)"
        }
    }
}