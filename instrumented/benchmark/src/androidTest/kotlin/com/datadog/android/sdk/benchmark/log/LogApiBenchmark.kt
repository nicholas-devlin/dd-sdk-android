/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-2019 Datadog, Inc.
 */

package com.datadog.android.sdk.benchmark.log

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.datadog.android.Datadog
import com.datadog.android.log.Logger
import com.datadog.tools.unit.forge.aThrowable
import com.datadog.tools.unit.invokeMethod
import fr.xgouchet.elmyr.junit4.ForgeRule
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LogApiBenchmark {
    @get:Rule
    val benchmark = BenchmarkRule()
    @get:Rule
    val forge = ForgeRule()

    lateinit var testedLogger: Logger

    lateinit var mockWebServer: MockWebServer

    @Before
    fun setUp() {
        mockWebServer = MockWebServer().apply { start() }
        mockWebServer.setDispatcher(object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return mockResponse()
            }
        })
        val fakeEndpoint = mockWebServer.url("/").toString().removeSuffix("/")

        val context = InstrumentationRegistry.getInstrumentation().context
        Datadog.initialize(context, "NO_TOKEN", fakeEndpoint)

        testedLogger = Logger.Builder()
            .setDatadogLogsEnabled(true)
            .setNetworkInfoEnabled(true)
            .build()
    }

    @After
    fun tearDown() {
        Datadog.invokeMethod("stop")
        mockWebServer.shutdown()
    }

    @Test
    fun benchmark_create_one_log() {
        benchmark.measureRepeated {
            val message = runWithTimingDisabled { forge.anAlphabeticalString() }
            testedLogger.i(message)
        }
    }

    @Test
    fun benchmark_create_one_log_with_throwable() {
        benchmark.measureRepeated {
            val (message, throwable) = runWithTimingDisabled {
                forge.anAlphabeticalString() to forge.aThrowable()
            }
            testedLogger.e(message, throwable)
        }
    }

    @Test
    fun benchmark_create_one_log_with_attributes() {
        for (i in 1..16) {
            testedLogger.addAttribute(forge.anAlphabeticalString(), forge.anHexadecimalString())
        }
        benchmark.measureRepeated {
            val (message, throwable) = runWithTimingDisabled {
                forge.anAlphabeticalString() to forge.aThrowable()
            }
            testedLogger.e(message, throwable)
        }
    }

    @Test
    fun benchmark_create_one_log_with_tags() {
        for (i in 1..8) {
            testedLogger.addTag(forge.anAlphabeticalString(), forge.anHexadecimalString())
        }
        benchmark.measureRepeated {
            val (message, throwable) = runWithTimingDisabled {
                forge.anAlphabeticalString() to forge.aThrowable()
            }
            testedLogger.e(message, throwable)
        }
    }

    // region Internal

    private fun mockResponse(): MockResponse {
        return MockResponse()
            .setResponseCode(200)
            .setBody("{}")
    }

    // endregion

    companion object {
        const val MAX_LOGS_PER_BATCH = 500
        const val MEDIUM_ITERATIONS = MAX_LOGS_PER_BATCH / 2
        const val BIG_ITERATIONS = MAX_LOGS_PER_BATCH
    }
}
