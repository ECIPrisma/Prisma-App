package com.prisma.mobile

import com.prisma.mobile.util.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConstantsTest {

    @Test
    fun testRolesDefinition() {
        assertEquals("ROLE_TENDERO", Constants.ROLE_TENDERO)
        assertEquals("ROLE_REPARTIDOR", Constants.ROLE_REPARTIDOR)
    }

    @Test
    fun testDefaultBaseUrlTrailingSlash() {
        assertTrue(Constants.DEFAULT_BASE_URL.endsWith("/"))
    }
}
