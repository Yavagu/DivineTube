package com.yourtube.app

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun launchesLockedAndHiddenEntryOnlyOpensPinGate() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Opening your library…").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Admin Mode").assertDoesNotExist()
        compose.onNodeWithText("Home · Viewer Mode").assertExists()
        compose.onNodeWithText("Search YouTube").assertDoesNotExist()
        compose.onNodeWithText("DivineTube").performTouchInput { longClick() }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("PIN").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("PIN").assertExists()
        compose.onNodeWithText("Admin Mode").assertDoesNotExist()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("PIN").assertDoesNotExist()
    }

    @Test fun sharedLinkDoesNotApproveOrOpenParentMode() {
        compose.activity.runOnUiThread {
            compose.activity.startActivity(Intent(compose.activity, MainActivity::class.java).apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "https://youtu.be/dQw4w9WgXcQ")
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("PIN").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Admin Mode").assertDoesNotExist()
        compose.onNodeWithText("Review link").assertDoesNotExist()
    }
}
