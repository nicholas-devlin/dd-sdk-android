package com.datadog.android.domain

import com.datadog.android.BuildConfig
import com.datadog.android.Datadog
import com.datadog.android.log.internal.domain.Log
import com.datadog.android.log.internal.domain.LogSerializer
import com.datadog.android.log.internal.user.UserInfo
import com.datadog.android.utils.forge.Configurator
import com.datadog.tools.unit.assertj.JsonObjectAssert.Companion.assertThat
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Date
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@ForgeConfiguration(Configurator::class)
internal class LogSerializerTest {

    lateinit var underTest: LogSerializer

    @BeforeEach
    fun `set up`() {
        underTest = LogSerializer()
    }

    @Test
    fun `serializes full log as json`(@Forgery fakeLog: Log) {
        val serialized = underTest.serialize(fakeLog)
        assertLogMatches(serialized, fakeLog)
    }

    @Test
    fun `serializes minimal log as json`(@Forgery fakeLog: Log) {
        val minimalLog = fakeLog.copy(
            throwable = null,
            networkInfo = null,
            userInfo = UserInfo(),
            attributes = emptyMap(),
            tags = emptyList()
        )

        val serialized = underTest.serialize(minimalLog)

        assertLogMatches(serialized, minimalLog)
    }

    @Test
    fun `ignores reserved attributes`(@Forgery fakeLog: Log, forge: Forge) {
        // given
        val logWithoutAttributes = fakeLog.copy(attributes = emptyMap())
        val attributes = forge.aMap {
            anElementFrom(*LogSerializer.reservedAttributes) to forge.anAsciiString()
        }.toMap()
        val logWithReservedAttributes = fakeLog.copy(attributes = attributes)

        // when
        val serialized = underTest.serialize(logWithReservedAttributes)

        // then
        assertLogMatches(serialized, logWithoutAttributes)
    }

    @Test
    fun `ignores reserved tags keys`(@Forgery fakeLog: Log, forge: Forge) {
        // given
        val logWithoutTags = fakeLog.copy(tags = emptyList())
        val key = forge.anElementFrom("host", "device", "source", "service")
        val value = forge.aNumericalString()
        val reservedTag = "$key:$value"
        val logWithReservedTags = fakeLog.copy(tags = listOf(reservedTag))

        // when
        val serialized = underTest.serialize(logWithReservedTags)

        // then
        assertLogMatches(serialized, logWithoutTags)
    }

    @Test
    fun `serializes a log with no network info available`(@Forgery fakeLog: Log, forge: Forge) {
        // given
        val logWithoutNetworkInfo = fakeLog.copy(networkInfo = null)

        // when
        val serialized = underTest.serialize(logWithoutNetworkInfo)

        // then
        assertLogMatches(serialized, logWithoutNetworkInfo)
    }

    @Test
    fun `serializes a log with no throwable available`(@Forgery fakeLog: Log, forge: Forge) {
        // given
        val logWithoutThrowable = fakeLog.copy(throwable = null)

        // when
        val serialized = underTest.serialize(logWithoutThrowable)

        // then
        assertLogMatches(serialized, logWithoutThrowable)
    }

    private fun assertLogMatches(
        serializedObject: String,
        log: Log
    ) {
        val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
        assertThat(jsonObject)
            .hasField(LogSerializer.TAG_MESSAGE, log.message)
            .hasField(LogSerializer.TAG_SERVICE_NAME, log.serviceName)
            .hasField(LogSerializer.TAG_STATUS, levels[log.level])
            .hasField(LogSerializer.TAG_LOGGER_NAME, log.loggerName)
            .hasField(LogSerializer.TAG_THREAD_NAME, log.threadName)
            .hasField(LogSerializer.TAG_VERSION_NAME, BuildConfig.VERSION_NAME)
            .hasField(LogSerializer.TAG_APP_VERSION_NAME, Datadog.packageVersion)

        // yyyy-mm-ddThh:mm:ss.SSSZ
        assertThat(jsonObject).hasStringFieldMatching(
            LogSerializer.TAG_DATE,
            "\\d+\\-\\d{2}\\-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z"
        )

        assertNetworkInfoMatches(log, jsonObject)
        assertUserInfoMatches(log, jsonObject)

        assertAttributesMatch(log, jsonObject)
        assertTagsMatch(jsonObject, log)
        assertThrowableMatches(log, jsonObject)
    }

