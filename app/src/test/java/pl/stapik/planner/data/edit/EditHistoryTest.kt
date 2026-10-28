package pl.stapik.planner.data.edit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditHistoryTest {
    @Test
    fun undoReturnsThePreviousStateAndEnablesRedo() {
        val history = EditHistory<String>()
        history.record("a")

        assertEquals("a", history.undo("b"))
        assertFalse(history.canUndo)
        assertTrue(history.canRedo)
        assertEquals("b", history.redo("a"))
        assertTrue(history.canUndo)
        assertFalse(history.canRedo)
    }

    @Test
    fun undoAndRedoOnAnEmptyHistoryDoNothing() {
        val history = EditHistory<String>()

        assertNull(history.undo("a"))
        assertNull(history.redo("a"))
    }

    @Test
    fun recordingANewEditDropsTheRedoStack() {
        val history = EditHistory<String>()
        history.record("a")
        history.undo("b")

        history.record("a")

        assertFalse(history.canRedo)
    }

    @Test
    fun theOldestStatesAreForgottenBeyondTheDepth() {
        val history = EditHistory<Int>(maxDepth = 2)
        history.record(1)
        history.record(2)
        history.record(3)

        assertEquals(3, history.undo(4))
        assertEquals(2, history.undo(3))
        assertNull(history.undo(2))
    }

    @Test
    fun clearForgetsBothDirections() {
        val history = EditHistory<String>()
        history.record("a")
        history.undo("b")

        history.clear()

        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
    }
}
