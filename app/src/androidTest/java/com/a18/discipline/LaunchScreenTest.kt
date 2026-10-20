package com.a18.discipline

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.a18.discipline.ui.A18Theme
import com.a18.discipline.ui.LaunchScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LaunchScreenTest {
    @get:Rule val compose=createComposeRule()
    @Test fun loadingFailureOffersRetryInsteadOfEndlessProgress() {
        val error=mutableStateOf<String?>(null);var retries=0
        compose.setContent{A18Theme{LaunchScreen(error.value,{retries++;error.value=null})}}
        compose.onNodeWithText("Daymark").assertIsDisplayed()
        compose.onNodeWithText("Opening your to-do list…").assertIsDisplayed()
        compose.onNodeWithContentDescription("Daymark logo").assertIsDisplayed()
        compose.runOnIdle{error.value="Local records are unavailable."}
        compose.onNodeWithText("Opening your to-do list…").assertDoesNotExist()
        compose.onNodeWithText("Local records are unavailable.").assertIsDisplayed()
        compose.onNodeWithText("Try again").performClick()
        compose.onNodeWithText("Opening your to-do list…").assertIsDisplayed()
        compose.runOnIdle{assertEquals(1,retries)}
    }
}
