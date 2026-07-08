package net.sudoer.nipo

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class NipoSubscription(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val link: String = "",
    val lastFetchAtMillis: Long? = null,
    val lastError: String? = null,
)

private fun subscriptionsFile(context: Context): File {
    return File(context.filesDir, "subscriptions.json")
}

fun loadSubscriptions(context: Context): List<NipoSubscription> {
    val file = subscriptionsFile(context)
    if (!file.exists()) return emptyList()

    return try {
        val array = JSONArray(file.readText())
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            NipoSubscription(
                id = obj.optString("id", UUID.randomUUID().toString()),
                name = obj.optString("name", "Subscription ${i + 1}"),
                link = obj.optString("link", ""),
                lastFetchAtMillis = obj.optLong("lastFetchAtMillis", -1L).takeIf { it > 0 },
                lastError = obj.optString("lastError", "").ifBlank { null },
            )
        }
    } catch (e: Exception) {
        LogManager.append("Failed to load subscriptions: ${e.message}")
        emptyList()
    }
}

fun saveSubscriptions(context: Context, subs: List<NipoSubscription>) {
    val array = JSONArray()
    subs.forEach { s ->
        array.put(
            JSONObject()
                .put("id", s.id)
                .put("name", s.name)
                .put("link", s.link)
                .put("lastFetchAtMillis", s.lastFetchAtMillis ?: JSONObject.NULL)
                .put("lastError", s.lastError ?: "")
        )
    }
    subscriptionsFile(context).writeText(array.toString(2))
}

// A subscription link returns plain text: one "nipovpn://…" share link per
// line (same encoding the app already uses for single-profile sharing).
// Blank lines are ignored; a line that fails to parse is skipped rather than
// failing the whole fetch.
fun fetchSubscriptionProfiles(link: String): List<NipoProfile> {
    val connection = URL(link).openConnection() as HttpURLConnection
    connection.connectTimeout = 10_000
    connection.readTimeout = 10_000
    try {
        val body = connection.inputStream.bufferedReader().readText()
        val profiles = body.lines()
            .map { it.trim() }
            .filter { it.startsWith("nipovpn://") }
            .mapNotNull { line -> runCatching { importNipoProfileFromLink(line) }.getOrNull() }
        require(profiles.isNotEmpty()) { "No profiles found at link" }
        return profiles
    } finally {
        connection.disconnect()
    }
}
