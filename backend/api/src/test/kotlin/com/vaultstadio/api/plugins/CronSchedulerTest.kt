/**
 * VaultStadio Cron Scheduler Tests
 */

package com.vaultstadio.api.plugins

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for CronScheduler and CronExpression.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CronSchedulerTest {

    @Nested
    @DisplayName("CronExpression Parsing Tests")
    inner class ParsingTests {

        @Test
        fun `should parse valid cron expression with all wildcards`() {
            val cron = CronExpression.parse("* * * * *")

            assertNotNull(cron)
            assertEquals(60, cron.minutes.size) // 0-59
            assertEquals(24, cron.hours.size) // 0-23
            assertEquals(31, cron.daysOfMonth.size) // 1-31
            assertEquals(12, cron.months.size) // 1-12
            assertEquals(7, cron.daysOfWeek.size) // 0-6
        }

        @Test
        fun `should parse cron expression with specific values`() {
            val cron = CronExpression.parse("30 14 1 6 3")

            assertNotNull(cron)
            assertEquals(setOf(30), cron.minutes)
            assertEquals(setOf(14), cron.hours)
            assertEquals(setOf(1), cron.daysOfMonth)
            assertEquals(setOf(6), cron.months)
            assertEquals(setOf(3), cron.daysOfWeek)
        }

        @Test
        fun `should parse cron expression with ranges`() {
            val cron = CronExpression.parse("0-30 9-17 1-15 1-6 1-5")

            assertNotNull(cron)
            assertEquals(31, cron.minutes.size) // 0-30
            assertEquals(9, cron.hours.size) // 9-17
            assertEquals(15, cron.daysOfMonth.size) // 1-15
            assertEquals(6, cron.months.size) // 1-6
            assertEquals(5, cron.daysOfWeek.size) // 1-5
        }

        @Test
        fun `should parse cron expression with steps`() {
            val cron = CronExpression.parse("*/15 */2 * * *")

            assertNotNull(cron)
            assertEquals(setOf(0, 15, 30, 45), cron.minutes)
            assertEquals(setOf(0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22), cron.hours)
        }

        @Test
        fun `should parse cron expression with comma-separated values`() {
            val cron = CronExpression.parse("0,30 8,12,18 * * *")

            assertNotNull(cron)
            assertEquals(setOf(0, 30), cron.minutes)
            assertEquals(setOf(8, 12, 18), cron.hours)
        }

        @Test
        fun `should return null for invalid cron expression with wrong field count`() {
            val cron = CronExpression.parse("* * *")

            assertNull(cron)
        }

        @Test
        fun `should return null for cron expression with too many fields`() {
            val cron = CronExpression.parse("* * * * * *")

            assertNull(cron)
        }

        @Test
        fun `should return null for empty expression`() {
            val cron = CronExpression.parse("")

            assertNull(cron)
        }

        @Test
        fun `should handle range with step`() {
            val cron = CronExpression.parse("0-30/10 * * * *")

            assertNotNull(cron)
            assertEquals(setOf(0, 10, 20, 30), cron.minutes)
        }
    }

    @Nested
    @DisplayName("CronExpression Matching Tests")
    inner class MatchingTests {

        @Test
        fun `should match datetime with all wildcards`() {
            val cron = CronExpression.parse("* * * * *")!!
            val dateTime = LocalDateTime(2024, 6, 15, 14, 30, 0, 0)

            assertTrue(cron.matches(dateTime))
        }

        @Test
        fun `should match specific minute and hour`() {
            val cron = CronExpression.parse("30 14 * * *")!!
            val matching = LocalDateTime(2024, 6, 15, 14, 30, 0, 0)
            val notMatching = LocalDateTime(2024, 6, 15, 14, 31, 0, 0)

            assertTrue(cron.matches(matching))
            assertFalse(cron.matches(notMatching))
        }

        @Test
        fun `should match every 15 minutes`() {
            val cron = CronExpression.parse("*/15 * * * *")!!

            assertTrue(cron.matches(LocalDateTime(2024, 6, 15, 10, 0, 0, 0)))
            assertTrue(cron.matches(LocalDateTime(2024, 6, 15, 10, 15, 0, 0)))
            assertTrue(cron.matches(LocalDateTime(2024, 6, 15, 10, 30, 0, 0)))
            assertTrue(cron.matches(LocalDateTime(2024, 6, 15, 10, 45, 0, 0)))
            assertFalse(cron.matches(LocalDateTime(2024, 6, 15, 10, 10, 0, 0)))
        }

        @Test
        fun `should match specific day of month`() {
            val cron = CronExpression.parse("0 0 15 * *")!!

            assertTrue(cron.matches(LocalDateTime(2024, 6, 15, 0, 0, 0, 0)))
            assertFalse(cron.matches(LocalDateTime(2024, 6, 14, 0, 0, 0, 0)))
        }

        @Test
        fun `should match specific month`() {
            val cron = CronExpression.parse("0 0 1 6 *")!!

            assertTrue(cron.matches(LocalDateTime(2024, 6, 1, 0, 0, 0, 0)))
            assertFalse(cron.matches(LocalDateTime(2024, 7, 1, 0, 0, 0, 0)))
        }
    }

    @Nested
    @DisplayName("Next Execution Tests")
    inner class NextExecutionTests {

        @Test
        fun `should calculate next execution for simple expression`() {
            val cron = CronExpression.parse("0 * * * *")!! // Every hour at minute 0
            val now = LocalDateTime(2024, 6, 15, 14, 30, 0, 0)
                .toInstant(TimeZone.UTC)

            val next = cron.nextExecution(now, TimeZone.UTC)

            assertNotNull(next)
            assertTrue(next > now)
        }

        @Test
        fun `should find next execution for every minute`() {
            val cron = CronExpression.parse("* * * * *")!!
            val now = LocalDateTime(2024, 6, 15, 14, 30, 30, 0)
                .toInstant(TimeZone.UTC)

            val next = cron.nextExecution(now, TimeZone.UTC)

            assertNotNull(next)
            assertTrue(next > now)
        }

        @Test
        fun `should find next execution for specific time`() {
            val cron = CronExpression.parse("30 15 * * *")!! // Every day at 15:30
            val now = LocalDateTime(2024, 6, 15, 14, 0, 0, 0)
                .toInstant(TimeZone.UTC)

            val next = cron.nextExecution(now, TimeZone.UTC)

            assertNotNull(next)
            assertTrue(next > now)
        }
    }

    @Nested
    @DisplayName("CronScheduler Tests")
    inner class SchedulerTests {

        @Test
        fun `schedule should return null for invalid cron expression`() = runTest {
            val job = CronScheduler.schedule(
                scope = this,
                cronExpression = "invalid cron",
                taskName = "test-task",
            ) {
                // Task body
            }

            assertNull(job)
        }

        @Test
        fun `schedule should return Job for valid cron expression`() = runTest {
            val job = CronScheduler.schedule(
                scope = this,
                cronExpression = "* * * * *",
                taskName = "test-task",
            ) {
                // Task body
            }

            assertNotNull(job)
            job.cancel()
        }
    }

    @Nested
    @DisplayName("Common Cron Patterns Tests")
    inner class CommonPatternsTests {

        @Test
        fun `every minute pattern should parse correctly`() {
            val cron = CronExpression.parse("* * * * *")
            assertNotNull(cron)
        }

        @Test
        fun `every 5 minutes pattern should parse correctly`() {
            val cron = CronExpression.parse("*/5 * * * *")
            assertNotNull(cron)
            assertEquals(12, cron.minutes.size)
        }

        @Test
        fun `hourly pattern should parse correctly`() {
            val cron = CronExpression.parse("0 * * * *")
            assertNotNull(cron)
            assertEquals(setOf(0), cron.minutes)
        }

        @Test
        fun `daily at midnight pattern should parse correctly`() {
            val cron = CronExpression.parse("0 0 * * *")
            assertNotNull(cron)
            assertEquals(setOf(0), cron.minutes)
            assertEquals(setOf(0), cron.hours)
        }

        @Test
        fun `weekly on sunday pattern should parse correctly`() {
            val cron = CronExpression.parse("0 0 * * 0")
            assertNotNull(cron)
            assertEquals(setOf(0), cron.daysOfWeek)
        }

        @Test
        fun `monthly on first day pattern should parse correctly`() {
            val cron = CronExpression.parse("0 0 1 * *")
            assertNotNull(cron)
            assertEquals(setOf(1), cron.daysOfMonth)
        }

        @Test
        fun `weekdays only pattern should parse correctly`() {
            val cron = CronExpression.parse("0 9 * * 1-5")
            assertNotNull(cron)
            assertEquals(setOf(1, 2, 3, 4, 5), cron.daysOfWeek)
        }
    }
}
