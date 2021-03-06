/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-2019 Datadog, Inc.
 */

package com.datadog.android.log.internal.logger

import com.datadog.android.core.internal.data.Writer
import com.datadog.android.core.internal.net.info.NetworkInfoProvider
import com.datadog.android.core.internal.time.TimeProvider
import com.datadog.android.log.internal.domain.Log
import com.datadog.android.log.internal.user.UserInfoProvider

internal class DatadogLogHandler(
    internal val serviceName: String,
    internal val loggerName: String,
    internal val writer: Writer<Log>,
    internal val networkInfoProvider: NetworkInfoProvider?,
    internal val timeProvider: TimeProvider,
    internal val userInfoProvider: UserInfoProvider
) : LogHandler {

    // region LogHandler

    override fun handleLog(
        level: Int,
        message: String,
        throwable: Throwable?,
        attributes: Map<String, Any?>,
        tags: Set<String>
    ) {
        val log = createLog(level, message, throwable, attributes, tags)
        writer.write(log)
    }

    // endregion

    // region Internal

    private fun createLog(
        level: Int,
        message: String,
        throwable: Throwable?,
        attributes: Map<String, Any?>,
        tags: Set<String>
    ): Log {

        return Log(
            serviceName = serviceName,
            level = level,
            message = message,
            timestamp = timeProvider.getServerTimestamp(),
            throwable = throwable,
            attributes = attributes,
            tags = tags.toList(),
            networkInfo = networkInfoProvider?.getLatestNetworkInfo(),
            userInfo = userInfoProvider.getUserInfo(),
            loggerName = loggerName,
            threadName = Thread.currentThread().name
        )
    }

    // endregion
}
