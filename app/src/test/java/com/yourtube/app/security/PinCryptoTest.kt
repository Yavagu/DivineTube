package com.yourtube.app.security

import org.junit.Assert.*
import org.junit.Test

class PinCryptoTest {
    @Test fun verifierMatchesOnlySamePinAndSalt() {
        val salt = PinCrypto.newSalt()
        val hash = PinCrypto.derive("123456", salt)
        assertTrue(PinCrypto.matches("123456", salt, hash))
        assertFalse(PinCrypto.matches("123457", salt, hash))
        assertFalse(PinCrypto.matches("123456", PinCrypto.newSalt(), hash))
    }
    @Test fun packedVerifierRoundTripsAndRejectsMalformedInput() {
        val salt = PinCrypto.newSalt()
        val hash = PinCrypto.derive("987654", salt)
        val (readSalt, readHash) = PinCrypto.unpack(PinCrypto.pack(salt, hash))
        assertArrayEquals(salt, readSalt)
        assertArrayEquals(hash, readHash)
        try { PinCrypto.unpack(byteArrayOf(1, 2)); fail("expected malformed packed data rejection") }
        catch (_: Exception) { }
    }
}
