package com.apexfission.android.yolo.engine

/**
 * A sealed class for specifying the number of threads to be used by the TFLite interpreter.
 * This allows for flexible configuration, from using a predefined fraction of available
 * processor cores to setting a specific, custom count. The resulting number of threads is
 * always at least 1.
 */
sealed class NumThreads {
    /** Uses a default of 3 threads, capped by the number of available processors. */
    object Default : NumThreads()

    /** Uses 25% of the available processors. */
    object Quarter : NumThreads()

    /** Uses 50% of the available processors. */
    object Half : NumThreads()

    /** Uses 75% of the available processors. */
    object ThreeQuarters : NumThreads()

    /**
     * Uses a custom percentage of the available processors.
     * @param percentage The fraction of available cores to use (e.g., 0.6f for 60%).
     */
    data class CustomPercentage(val percentage: Float) : NumThreads()

    /**
     * Uses a specific number of threads.
     * @param count The exact number of threads to use.
     */
    data class CustomCount(val count: Int) : NumThreads()

    /**
     * Calculates and returns the actual number of threads based on the selected configuration
     * and the number of available processors on the device.
     * The result is always coerced to be at least 1.
     * @return The calculated integer number of threads.
     */
    fun toInt(): Int {
        val availableProcessors = Runtime.getRuntime().availableProcessors()
        return when (this) {
            Default -> 3.coerceAtMost(availableProcessors)
            Quarter -> (availableProcessors * 0.25f).toInt().coerceAtLeast(1)
            Half -> (availableProcessors * 0.5f).toInt().coerceAtLeast(1)
            ThreeQuarters -> (availableProcessors * 0.75f).toInt().coerceAtLeast(1)
            is CustomPercentage -> (availableProcessors * percentage).toInt().coerceAtLeast(1)
            is CustomCount -> count.coerceAtLeast(1)
        }
    }

    /**
     * Provides a human-readable string representation of the thread configuration.
     * @return A string describing the configuration (e.g., "50%", "Custom (2)").
     */
    override fun toString(): String {
        return when (this) {
            Default -> "Default"
            Quarter -> "25%"
            Half -> "50%"
            ThreeQuarters -> "75%"
            is CustomPercentage -> "Custom (${(percentage * 100).toInt()}%)"
            is CustomCount -> "Custom (${count})"
        }
    }
}
