package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Subtask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false
) {
    companion object {
        fun listToJson(subtasks: List<Subtask>): String {
            val jsonArray = JSONArray()
            for (item in subtasks) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("title", item.title)
                obj.put("isCompleted", item.isCompleted)
                jsonArray.put(obj)
            }
            return jsonArray.toString()
        }

        fun jsonToList(jsonStr: String?): List<Subtask> {
            if (jsonStr.isNullOrBlank()) return emptyList()
            val list = mutableListOf<Subtask>()
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        Subtask(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            title = obj.optString("title", ""),
                            isCompleted = obj.optBoolean("isCompleted", false)
                        )
                    )
                }
            } catch (_: Exception) {
                // Return empty if parsing failed
            }
            return list
        }
    }
}
