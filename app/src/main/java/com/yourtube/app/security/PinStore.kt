package com.yourtube.app.security

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.pinDataStore by preferencesDataStore(name = "parent_security")
data class PinResult(val success: Boolean, val retryAfterSeconds: Long = 0)

/** Stores an AES-GCM protected salted PBKDF2 verifier and persistent lockout state. */
class PinStore(private val context: Context, private val dataStore: DataStore<Preferences> = context.pinDataStore) {
    suspend fun isConfigured(): Boolean = dataStore.data.first()[PIN_BLOB] != null

    suspend fun setup(pin: String) = processMutex.withLock {
        require(PIN_PATTERN.matches(pin)) { "PIN must contain 6 to 12 digits" }
        require(dataStore.data.first()[PIN_BLOB] == null) { "PIN already configured" }
        val salt = PinCrypto.newSalt()
        val saltedVerifier = withContext(Dispatchers.Default) { PinCrypto.derive(pin, salt) }
        val ciphertext = encrypt(PinCrypto.pack(salt, saltedVerifier))
        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()
        val boot = bootCount()
        dataStore.edit { prefs ->
            prefs[PIN_BLOB] = android.util.Base64.encodeToString(ciphertext, android.util.Base64.NO_WRAP)
            prefs[FAILURES] = 0L
            prefs[BLOCKED_ELAPSED] = 0L
            prefs[BLOCKED_WALL] = 0L
            prefs[LAST_WALL] = nowWall
            prefs[LAST_ELAPSED] = nowElapsed
            prefs[LAST_BOOT_COUNT] = boot
        }
    }

    suspend fun verify(pin: String): PinResult = processMutex.withLock {
        if (!PIN_PATTERN.matches(pin)) return@withLock PinResult(false)
        val prefs = dataStore.data.first()
        val encoded = prefs[PIN_BLOB] ?: return@withLock PinResult(false)
        val wallNow = System.currentTimeMillis()
        val elapsedNow = SystemClock.elapsedRealtime()
        val boot = bootCount()
        val state = prefs.toAttemptState()
        val checked = AttemptPolicy.check(state, wallNow, elapsedNow, boot)
        if (checked.retryAfterMillis > 0) {
            dataStore.edit { checked.state.writeTo(it) }
            return@withLock PinResult(false, (checked.retryAfterMillis + 999) / 1000)
        }

        // Commit the attempt reservation before the expensive verifier operation.
        val reserved = AttemptPolicy.reserveFailure(checked.state, wallNow, elapsedNow, boot)
        dataStore.edit { reserved.writeTo(it) }
        val matches = try {
            val packed = decrypt(android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP))
            val (salt, expected) = PinCrypto.unpack(packed)
            withContext(Dispatchers.Default) { PinCrypto.matches(pin, salt, expected) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
        if (!matches) return@withLock PinResult(false, (reserved.blockedUntilElapsed - elapsedNow).coerceAtLeast(0L).let { (it + 999) / 1000 })

        val reset = reserved.copy(failures = 0, blockedUntilElapsed = 0, blockedUntilWall = 0,
            lastWall = System.currentTimeMillis(), lastElapsed = SystemClock.elapsedRealtime(), lastBootCount = bootCount())
        dataStore.edit { reset.writeTo(it) }
        PinResult(true)
    }

    private fun encrypt(clear: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return cipher.iv + cipher.doFinal(clear)
    }

    private fun decrypt(data: ByteArray): ByteArray {
        require(data.size > 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, data.copyOfRange(0, 12)))
        return cipher.doFinal(data.copyOfRange(12, data.size))
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build())
        return generator.generateKey()
    }

    private fun bootCount(): Long = try {
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT).toLong()
    } catch (_: Exception) { -1L }

    private fun AttemptState.writeTo(prefs: androidx.datastore.preferences.core.MutablePreferences) {
        prefs[FAILURES] = failures
        prefs[BLOCKED_ELAPSED] = blockedUntilElapsed
        prefs[BLOCKED_WALL] = blockedUntilWall
        prefs[LAST_WALL] = lastWall
        prefs[LAST_ELAPSED] = lastElapsed
        prefs[LAST_BOOT_COUNT] = lastBootCount
    }

    private fun androidx.datastore.preferences.core.Preferences.toAttemptState() = AttemptState(
        failures = this[FAILURES] ?: 0L,
        blockedUntilElapsed = this[BLOCKED_ELAPSED] ?: 0L,
        blockedUntilWall = this[BLOCKED_WALL] ?: 0L,
        lastWall = this[LAST_WALL] ?: 0L,
        lastElapsed = this[LAST_ELAPSED] ?: 0L,
        lastBootCount = this[LAST_BOOT_COUNT] ?: -1L
    )

    companion object {
        private val processMutex = Mutex()
        private val PIN_BLOB = stringPreferencesKey("encrypted_verifier")
        private val FAILURES = longPreferencesKey("failures")
        private val BLOCKED_ELAPSED = longPreferencesKey("blocked_until_elapsed")
        private val BLOCKED_WALL = longPreferencesKey("blocked_until_wall")
        private val LAST_WALL = longPreferencesKey("last_wall")
        private val LAST_ELAPSED = longPreferencesKey("last_elapsed")
        private val LAST_BOOT_COUNT = longPreferencesKey("last_boot_count")
        private val PIN_PATTERN = Regex("^[0-9]{6,12}$")
        private const val KEY_ALIAS = "yourtube_parent_pin_v1"
    }
}





