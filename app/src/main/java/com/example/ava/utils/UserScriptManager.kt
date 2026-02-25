package com.example.ava.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class UserScript(
    val id: String,
    val name: String,
    val namespace: String,
    val version: String,
    val description: String,
    val matchPatterns: List<String>,
    val code: String,
    val enabled: Boolean = true
)

class UserScriptManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("user_scripts", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    
    fun getAllScripts(): List<UserScript> {
        val scriptsJson = prefs.getString("scripts", "[]") ?: "[]"
        return try {
            json.decodeFromString<List<UserScript>>(scriptsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun saveScript(script: UserScript) {
        val scripts = getAllScripts().toMutableList()
        val existingIndex = scripts.indexOfFirst { it.id == script.id }
        if (existingIndex >= 0) {
            scripts[existingIndex] = script
        } else {
            scripts.add(script)
        }
        prefs.edit().putString("scripts", json.encodeToString(scripts)).apply()
    }
    
    fun deleteScript(scriptId: String) {
        val scripts = getAllScripts().filter { it.id != scriptId }
        prefs.edit().putString("scripts", json.encodeToString(scripts)).apply()
    }
    
    fun toggleScript(scriptId: String, enabled: Boolean) {
        val scripts = getAllScripts().map { 
            if (it.id == scriptId) it.copy(enabled = enabled) else it 
        }
        prefs.edit().putString("scripts", json.encodeToString(scripts)).apply()
    }
    
    fun getMatchingScripts(url: String): List<UserScript> {
        return getAllScripts().filter { script ->
            script.enabled && script.matchPatterns.any { pattern ->
                matchesPattern(url, pattern)
            }
        }
    }
    
    private fun matchesPattern(url: String, pattern: String): Boolean {
        if (pattern == "*") return true
        val regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".")
        return try {
            Regex(regex).containsMatchIn(url)
        } catch (e: Exception) {
            false
        }
    }
    
    companion object {
        fun parseUserScript(scriptContent: String): UserScript? {
            val metadataRegex = Regex("""==UserScript==(.*?)==/UserScript==""", RegexOption.DOT_MATCHES_ALL)
            val metadataMatch = metadataRegex.find(scriptContent) ?: return null
            val metadata = metadataMatch.groupValues[1]
            
            fun extractValue(key: String): String {
                val regex = Regex("""@$key\s+(.+)""")
                return regex.find(metadata)?.groupValues?.get(1)?.trim() ?: ""
            }
            
            fun extractValues(key: String): List<String> {
                val regex = Regex("""@$key\s+(.+)""")
                return regex.findAll(metadata).map { it.groupValues[1].trim() }.toList()
            }
            
            val name = extractValue("name")
            if (name.isEmpty()) return null
            
            val namespace = extractValue("namespace")
            val version = extractValue("version")
            val description = extractValue("description")
            val matchPatterns = extractValues("match") + extractValues("include")
            
            val id = "$namespace:$name".hashCode().toString()
            
            return UserScript(
                id = id,
                name = name,
                namespace = namespace,
                version = version,
                description = description,
                matchPatterns = matchPatterns.ifEmpty { listOf("*") },
                code = scriptContent,
                enabled = true
            )
        }
    }
}
