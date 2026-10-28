package pl.stapik.planner.data.edit

import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.stapik.planner.data.model.ActivityDef
import pl.stapik.planner.data.model.DaySlotsWire
import pl.stapik.planner.data.model.Difficulty
import pl.stapik.planner.data.model.PlannerPayload
import pl.stapik.planner.data.model.PlannerSettings

class PlannerEditsTest {
    private class SequentialIds : IdGenerator {
        private var counter = 0

        override fun next(): String = "id${++counter}"
    }

    private val ids = SequentialIds()
    private val hard = ActivityDef(name = "Run", difficulty = "hard", id = "hard")
    private val light = ActivityDef(name = "Walk", difficulty = "light", id = "light")

    private fun payload(
        slots: Int = 3,
        activities: List<ActivityDef> = listOf(hard, light),
        week: List<DaySlotsWire> = emptyList()
    ) = PlannerPayload(PlannerSettings(slots), activities, week)

    private fun EditResult.applied(): PlannerPayload = (this as EditResult.Applied).payload

    private fun PlannerPayload.day(weekday: DayOfWeek): DaySlotsWire =
        weekPlan.first { it.weekday == weekday.name.lowercase() }

    @Test
    fun normalizedPayloadHasTheFullWeekInDesktopOrder() {
        val normalized = PlannerEdits.normalized(payload(slots = 2), ids)

        assertEquals(
            listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"),
            normalized.weekPlan.map { it.weekday }
        )
        assertTrue(normalized.weekPlan.all { it.slots.size == 2 })
    }

    @Test
    fun normalizedPayloadClampsTheSlotCount() {
        assertEquals(8, PlannerEdits.normalized(payload(slots = 99), ids).settings.slots)
        assertEquals(1, PlannerEdits.normalized(payload(slots = -4), ids).settings.slots)
    }

    @Test
    fun normalizedPayloadGivesIdsToActivitiesWithoutOne() {
        val withoutId = ActivityDef(name = "Swim", difficulty = "normal")

        val normalized = PlannerEdits.normalized(payload(activities = listOf(withoutId)), ids)

        assertEquals("id1", normalized.activities.single().id)
    }

    @Test
    fun normalizedPayloadDropsSlotsWhoseActivityIsGone() {
        val gone = ActivityDef(name = "Old", difficulty = "light", id = "gone")
        val week = listOf(DaySlotsWire("monday", listOf(gone, hard)))

        val normalized = PlannerEdits.normalized(payload(week = week), ids)

        assertEquals(listOf(null, hard, null), normalized.day(DayOfWeek.MONDAY).slots)
    }

    @Test
    fun normalizedPayloadMatchesSlotsWithoutIdByName() {
        val legacySlot = ActivityDef(name = "Run", difficulty = "hard")
        val week = listOf(DaySlotsWire("friday", listOf(legacySlot)))

        val normalized = PlannerEdits.normalized(payload(week = week), ids)

        assertEquals(hard, normalized.day(DayOfWeek.FRIDAY).slots[0])
    }

    @Test
    fun assignPutsTheCatalogActivityIntoTheSlot() {
        val result = PlannerEdits.assign(payload(), DayOfWeek.MONDAY, 1, "hard", ids).applied()

        assertEquals(hard, result.day(DayOfWeek.MONDAY).slots[1])
        assertNull(result.day(DayOfWeek.TUESDAY).slots[1])
    }

    @Test
    fun assigningNullClearsTheSlot() {
        val filled = PlannerEdits.assign(payload(), DayOfWeek.MONDAY, 0, "hard", ids).applied()

        val cleared = PlannerEdits.assign(filled, DayOfWeek.MONDAY, 0, null, ids).applied()

        assertNull(cleared.day(DayOfWeek.MONDAY).slots[0])
    }

    @Test
    fun aDayMayReachExactlyTheLimit() {
        val first = PlannerEdits.assign(payload(), DayOfWeek.MONDAY, 0, "hard", ids).applied()

        val second = PlannerEdits.assign(first, DayOfWeek.MONDAY, 1, "hard", ids)

        assertEquals(PlannerEdits.LOAD_LIMIT, PlannerEdits.loadOf((second as EditResult.Applied).payload.day(DayOfWeek.MONDAY)))
    }

    @Test
    fun assigningAboveTheLimitIsRejected() {
        val first = PlannerEdits.assign(payload(), DayOfWeek.MONDAY, 0, "hard", ids).applied()
        val second = PlannerEdits.assign(first, DayOfWeek.MONDAY, 1, "hard", ids).applied()

        val third = PlannerEdits.assign(second, DayOfWeek.MONDAY, 2, "light", ids)

        assertEquals(EditResult.LoadLimitExceeded, third)
    }

    @Test
    fun replacingAnActivityWithALighterOneInAFullDayIsAllowed() {
        val first = PlannerEdits.assign(payload(), DayOfWeek.MONDAY, 0, "hard", ids).applied()
        val second = PlannerEdits.assign(first, DayOfWeek.MONDAY, 1, "hard", ids).applied()

        val replaced = PlannerEdits.assign(second, DayOfWeek.MONDAY, 1, "light", ids)

        assertTrue(replaced is EditResult.Applied)
    }

