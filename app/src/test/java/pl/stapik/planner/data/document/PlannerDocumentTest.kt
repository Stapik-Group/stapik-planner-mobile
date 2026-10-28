package pl.stapik.planner.data.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.stapik.common.model.DocumentEnvelope
import pl.stapik.planner.data.model.ActivityDef
import pl.stapik.planner.data.model.PlannerPayload

class PlannerDocumentTest {
    private val document = """
        {
          "lastUpdate": "2026-10-26T08:00:00Z",
          "payload": {
            "settings": {"slots": 3, "extra": true},
            "activities": [{"id": "a1", "name": "Run", "difficulty": "hard"}],
            "weekPlan": [
              {"weekday": "monday", "slots": [{"id": "a1", "name": "Run", "difficulty": "hard"}, null]}
            ],
            "plan": {"id": "p1", "name": "Default"}
          }
        }
    """.trimIndent()

    @Test
    fun decodesThePayloadAndIgnoresUnknownFields() {
        val payload = PlannerDocument.codec.decode(DocumentEnvelope.parse(document))

        val run = ActivityDef(name = "Run", difficulty = "hard", id = "a1")
        assertEquals(3, payload.settings.slots)
        assertEquals(listOf(run), payload.activities)
        assertEquals("monday", payload.weekPlan.single().weekday)
        assertEquals(listOf(run, null), payload.weekPlan.single().slots)
        assertNull(payload.weekPlan.single().slots[1])
    }

    @Test
    fun emptyPayloadCountsAsNoData() {
        assertTrue(PlannerDocument.codec.isEmpty(PlannerPayload()))
    }

    @Test
    fun payloadWithActivitiesCountsAsData() {
        val payload = PlannerDocument.codec.decode(DocumentEnvelope.parse(document))

        assertFalse(PlannerDocument.codec.isEmpty(payload))
    }

    @Test
    fun encodingKeepsUnknownTopLevelPayloadKeys() {
        val envelope = DocumentEnvelope.parse(document)
        val payload = PlannerDocument.codec.decode(envelope)

        val encoded = PlannerDocument.codec.encode(payload, envelope, "2026-10-27T08:00:00Z")

        assertTrue(encoded.payload.containsKey("plan"))
        assertEquals("2026-10-27T08:00:00Z", encoded.lastUpdate)
    }
}
