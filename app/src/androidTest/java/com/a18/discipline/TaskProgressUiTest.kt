package com.a18.discipline

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.a18.discipline.data.*
import com.a18.discipline.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TaskProgressUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun startRecordsProgressAndCheckboxRecordsCompletion() {
        var task by mutableStateOf(Task(title="Test coding", due="2099-10-20", start="19:00", end="20:00"))
        compose.setContent { A18Theme { TaskCard(task, {}, { task=task.transition(it) }) } }
        compose.onNodeWithText("Start").performClick()
        compose.onNodeWithText("In Progress").assertExists()
        compose.runOnIdle { assertNull(task.completed) }
        compose.onNodeWithContentDescription("Complete Test coding").performClick()
        compose.onNodeWithText("Completed").assertExists()
        compose.runOnIdle { assertNotNull(task.completed) }
    }

    @Test fun firstEveningTaskHasNoNewStartButton() {
        compose.setContent { A18Theme { TaskCard(Task(title="Assignments", due="2026-10-19", start="20:00", end="21:45"), {}, {}) } }
        compose.onNodeWithText("Start").assertDoesNotExist()
        compose.onNodeWithContentDescription("Complete Assignments").assertExists()
    }
}
