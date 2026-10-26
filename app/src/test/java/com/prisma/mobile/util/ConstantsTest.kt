package com.prisma.mobile.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConstantsTest {
    @Test
    fun urlsDeApiTerminanEnV1() {
        listOf(Constants.DEFAULT_BASE_URL, Constants.SERVER_SABIO_VPN_URL, Constants.EMULATOR_BASE_URL)
            .forEach { assertTrue("$it debe terminar en /api/v1/", it.endsWith("/api/v1/")) }
    }

    @Test
    fun rolesCoincidenConBackend() {
        assertEquals("ROLE_TENDERO", Constants.ROLE_TENDERO)
        assertEquals("ROLE_REPARTIDOR", Constants.ROLE_REPARTIDOR)
    }
}
