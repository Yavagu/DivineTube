package com.yourtube.app.security

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

internal object PinCrypto {
    private const val SALT_BYTES = 16
    private const val HASH_BYTES = 32
    private const val ITERATIONS = 210_000
    private val random = SecureRandom()

    fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also(random::nextBytes)
    fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, HASH_BYTES * 8)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
    fun matches(pin: String, salt: ByteArray, expected: ByteArray): Boolean =
        MessageDigest.isEqual(derive(pin, salt), expected)
    fun pack(salt: ByteArray, verifier: ByteArray): ByteArray = ByteBuffer.allocate(4 + salt.size + verifier.size)
        .putInt(salt.size).put(salt).put(verifier).array()
    fun unpack(packed: ByteArray): Pair<ByteArray, ByteArray> {
        val buffer = ByteBuffer.wrap(packed)
        val size = buffer.int
        require(size == SALT_BYTES && buffer.remaining() == SALT_BYTES + HASH_BYTES)
        val salt = ByteArray(size).also(buffer::get)
        val verifier = ByteArray(buffer.remaining()).also(buffer::get)
        return salt to verifier
    }
}
