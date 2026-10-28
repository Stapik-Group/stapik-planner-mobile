package pl.stapik.planner.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityCheckSignatureTest {
    @Test
    fun emptySlotHasEmptySignature() {
        val activity: ActivityDef? = null

        assertEquals("", activity.checkSignature)
    }

    @Test
    fun activityIdIsTheSignatureWhenPresent() {
        assertEquals("a1", ActivityDef(name = "Run", difficulty = "hard", id = "a1").checkSignature)
    }

    @Test
    fun renamedActivityKeepsItsSignature() {
        val before = ActivityDef(name = "Run", difficulty = "hard", id = "a1")
        val after = ActivityDef(name = "Jog", difficulty = "light", id = "a1")

        assertEquals(before.checkSignature, after.checkSignature)
    }

    @Test
    fun nameAndDifficultyAreTheSignatureWithoutId() {
        assertEquals("Run|hard", ActivityDef(name = "Run", difficulty = "hard").checkSignature)
        assertEquals("Run|hard", ActivityDef(name = "Run", difficulty = "hard", id = " ").checkSignature)
    }
}
