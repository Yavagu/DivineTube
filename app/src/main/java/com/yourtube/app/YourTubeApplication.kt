package com.yourtube.app

import android.app.Application
import android.content.pm.PackageManager
import androidx.room.Room
import com.yourtube.app.data.local.LibraryDatabase
import com.yourtube.app.data.local.LibraryRepository
import com.yourtube.app.data.youtube.YoutubeApi
import com.yourtube.app.security.PinStore
import java.security.MessageDigest

class YourTubeApplication : Application() {
    val database by lazy {
        Room.databaseBuilder(this, LibraryDatabase::class.java, "library.db")
            .addMigrations(LibraryDatabase.MIGRATION_1_2, LibraryDatabase.MIGRATION_2_3)
            .build()
    }
    val library by lazy { LibraryRepository(database) }
    val pins by lazy { PinStore(this) }
    @Suppress("DEPRECATION")
    val youtube by lazy {
        val signature = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures!!.first()
        val sha1 = MessageDigest.getInstance("SHA-1").digest(signature.toByteArray())
            .joinToString("") { "%02X".format(it) }
        YoutubeApi(BuildConfig.YOUTUBE_API_KEY, packageName, sha1)
    }
}
