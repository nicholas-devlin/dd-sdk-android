/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-2020 Datadog, Inc.
 */

package com.datadog.gradle.plugin.benchmark.model

import com.google.gson.annotations.SerializedName

data class BenchmarkMetrics(
    @SerializedName("timeNs") var timeNs: BenchmarkTimeMetrics
)
