package pl.stapik.planner.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PlannerSettingsTest {
    @Test
    fun supportedSlotsStayInsideTheDesktopRange() {
        assertEquals(1, PlannerSettings(slots = Int.MIN_VALUE).supportedSlots)
        assertEquals(1, PlannerSettings(slots = -3).supportedSlots)
        assertEquals(1, PlannerSettings(slots = 0).supportedSlots)
        assertEquals(4, PlannerSettings(slots = 4).supportedSlots)
        assertEquals(8, PlannerSettings(slots = 99).supportedSlots)
        assertEquals(8, PlannerSettings(slots = Int.MAX_VALUE).supportedSlots)
    }
}
