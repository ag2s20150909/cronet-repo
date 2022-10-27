package me.ag2s.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class AutoBoxingBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    /**
     * Measure the cost of allocating a boxed integer that takes advantage of ART's cache.
     */
    @Test
    fun integerArtCacheAlloc() {
        var i = Integer(1000)
        benchmarkRule.measureRepeated {
            if (i < 100) {
                i = Integer(i.toInt() + 1)
            } else {
                i = Integer(0)
            }
        }
    }

    /**
     * Measure the cost of allocating a boxed integer that falls outside the range of ART's cache.
     */
    @Test
    fun integerAlloc() {
        var i = Integer(1000)
        benchmarkRule.measureRepeated {
            if (i < 1100) {
                i = Integer(i.toInt() + 1)
            } else {
                i = Integer(1000)
            }

        }
    }
}