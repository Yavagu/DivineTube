package com.yourtube.app.security

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PinStoreTest {
    @Test fun setupMismatchAndLockoutSurviveStoreRecreation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val file = File(context.cacheDir, "pin-test-${System.nanoTime()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        try {
            val store = PinStore(context, dataStore)
            assertFalse(store.isConfigured())
            store.setup("135790")
            assertTrue(store.isConfigured())
            assertFalse(store.verify("000000").success)
            assertTrue(store.verify("135790").success)

            repeat(4) { assertFalse(store.verify("000000").success) }
            val fifth = store.verify("000000")
            assertFalse(fifth.success)
            assertTrue("fifth failure should lock for at least 30 seconds", fifth.retryAfterSeconds >= 30)

            val recreated = PinStore(context, dataStore)
            val stillLocked = recreated.verify("135790")
            assertFalse(stillLocked.success)
            assertTrue("lockout should survive creating a new store", stillLocked.retryAfterSeconds > 0)
        } finally {
            scope.cancel()
            file.delete()
        }
    }
}