    @Test
    fun assignRejectsUnknownActivitiesAndSlotsOutOfRange() {
        assertEquals(EditResult.UnknownActivity, PlannerEdits.assign(payload(), DayOfWeek.MONDAY, 0, "nope", ids))
        assertEquals(EditResult.OutOfRange, PlannerEdits.assign(payload(), DayOfWeek.MONDAY, 3, "hard", ids))
        assertEquals(EditResult.OutOfRange, PlannerEdits.assign(payload(), DayOfWeek.MONDAY, -1, "hard", ids))
    }

    @Test
    fun addActivityTrimsTheNameAndGivesItAnId() {
        val result = PlannerEdits.addActivity(payload(activities = emptyList()), "  Swim  ", Difficulty.MEDIUM, ids).applied()

        assertEquals(ActivityDef(name = "Swim", difficulty = "medium", id = "id1"), result.activities.single())
    }

    @Test
    fun addActivityValidatesTheName() {
        assertEquals(EditResult.InvalidName, PlannerEdits.addActivity(payload(), "   ", Difficulty.LIGHT, ids))
        assertEquals(EditResult.InvalidName, PlannerEdits.addActivity(payload(), "x".repeat(61), Difficulty.LIGHT, ids))
        assertEquals(EditResult.NameTaken, PlannerEdits.addActivity(payload(), " run ", Difficulty.LIGHT, ids))
        assertTrue(PlannerEdits.addActivity(payload(), "x".repeat(60), Difficulty.LIGHT, ids) is EditResult.Applied)
    }

    @Test
    fun updateActivityRefreshesTheCopiesInSlots() {
        val planned = PlannerEdits.assign(payload(), DayOfWeek.WEDNESDAY, 0, "light", ids).applied()

        val updated = PlannerEdits.updateActivity(planned, "light", "Stroll", Difficulty.NORMAL, ids).applied()

        val expected = ActivityDef(name = "Stroll", difficulty = "normal", id = "light")
        assertEquals(expected, updated.activities.first { it.id == "light" })
        assertEquals(expected, updated.day(DayOfWeek.WEDNESDAY).slots[0])
    }

    @Test
    fun updateActivityMayKeepItsOwnNameButNotTakeAnotherOne() {
        assertTrue(PlannerEdits.updateActivity(payload(), "hard", "RUN", Difficulty.HARD, ids) is EditResult.Applied)
        assertEquals(EditResult.NameTaken, PlannerEdits.updateActivity(payload(), "hard", "walk", Difficulty.HARD, ids))
        assertEquals(EditResult.UnknownActivity, PlannerEdits.updateActivity(payload(), "nope", "Any", Difficulty.HARD, ids))
    }

    @Test
    fun updateActivityIsRejectedWhenItRaisesADayAboveTheLimit() {
        val overloaded = payload(
            week = listOf(DaySlotsWire("monday", listOf(hard, hard, light)))
        )

        val raised = PlannerEdits.updateActivity(overloaded, "light", "Walk", Difficulty.HARD, ids)
        val lowered = PlannerEdits.updateActivity(overloaded, "hard", "Run", Difficulty.LIGHT, ids)

        assertEquals(EditResult.LoadLimitExceeded, raised)
        assertTrue(lowered is EditResult.Applied)
    }

    @Test
    fun removeActivityClearsItsSlots() {
        val planned = PlannerEdits.assign(payload(), DayOfWeek.MONDAY, 0, "hard", ids).applied()

        val removed = PlannerEdits.removeActivity(planned, "hard", ids).applied()

        assertEquals(listOf(light), removed.activities)
        assertNull(removed.day(DayOfWeek.MONDAY).slots[0])
        assertEquals(EditResult.UnknownActivity, PlannerEdits.removeActivity(planned, "nope", ids))
    }

    @Test
    fun shrinkingTheSlotCountDropsTheLastSlotsAndReportsTheLoss() {
        val planned = PlannerEdits.assign(payload(slots = 3), DayOfWeek.MONDAY, 2, "light", ids).applied()

        val lost = PlannerEdits.lostAssignments(planned, 2, ids)
        val shrunk = PlannerEdits.setSlotCount(planned, 2, ids).applied()

        assertEquals(1, lost)
        assertEquals(2, shrunk.settings.slots)
        assertTrue(shrunk.weekPlan.all { it.slots.size == 2 })
    }

    @Test
    fun growingTheSlotCountAddsEmptySlots() {
        val grown = PlannerEdits.setSlotCount(payload(slots = 2), 5, ids).applied()

        assertTrue(grown.weekPlan.all { it.slots.size == 5 && it.slots.drop(2).all { slot -> slot == null } })
    }

    @Test
    fun slotCountOutsideTheSupportedRangeIsRejected() {
        assertEquals(EditResult.OutOfRange, PlannerEdits.setSlotCount(payload(), 0, ids))
        assertEquals(EditResult.OutOfRange, PlannerEdits.setSlotCount(payload(), 9, ids))
    }
}
