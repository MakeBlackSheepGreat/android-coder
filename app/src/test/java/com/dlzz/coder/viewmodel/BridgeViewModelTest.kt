package com.dlzz.coder.viewmodel

import com.dlzz.coder.viewmodel.BridgeViewModel.Companion.expandCidr
import com.dlzz.coder.viewmodel.BridgeViewModel.Companion.expandScanTarget
import com.dlzz.coder.viewmodel.BridgeViewModel.Companion.ipv4ToLong
import com.dlzz.coder.viewmodel.BridgeViewModel.Companion.longToIpv4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BridgeViewModelTest {

    @Test
    fun ipv4ToLong_convertsValidAddress() {
        assertEquals(0L, ipv4ToLong("0.0.0.0"))
        assertEquals(16909060L, ipv4ToLong("1.2.3.4"))
        assertEquals(2130706433L, ipv4ToLong("127.0.0.1"))
        assertEquals(3232235521L, ipv4ToLong("192.168.0.1"))
    }

    @Test
    fun ipv4ToLong_returnsNullForInvalidAddress() {
        assertNull(ipv4ToLong("not-an-ip"))
        assertNull(ipv4ToLong("1.2.3"))
        assertNull(ipv4ToLong("1.2.3.4.5"))
        assertNull(ipv4ToLong("256.1.1.1"))
        assertNull(ipv4ToLong(""))
    }

    @Test
    fun longToIpv4_convertsBackToAddress() {
        assertEquals("0.0.0.0", longToIpv4(0L))
        assertEquals("1.2.3.4", longToIpv4(16909060L))
        assertEquals("127.0.0.1", longToIpv4(2130706433L))
        assertEquals("192.168.0.1", longToIpv4(3232235521L))
        assertEquals("255.255.255.255", longToIpv4(4294967295L))
    }

    @Test
    fun ipv4RoundTrip_preservesAddress() {
        val addresses = listOf("10.0.0.1", "172.16.5.100", "192.168.1.254", "8.8.8.8")
        for (addr in addresses) {
            val converted = ipv4ToLong(addr)
            val back = longToIpv4(converted!!)
            assertEquals(addr, back)
        }
    }

    @Test
    fun expandScanTarget_singleIpReturnsSingleElement() {
        val result = expandScanTarget("192.168.1.50")
        assertEquals(1, result.size)
        assertEquals("192.168.1.50", result[0])
    }

    @Test
    fun expandScanTarget_wildcardExpandsTo254() {
        val result = expandScanTarget("192.168.1.*")
        assertEquals(254, result.size)
        assertEquals("192.168.1.1", result[0])
        assertEquals("192.168.1.254", result[253])
    }

    @Test
    fun expandScanTarget_threePartExpandsTo254() {
        val result = expandScanTarget("192.168.1")
        assertEquals(254, result.size)
        assertEquals("192.168.1.1", result[0])
    }

    @Test
    fun expandScanTarget_stripsHttpPrefix() {
        val result = expandScanTarget("http://192.168.1.50")
        assertEquals(1, result.size)
        assertEquals("192.168.1.50", result[0])
    }

    @Test
    fun expandScanTarget_stripsPort() {
        val result = expandScanTarget("192.168.1.50:8787")
        assertEquals(1, result.size)
        assertEquals("192.168.1.50", result[0])
    }

    @Test
    fun expandCidr_24ExpandsTo254() {
        val result = expandCidr("192.168.1.0/24")
        assertEquals(254, result.size)
        assertEquals("192.168.1.1", result[0])
        assertEquals("192.168.1.254", result[253])
    }

    @Test
    fun expandCidr_30ExpandsTo2() {
        val result = expandCidr("10.0.0.0/30")
        assertEquals(2, result.size)
    }

    @Test
    fun expandCidr_32ExpandsTo1OrEmpty() {
        val result = expandCidr("10.0.0.5/32")
        assertTrue(result.size <= 2)
    }

    @Test
    fun expandCidr_invalidReturnsEmpty() {
        val result = expandCidr("not-a-cidr")
        assertTrue(result.isEmpty())
    }
}
