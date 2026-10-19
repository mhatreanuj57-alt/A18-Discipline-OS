package com.a18.discipline

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class NavigationTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun waitForApp() {
        compose.waitUntil(30000){compose.onAllNodesWithText("View: Selected date").fetchSemanticsNodes().isNotEmpty()}
    }
    private fun navigate(page: String) {
        compose.onNodeWithContentDescription("Open navigation").performClick()
        compose.onNodeWithText(page).performScrollTo().performClick()
        compose.waitForIdle()
    }
    @Test fun freshLaunchOpensTheTodoListAfterLoading() {
        waitForApp()
        compose.onNodeWithText("Tasks").assertIsSelected()
        compose.onNodeWithText("Add task").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("View: Selected date").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Opening your to-do list…").assertDoesNotExist()
    }
    @Test fun materialBottomNavigationAndBothSoundControlsWork() {
        waitForApp()
        compose.onNodeWithText("Tasks").performClick()
        compose.onNodeWithText("Add task").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Plan").performClick()
        compose.onNodeWithText("Calendar: Month").assertIsDisplayed()
        compose.onNodeWithText("Focus").performClick()
        compose.onNodeWithText("Focus is a practice").assertIsDisplayed()
        compose.onNodeWithText("More").performClick()
        compose.onNodeWithText("Settings").performScrollTo().performClick()
        compose.onNodeWithText("Starting sound & popups").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ending sound & popups").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Home").performClick()
        compose.onNodeWithText("Open prebuilt to-do list").performScrollTo().assertIsDisplayed()
    }
    @Test fun nativeCalendarAndDailyListShowTheBundledPlan() {
        waitForApp()
        navigate("Calendar")
        compose.onNodeWithText("19",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Date · 2026-10-19").assertIsDisplayed()
        navigate("To-do list")
        compose.onNodeWithText("Date · 2026-10-19").assertIsDisplayed()
        compose.onNodeWithText("Add task").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("View: Selected date").performScrollTo().assertIsDisplayed()
        navigate("Activities");compose.onNodeWithText("Log an activity").assertIsDisplayed()
        navigate("Focus timer");compose.onNodeWithText("Focus is a practice").assertIsDisplayed()
        navigate("30-day challenge");compose.onNodeWithText("30 days. Honest progress.").assertIsDisplayed()
        navigate("Analytics");compose.onNodeWithText("Period: Week").assertIsDisplayed()
    }
    @Test fun dashboardShortcutOpensPlanStartingOctober19AndNoOldProjectNavigation() {
        waitForApp()
        compose.onNodeWithText("Home").performClick()
        compose.waitUntil(30000){compose.onAllNodesWithText("Open prebuilt to-do list").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Open prebuilt to-do list").performScrollTo().performClick()
        compose.onNodeWithText("View: Four-month plan").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Open 19 October").performScrollTo().performClick()
        compose.onNodeWithText("Date · 2026-10-19").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("View: Selected date").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Open navigation").performClick()
        compose.onNodeWithText("AgroVision AI").assertDoesNotExist()
        compose.onNodeWithText("Projects").performScrollTo().performClick()
        compose.onNodeWithText("Personal projects workspace").assertIsDisplayed()
    }
}
