package me.ag2s.benchmark

import android.os.ConditionVariable
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.resume

@RunWith(AndroidJUnit4::class)
class CronetBenchmark {

    abstract class AbsCB {
        private val lock = ReentrantLock()
        private val condition = lock.newCondition()
        var res: String = ""

        abstract fun getResult(): String
        abstract fun onSuccess(result: String)

        fun start() {
            val value = "HHHHHHH"
            lock.withLock {
                condition.await(500, TimeUnit.MILLISECONDS)
            }
            res = value
            onSuccess(value)

        }
    }

    class Cb1 : AbsCB() {
        private val responseFuture = CompletableFuture<String>()

        override fun getResult(): String {
            return responseFuture.get()
        }

        override fun onSuccess(result: String) {
            responseFuture.complete(result)
        }

    }

    class Cb2 : AbsCB() {
        private val mResponseCondition = ConditionVariable()
        override fun getResult(): String {
            mResponseCondition.block()
            return res
        }

        override fun onSuccess(result: String) {
            mResponseCondition.open()
        }

    }

    suspend fun tes3(): String = suspendCancellableCoroutine {
        val cb = object : AbsCB() {
            override fun getResult(): String {
                TODO("Not yet implemented")
            }

            override fun onSuccess(result: String) {
                it.resume(result)
            }
        }
        cb.start()
    }


    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun testAAAA() {
        val cb = Cb1()
        benchmarkRule.measureRepeated {
            cb.start()
        }
    }

    @Test
    fun testBBBB() {
        val cb = Cb2()
        benchmarkRule.measureRepeated {
            cb.start()
        }
    }

    @Test
    fun testCCCC() {

        benchmarkRule.measureRepeated {

            runBlocking { tes3() }
        }
    }


}