package com.example.data.repository

import com.example.domain.model.ChallengeDailyLog
import com.example.domain.model.ChallengeRule
import com.example.domain.model.Subtask
import org.json.JSONArray
import org.json.JSONObject

object JsonMappers {

    fun parseStringList(jsonStr: String?): List<String> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeStringList(list: Collection<String>): String {
        val jsonArray = JSONArray()
        list.forEach { jsonArray.put(it) }
        return jsonArray.toString()
    }

    fun parseStringMap(jsonStr: String?): Map<String, String> {
        if (jsonStr.isNullOrBlank()) return emptyMap()
        return try {
            val obj = JSONObject(jsonStr)
            val map = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = obj.getString(key)
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun serializeStringMap(map: Map<String, String>): String {
        val obj = JSONObject()
        for ((k, v) in map) {
            obj.put(k, v)
        }
        return obj.toString()
    }

    fun parseSubtasks(jsonStr: String?): List<Subtask> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<Subtask>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    Subtask(
                        id = obj.optString("id", System.currentTimeMillis().toString()),
                        title = obj.optString("title", ""),
                        isCompleted = obj.optBoolean("isCompleted", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeSubtasks(list: List<Subtask>): String {
        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("isCompleted", item.isCompleted)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    fun parseChallengeRules(jsonStr: String?): List<ChallengeRule> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<ChallengeRule>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ChallengeRule(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        emoji = obj.optString("emoji", "🎯"),
                        description = obj.optString("description", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeChallengeRules(list: List<ChallengeRule>): String {
        val jsonArray = JSONArray()
        for (rule in list) {
            val obj = JSONObject()
            obj.put("id", rule.id)
            obj.put("title", rule.title)
            obj.put("emoji", rule.emoji)
            obj.put("description", rule.description)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    fun parseChallengeDailyLogs(jsonStr: String?): Map<String, ChallengeDailyLog> {
        if (jsonStr.isNullOrBlank()) return emptyMap()
        return try {
            val root = JSONObject(jsonStr)
            val map = mutableMapOf<String, ChallengeDailyLog>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val date = keys.next()
                val logObj = root.getJSONObject(date)
                val completedIds = mutableSetOf<String>()
                val arr = logObj.optJSONArray("completedRuleIds")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        completedIds.add(arr.getString(i))
                    }
                }
                map[date] = ChallengeDailyLog(
                    dateIso = date,
                    completedRuleIds = completedIds,
                    notes = logObj.optString("notes", "")
                )
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun serializeChallengeDailyLogs(map: Map<String, ChallengeDailyLog>): String {
        val root = JSONObject()
        for ((date, log) in map) {
            val logObj = JSONObject()
            val arr = JSONArray()
            log.completedRuleIds.forEach { arr.put(it) }
            logObj.put("completedRuleIds", arr)
            logObj.put("notes", log.notes)
            root.put(date, logObj)
        }
        return root.toString()
    }
}
