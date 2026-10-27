package com.vaultstadio.app.data.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class Network119ResultTest {
    @Test fun net119SuccessfulMappingUsesActualData() {
        val success = ApiResult.success(7)
        assertTrue(success.isSuccess()); assertFalse(success.isError())
        assertEquals(7, success.getOrNull())
        assertEquals(ApiResult.Success("file-7"), success.map { "file-$it" })
        assertEquals(ApiResult.Success(14), success.flatMap { ApiResult.success(it * 2) })
        var observed = 0
        assertSame(success, success.onSuccess { observed = it })
        assertEquals(7, observed)
    }
    @Test fun net119FailuresNeverRunSuccessTransforms() {
        for (failure in listOf(ApiResult.error<Int>("NO_GRANT", "private"), ApiResult.networkError<Int>("offline"))) {
            assertFalse(failure.isSuccess()); assertTrue(failure.isError()); assertNull(failure.getOrNull())
            assertSame(failure, failure.map { error("must not execute") })
            assertSame(failure, failure.flatMap { error("must not execute") })
            assertSame(failure, failure.onSuccess { error("must not execute") })
        }
    }
}
