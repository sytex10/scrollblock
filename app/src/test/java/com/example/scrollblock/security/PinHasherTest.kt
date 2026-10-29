package com.example.scrollblock.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test
    fun testValidFormat() {
        assertTrue(PinHasher.isValidFormat("1234"))
        assertTrue(PinHasher.isValidFormat("0000"))
        assertTrue(PinHasher.isValidFormat("9999"))

        assertFalse(PinHasher.isValidFormat(""))
        assertFalse(PinHasher.isValidFormat("123"))
        assertFalse(PinHasher.isValidFormat("12345"))
        assertFalse(PinHasher.isValidFormat("abcd"))
        assertFalse(PinHasher.isValidFormat("12 4"))
        assertFalse(PinHasher.isValidFormat("12-4"))
    }

    @Test
    fun testHexConversion() {
        val original = byteArrayOf(0x00, 0x0F, 0x7F, 0xFF.toByte(), 0x42)
        val hex = PinHasher.toHex(original)
        assertEquals("000f7fff42", hex)

        val restored = PinHasher.fromHex(hex)
        assertTrue(original.contentEquals(restored))
    }

    @Test
    fun testHashAndVerifyCorrectPin() {
        val pin = "4821"
        val salt = PinHasher.newSalt()
        assertEquals(16, salt.size)

        val hash = PinHasher.hash(pin, salt)
        assertEquals(32, hash.size) // 256 bits = 32 bytes

        assertTrue(PinHasher.verify(pin, salt, hash))
    }

    @Test
    fun testVerifyWrongPinFails() {
        val correctPin = "1234"
        val wrongPin = "1235"
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash(correctPin, salt)

        assertFalse(PinHasher.verify(wrongPin, salt, hash))
        assertFalse(PinHasher.verify("0000", salt, hash))
        assertFalse(PinHasher.verify("invalid", salt, hash))
    }

    @Test
    fun testDifferentSaltsProduceDifferentHashes() {
        val pin = "5555"
        val salt1 = PinHasher.newSalt()
        val salt2 = PinHasher.newSalt()

        val hash1 = PinHasher.hash(pin, salt1)
        val hash2 = PinHasher.hash(pin, salt2)

        assertFalse(hash1.contentEquals(hash2))
        assertTrue(PinHasher.verify(pin, salt1, hash1))
        assertTrue(PinHasher.verify(pin, salt2, hash2))
        assertFalse(PinHasher.verify(pin, salt1, hash2))
    }
}
