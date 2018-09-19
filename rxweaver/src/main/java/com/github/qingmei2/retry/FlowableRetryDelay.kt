package com.github.qingmei2.retry

import com.github.qingmei2.core.RxThrowable
import io.reactivex.Flowable
import io.reactivex.annotations.NonNull
import io.reactivex.functions.Function
import org.reactivestreams.Publisher
import java.util.concurrent.TimeUnit

class FlowableRetryDelay(
        val retryConfigProvider: (RxThrowable) -> RetryConfig
) : Function<Flowable<Throwable>, Publisher<*>> {

    private var retryCount: Int = 0

    @Throws(Exception::class)
    override fun apply(@NonNull throwableFlowable: Flowable<Throwable>): Publisher<*> {
        return throwableFlowable
                .flatMap(Function<Throwable, Publisher<*>> { error ->
                    if (error !is RxThrowable)
                        return@Function Flowable.error<Any>(error)

                    val (maxRetries, delay, retryTransform) = retryConfigProvider(error)

                    if (++retryCount <= maxRetries) {
                        retryTransform()
                                .flatMapPublisher { retry ->
                                    if (retry)
                                        Flowable.timer(delay.toLong(), TimeUnit.MILLISECONDS)
                                    else
                                        Flowable.error<Any>(error)
                                }
                    } else Flowable.error<Any>(error)
                })
    }
}