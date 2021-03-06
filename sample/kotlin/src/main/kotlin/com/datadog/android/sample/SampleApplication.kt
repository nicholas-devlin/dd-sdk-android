/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-2019 Datadog, Inc.
 */
package com.datadog.android.sample

import android.app.Application
import android.util.Log
import com.datadog.android.Datadog

class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialise Datadog
        Datadog.initialize(
            this,
            BuildConfig.DD_CLIENT_TOKEN,
            if (BuildConfig.DD_OVERRIDE_URL.isEmpty()) Datadog.DATADOG_US else BuildConfig.DD_OVERRIDE_URL
        )
        Datadog.setVerbosity(Log.VERBOSE)
    }
}
