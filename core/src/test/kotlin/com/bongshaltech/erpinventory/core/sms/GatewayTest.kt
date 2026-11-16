package com.bongshaltech.erpinventory.core.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GatewayTest {
    @Test fun numbers() {
        assertEquals("8801712345678", Gateway.bdNumber("01712-345678"))
        assertEquals("8801712345678", Gateway.bdNumber("+880 1712 345678"))
        assertEquals("8801712345678", Gateway.bdNumber("8801712345678"))
        assertNull(Gateway.bdNumber("01212345678"))
        assertNull(Gateway.bdNumber("12345"))
    }

    @Test fun alphaUrl() {
        val u = Gateway.url(GatewayConfig(Provider.ALPHA, "k&1"), "8801712345678", "Tk 50 & thanks")
        assertEquals("https://api.sms.net.bd/sendsms?api_key=k%261&msg=Tk+50+%26+thanks&to=8801712345678", u)
    }

    @Test fun customUrl() {
        val c = GatewayConfig(Provider.CUSTOM, "abc", "SHOP", "https://x.bd/send?k={key}&to={number}&t={message}&s={sender}")
        assertTrue(c.ready)
        assertEquals("https://x.bd/send?k=abc&to=8801712345678&t=hi+there&s=SHOP", Gateway.url(c, "8801712345678", "hi there"))
        assertFalse(GatewayConfig(Provider.CUSTOM, "", customUrl = "http://x.bd/{number}/{message}").ready)
    }

    @Test fun answers() {
        assertNull(Gateway.refusal(Provider.ALPHA, 200, """{"error":0,"msg":"Request successfully submitted"}"""))
        assertEquals("Invalid API key", Gateway.refusal(Provider.ALPHA, 200, """{"error":405,"msg":"Invalid API key"}"""))
        assertNull(Gateway.refusal(Provider.BULKSMSBD, 200, """{"response_code":202,"success_message":"SMS Submitted Successfully"}"""))
        assertEquals("code 1007", Gateway.refusal(Provider.BULKSMSBD, 200, """{"response_code":1007,"error_message":""}"""))
        assertNull(Gateway.refusal(Provider.CUSTOM, 200, "ok"))
        assertTrue(Gateway.refusal(Provider.CUSTOM, 500, "down")!!.startsWith("HTTP 500"))
    }
}
