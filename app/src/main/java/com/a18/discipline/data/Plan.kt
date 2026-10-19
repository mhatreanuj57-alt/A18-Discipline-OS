package com.a18.discipline.data

import kotlinx.serialization.json.*
import java.time.LocalDate

const val planStart = "2026-10-19"
const val planEnd = "2027-02-18"
fun planOpeningDate(today: LocalDate = LocalDate.now()) = today.toString().coerceIn(planStart,planEnd)

private val oldProjectName = Regex("\\bAgroVision(?:\\s*AI)?\\b",RegexOption.IGNORE_CASE)
// Keep stable record IDs, dates, history and private content when renaming the old project.
fun projectLabels(kind: String, payload: String): String {
    if(kind !in setOf("tasks","schedules","items","logs")) return payload
    fun rename(value: JsonElement): JsonElement = when(value) {
        is JsonObject -> JsonObject(value.mapValues { (key,v) ->
            if(v is JsonPrimitive && v.isString && key in setOf("title","notes","group","subject","category")) {
                JsonPrimitive(if(key=="category" && oldProjectName.matches(v.content)) "Project" else oldProjectName.replace(v.content,"Personal project"))
            } else if(v is JsonObject || v is JsonArray) rename(v) else v
        })
        is JsonArray -> JsonArray(value.map(::rename))
        else -> value
    }
    return rename(json.parseToJsonElement(payload)).toString()
}