    private fun assertNetworkInfoMatches(log: Log, jsonObject: JsonObject) {
        val info = log.networkInfo
        if (info != null) {
            assertThat(jsonObject).apply {
                hasField(LogSerializer.TAG_NETWORK_CONNECTIVITY, info.connectivity.serialized)
                if (!info.carrierName.isNullOrBlank()) {
                    hasField(LogSerializer.TAG_NETWORK_CARRIER_NAME, info.carrierName)
                } else {
                    doesNotHaveField(LogSerializer.TAG_NETWORK_CARRIER_NAME)
                }
                if (info.carrierId >= 0) {
                    hasField(LogSerializer.TAG_NETWORK_CARRIER_ID, info.carrierId)
                } else {
                    doesNotHaveField(LogSerializer.TAG_NETWORK_CARRIER_ID)
                }
                if (info.upKbps >= 0) {
                    hasField(LogSerializer.TAG_NETWORK_UP_KBPS, info.upKbps)
                } else {
                    doesNotHaveField(LogSerializer.TAG_NETWORK_UP_KBPS)
                }
                if (info.downKbps >= 0) {
                    hasField(LogSerializer.TAG_NETWORK_DOWN_KBPS, info.downKbps)
                } else {
                    doesNotHaveField(LogSerializer.TAG_NETWORK_DOWN_KBPS)
                }
                if (info.strength > Int.MIN_VALUE) {
                    hasField(LogSerializer.TAG_NETWORK_SIGNAL_STRENGTH, info.strength)
                } else {
                    doesNotHaveField(LogSerializer.TAG_NETWORK_SIGNAL_STRENGTH)
                }
            }
        } else {
            assertThat(jsonObject)
                .doesNotHaveField(LogSerializer.TAG_NETWORK_CONNECTIVITY)
                .doesNotHaveField(LogSerializer.TAG_NETWORK_CARRIER_NAME)
                .doesNotHaveField(LogSerializer.TAG_NETWORK_CARRIER_ID)
        }
    }

    private fun assertAttributesMatch(log: Log, jsonObject: JsonObject) {
        log.attributes
            .filter { it.key.isNotBlank() }
            .forEach {
                val value = it.value
                when (value) {
                    null -> assertThat(jsonObject).hasNullField(it.key)
                    is Boolean -> assertThat(jsonObject).hasField(it.key, value)
                    is Int -> assertThat(jsonObject).hasField(it.key, value)
                    is Long -> assertThat(jsonObject).hasField(it.key, value)
                    is Float -> assertThat(jsonObject).hasField(it.key, value)
                    is Double -> assertThat(jsonObject).hasField(it.key, value)
                    is String -> assertThat(jsonObject).hasField(it.key, value)
                    is Date -> assertThat(jsonObject).hasField(it.key, value.time)
                    is JsonObject -> assertThat(jsonObject).hasField(it.key, value)
                    is JsonArray -> assertThat(jsonObject).hasField(it.key, value)
                    else -> assertThat(jsonObject).hasField(it.key, value.toString())
                }
            }
    }

    private fun assertTagsMatch(jsonObject: JsonObject, log: Log) {
        val jsonTagString = (jsonObject[LogSerializer.TAG_DATADOG_TAGS] as? JsonPrimitive)?.asString

        if (jsonTagString.isNullOrBlank()) {
            Assertions.assertThat(log.tags)
                .isEmpty()
        } else {
            val tags = jsonTagString
                .split(',')
                .toList()

            Assertions.assertThat(tags)
                .containsExactlyInAnyOrder(*log.tags.toTypedArray())
        }
    }

    private fun assertThrowableMatches(log: Log, jsonObject: JsonObject) {
        val throwable = log.throwable
        if (throwable != null) {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))

            assertThat(jsonObject)
                .hasField(LogSerializer.TAG_ERROR_KIND, throwable.javaClass.simpleName)
                .hasField(LogSerializer.TAG_ERROR_MESSAGE, throwable.message)
                .hasField(LogSerializer.TAG_ERROR_STACK, sw.toString())
        } else {
            assertThat(jsonObject)
                .doesNotHaveField(LogSerializer.TAG_ERROR_KIND)
                .doesNotHaveField(LogSerializer.TAG_ERROR_MESSAGE)
                .doesNotHaveField(LogSerializer.TAG_ERROR_STACK)
        }
    }

    private fun assertUserInfoMatches(log: Log, jsonObject: JsonObject) {
        val info = log.userInfo
        assertThat(jsonObject).apply {
            if (info.id.isNullOrEmpty()) {
                doesNotHaveField(LogSerializer.TAG_USER_ID)
            } else {
                hasField(LogSerializer.TAG_USER_ID, info.id)
            }
            if (info.name.isNullOrEmpty()) {
                doesNotHaveField(LogSerializer.TAG_USER_NAME)
            } else {
                hasField(LogSerializer.TAG_USER_NAME, info.name)
            }
            if (info.email.isNullOrEmpty()) {
                doesNotHaveField(LogSerializer.TAG_USER_EMAIL)
            } else {
                hasField(LogSerializer.TAG_USER_EMAIL, info.email)
            }
        }
    }

    companion object {
        internal val levels = arrayOf(
            "DEBUG", "DEBUG", "TRACE", "DEBUG", "INFO", "WARN",
            "ERROR", "CRITICAL", "DEBUG", "EMERGENCY"
        )
    }
}
