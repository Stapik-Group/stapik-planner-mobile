package pl.stapik.planner.data.edit

import java.time.DayOfWeek
import java.time.Instant
import java.time.temporal.ChronoUnit
import pl.stapik.planner.data.model.ActivityDef
import pl.stapik.planner.data.model.DaySlotsWire
import pl.stapik.planner.data.model.Difficulty
import pl.stapik.planner.data.model.PlannerPayload
import pl.stapik.planner.data.model.PlannerSettings
import pl.stapik.planner.data.model.WeekdayMapping

object PlannerEdits {
    const val LOAD_LIMIT = 100
    const val MAX_NAME_LENGTH = 60

    fun stamped(payload: PlannerPayload, now: Instant): PlannerPayload =
        payload.copy(lastUpdate = now.truncatedTo(ChronoUnit.SECONDS).toString())

    fun loadOf(day: DaySlotsWire): Int =
        day.slots.sumOf { slot -> slot?.let { Difficulty.fromWireValue(it.difficulty).loadPercent } ?: 0 }

    fun normalized(payload: PlannerPayload, ids: IdGenerator = IdGenerator.Secure): PlannerPayload {
        val slotCount = payload.settings.supportedSlots
        val activities = withIds(payload.activities, ids)
        val storedDays = payload.weekPlan
            .mapNotNull { day -> WeekdayMapping.fromWireValue(day.weekday)?.let { it to day } }
            .toMap()
        val week = DayOfWeek.entries.map { weekday ->
            val stored = storedDays[weekday]?.slots.orEmpty()
            DaySlotsWire(
                weekday = WeekdayMapping.toWireValue(weekday),
                slots = List(slotCount) { index -> resolvedSlot(stored.getOrNull(index), activities) }
            )
        }
        return payload.copy(settings = payload.settings.copy(slots = slotCount), activities = activities, weekPlan = week)
    }

    fun assign(
        payload: PlannerPayload,
        weekday: DayOfWeek,
        slotIndex: Int,
        activityId: String?,
        ids: IdGenerator = IdGenerator.Secure
    ): EditResult {
        val base = normalized(payload, ids)
        if (slotIndex !in 0 until base.settings.slots) return EditResult.OutOfRange
        val activity = if (activityId == null) {
            null
        } else {
            base.activities.firstOrNull { it.id == activityId } ?: return EditResult.UnknownActivity
        }
        val day = dayOf(base, weekday)
        val updatedDay = day.copy(slots = day.slots.toMutableList().also { it[slotIndex] = activity })
        if (raisesAboveLimit(day, updatedDay)) return EditResult.LoadLimitExceeded
        return EditResult.Applied(base.copy(weekPlan = base.weekPlan.map { if (it === day) updatedDay else it }))
    }

    fun addActivity(
        payload: PlannerPayload,
        name: String,
        difficulty: Difficulty,
        ids: IdGenerator = IdGenerator.Secure
    ): EditResult {
        val trimmed = name.trim()
        if (!isValidName(trimmed)) return EditResult.InvalidName
        val base = normalized(payload, ids)
        if (base.activities.any { sameName(it.name, trimmed) }) return EditResult.NameTaken
        val used = base.activities.mapNotNull { it.id }.toMutableSet()
        val activity = ActivityDef(name = trimmed, difficulty = difficulty.wireValue, id = freshId(used, ids))
        return EditResult.Applied(base.copy(activities = base.activities + activity))
    }

    fun updateActivity(
        payload: PlannerPayload,
        id: String,
        name: String,
        difficulty: Difficulty,
        ids: IdGenerator = IdGenerator.Secure
    ): EditResult {
        val trimmed = name.trim()
        if (!isValidName(trimmed)) return EditResult.InvalidName
        val base = normalized(payload, ids)
        val index = base.activities.indexOfFirst { it.id == id }
        if (index < 0) return EditResult.UnknownActivity
        val taken = base.activities.withIndex().any { (other, activity) -> other != index && sameName(activity.name, trimmed) }
        if (taken) return EditResult.NameTaken
        val updated = base.activities[index].copy(name = trimmed, difficulty = difficulty.wireValue)
        val week = base.weekPlan.map { day -> day.copy(slots = day.slots.map { slot -> if (slot?.id == id) updated else slot }) }
        if (base.weekPlan.zip(week).any { (before, after) -> raisesAboveLimit(before, after) }) return EditResult.LoadLimitExceeded
        val activities = base.activities.toMutableList().also { it[index] = updated }
        return EditResult.Applied(base.copy(activities = activities, weekPlan = week))
    }

    fun removeActivity(payload: PlannerPayload, id: String, ids: IdGenerator = IdGenerator.Secure): EditResult {
        val base = normalized(payload, ids)
        if (base.activities.none { it.id == id }) return EditResult.UnknownActivity
        val week = base.weekPlan.map { day -> day.copy(slots = day.slots.map { slot -> slot?.takeUnless { it.id == id } }) }
        return EditResult.Applied(base.copy(activities = base.activities.filterNot { it.id == id }, weekPlan = week))
    }

    fun setSlotCount(payload: PlannerPayload, count: Int, ids: IdGenerator = IdGenerator.Secure): EditResult {
        if (count !in PlannerSettings.MIN_SLOTS..PlannerSettings.MAX_SLOTS) return EditResult.OutOfRange
        val base = normalized(payload, ids)
        val week = base.weekPlan.map { day -> day.copy(slots = List(count) { index -> day.slots.getOrNull(index) }) }
        return EditResult.Applied(base.copy(settings = base.settings.copy(slots = count), weekPlan = week))
    }

    fun lostAssignments(payload: PlannerPayload, count: Int, ids: IdGenerator = IdGenerator.Secure): Int =
        normalized(payload, ids).weekPlan.sumOf { day -> day.slots.drop(count).count { it != null } }

    fun plannedSlotCount(payload: PlannerPayload, activityId: String): Int =
        payload.weekPlan.sumOf { day -> day.slots.count { it?.id == activityId } }

    private fun dayOf(payload: PlannerPayload, weekday: DayOfWeek): DaySlotsWire {
        val wire = WeekdayMapping.toWireValue(weekday)
        return payload.weekPlan.first { it.weekday == wire }
    }

    private fun raisesAboveLimit(before: DaySlotsWire, after: DaySlotsWire): Boolean {
        val loadAfter = loadOf(after)
        return loadAfter > LOAD_LIMIT && loadAfter > loadOf(before)
    }

    private fun isValidName(trimmed: String): Boolean = trimmed.isNotEmpty() && trimmed.length <= MAX_NAME_LENGTH

    private fun sameName(first: String, second: String): Boolean = first.trim().equals(second.trim(), ignoreCase = true)

    private fun withIds(activities: List<ActivityDef>, ids: IdGenerator): List<ActivityDef> {
        val used = activities.mapNotNull { it.id }.filter { it.isNotBlank() }.toMutableSet()
        return activities.map { activity ->
            if (activity.id.isNullOrBlank()) activity.copy(id = freshId(used, ids)) else activity
        }
    }

    private fun freshId(used: MutableSet<String>, ids: IdGenerator): String {
        var candidate = ids.next()
        while (!used.add(candidate)) candidate = ids.next()
        return candidate
    }

    private fun resolvedSlot(slot: ActivityDef?, activities: List<ActivityDef>): ActivityDef? = when {
        slot == null -> null
        !slot.id.isNullOrBlank() -> activities.firstOrNull { it.id == slot.id }
        else -> activities.firstOrNull { it.name == slot.name && it.difficulty == slot.difficulty }
            ?: activities.firstOrNull { it.name == slot.name }
    }
}
