package com.bongshaltech.erpinventory.core.courier

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CourierTest {
    private val p = Parcel("S-0007", "Rahim \"Bhai\"", "+8801712345678", "Mirpur 10,\nDhaka", 120_50L, "Rice 5 kg")

    @Test fun steadfastBody() {
        assertEquals(
            "{\"invoice\":\"S-0007\",\"recipient_name\":\"Rahim \\\"Bhai\\\"\",\"recipient_phone\":\"01712345678\"," +
                "\"recipient_address\":\"Mirpur 10,\\nDhaka\",\"cod_amount\":\"121\",\"item_description\":\"Rice 5 kg\"}",
            CourierJson.steadfastOrder(p),
        )
    }

    @Test fun webhookBody() {
        val j = CourierJson.webhook(p, "My Shop")
        assertTrue(j.startsWith("{\"event\":\"parcel.create\",\"shop\":\"My Shop\""))
        assertTrue(j.contains("\"cod_amount\":121,"))
    }

    @Test fun ready() {
        assertTrue(CourierKind.STEADFAST.ready(mapOf("apiKey" to "a", "secretKey" to "b")))
        assertFalse(CourierKind.STEADFAST.ready(mapOf("apiKey" to "a")))
        assertTrue(CourierKind.WEBHOOK.ready(mapOf("url" to "https://hook.example/x")))
        assertFalse(CourierKind.WEBHOOK.ready(mapOf("url" to "http://hook.example/x")))
    }

    @Test fun cod() {
        assertEquals(0L, CourierJson.codTaka(0))
        assertEquals(100L, CourierJson.codTaka(100_00))
        assertEquals("01712345678", CourierJson.localPhone("01712-345678"))
    }
}
