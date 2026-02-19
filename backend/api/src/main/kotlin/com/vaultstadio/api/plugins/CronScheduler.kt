/**
 * VaultStadio Cron Scheduler
 *
 * Lightweight cron expression parser and scheduler for plugin tasks.
 * Supports standard 5-field cron expressions: minute hour day-of-month month day-of-week
 */

package com.vaultstadio.api.plugins

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

private val logger = KotlinLogging.logger {}

/**
 * Represents a parsed cron expression.
 */
data class CronExpression(
    val minutes: Set<Int>,
    val hours: Set<Int>,
    val daysOfMonth: Set<Int>,
    val months: Set<Int>,
    val daysOfWeek: Set<Int>,
) {
    companion object {
        /**
         * Parse a cron expression string.
         *
         * @param expression 5-field cron expression (minute hour day-of-month month day-of-week)
         * @return Parsed CronExpression or null if invalid
         */
        fun parse(expression: String): CronExpression? {
            val parts = expression.trim().split("\\s+".toRegex())
            if (parts.size != 5) {
                logger.warn { "Invalid cron expression: $expression (expected 5 fields, got ${parts.size})" }
                return null
            }

            return try {
                CronExpression(
                    minutes = parseField(parts[0], 0, 59),
                    hours = parseField(parts[1], 0, 23),
                    daysOfMonth = parseField(parts[2], 1, 31),
                    months = parseField(parts[3], 1, 12),
                    daysOfWeek = parseField(parts[4], 0, 6),
                )
            } catch (e: Exception) {
                logger.warn(e) { "Failed to parse cron expression: $expression" }
                null
            }
        }

        private fun parseField(field: String, min: Int, max: Int): Set<Int> {
            val values = mutableSetOf<Int>()

            for (part in field.split(",")) {
                when {
                    part == "*" -> {
                        values.addAll(min..max)
                    }

                    part.contains("/") -> {
                        val splitParts = part.split("/", limit = 2)
                        val range = splitParts[0]
                        val step = splitParts[1].toInt()
                        val rangeBounds = if (range == "*") {
                            min to max
                        } else if (range.contains("-")) {
                            val rangeSplit = range.split("-", limit = 2)
                            rangeSplit[0].toInt() to rangeSplit[1].toInt()
                        } else {
                            range.toInt() to max
                        }
                        for (i in rangeBounds.first..rangeBounds.second step step) {
                            values.add(i)
                        }
                    }

                    part.contains("-") -> {
                        val rangeSplit = part.split("-", limit = 2)
                        values.addAll(rangeSplit[0].toInt()..rangeSplit[1].toInt())
                    }

                    else -> {
                        values.add(part.toInt())
                    }
                }
            }

            return values.filter { it in min..max }.toSet()
        }
    }

    /**
     * Check if this expression matches the given date/time.
     */
    fun matches(dateTime: LocalDateTime): Boolean {
        val dayOfWeek = dateTime.dayOfWeek.ordinal
        val adjustedDayOfWeek = (dayOfWeek + 1) % 7

        return dateTime.minute in minutes &&
            dateTime.hour in hours &&
            dateTime.dayOfMonth in daysOfMonth &&
            dateTime.monthNumber in months &&
            adjustedDayOfWeek in daysOfWeek
    }

    /**
     * Calculate the next execution time after the given instant.
     */
    fun nextExecution(after: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()): Instant {
        var candidate = after.plus(1, DateTimeUnit.MINUTE, timeZone)
        var candidateDateTime = candidate.toLocalDateTime(timeZone)

        candidate = LocalDateTime(
            candidateDateTime.year,
            candidateDateTime.month,
            candidateDateTime.dayOfMonth,
            candidateDateTime.hour,
            candidateDateTime.minute,
            0,
            0,
        ).toInstant(timeZone)

        val maxIterations = 60 * 24 * 365 * 2
        var iterations = 0

        while (iterations < maxIterations) {
            candidateDateTime = candidate.toLocalDateTime(timeZone)

            if (matches(candidateDateTime)) {
                return candidate
            }

            candidate = candidate.plus(1, DateTimeUnit.MINUTE, timeZone)
            iterations++
        }

        logger.warn { "Could not find next execution time for cron expression, falling back to 1 hour" }
        return after.plus(1, DateTimeUnit.HOUR, timeZone)
    }
}

/**
 * Scheduler for cron-based tasks.
 */
object CronScheduler {

    /**
     * Schedule a task to run according to a cron expression.
     *
     * @param scope CoroutineScope to run the task in
     * @param cronExpression Cron expression string
     * @param taskName Name of the task for logging
     * @param task The suspend function to execute
     * @return Job that can be cancelled to stop the scheduled task
     */
    fun schedule(
        scope: CoroutineScope,
        cronExpression: String,
        taskName: String,
        task: suspend () -> Unit,
    ): Job? {
        val cron = CronExpression.parse(cronExpression)
        if (cron == null) {
            logger.error { "Invalid cron expression for task '$taskName': $cronExpression" }
            return null
        }

        return scope.launch {
            val timeZone = TimeZone.currentSystemDefault()
            logger.info { "Scheduled task '$taskName' with cron: $cronExpression" }

            while (isActive) {
                try {
                    val now = Clock.System.now()
                    val nextRun = cron.nextExecution(now, timeZone)
                    val delayMs = (nextRun - now).inWholeMilliseconds

                    logger.debug {
                        "Task '$taskName' next run at ${nextRun.toLocalDateTime(timeZone)} (in ${delayMs / 1000}s)"
                    }

                    if (delayMs > 0) {
                        delay(delayMs)
                    }

                    if (isActive) {
                        logger.info { "Executing scheduled task: $taskName" }
                        try {
                            task()
                            logger.debug { "Task '$taskName' completed successfully" }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            logger.error(e) { "Task '$taskName' failed with error" }
                        }
                    }
                } catch (e: CancellationException) {
                    logger.info { "Scheduled task '$taskName' cancelled" }
                    throw e
                } catch (e: Exception) {
                    logger.error(e) { "Error in scheduler for task '$taskName'" }
                    delay(60_000)
                }
            }
        }
    }
}
