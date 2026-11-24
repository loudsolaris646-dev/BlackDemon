package online.layersync.blackdemon.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

object Config {
    const val SUB_HOST = "https://sub.layersync.online"   // базовая ссылка подписки
    const val SHOP_URL = "https://layersync.online"       // кнопка «корзина» открывает этот сайт
    const val USER_AGENT = "v2rayNG/1.10.0"                // Remnawave отдаёт список ссылок для этого UA
}

data class Server(val name: String, val host: String, val port: Int, val link: String)
data class Fetched(val servers: List<Server>, val title: String, val userInfo: String)

class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("blackdemon", Context.MODE_PRIVATE)
    var subUrl: String
        get() = sp.getString("sub", "") ?: ""
        set(v) = sp.edit().putString("sub", v).apply()
    var links: List<String>
        get() = (sp.getString("links", "") ?: "").lines().filter { it.isNotBlank() }
        set(v) = sp.edit().putString("links", v.joinToString("\n")).apply()
    var selected: Int
        get() = sp.getInt("selected", 0)
        set(v) = sp.edit().putInt("selected", v).apply()
    var provider: String
        get() = sp.getString("provider", "") ?: ""
        set(v) = sp.edit().putString("provider", v).apply()
    var userInfo: String
        get() = sp.getString("userinfo", "") ?: ""
        set(v) = sp.edit().putString("userinfo", v).apply()
    fun clear() = sp.edit().clear().apply()
}

object SubscriptionRepo {
    /** Принимает полную ссылку, blackdemon://import/..., или просто токен (short UUID) — и добавляет sub.layersync.online. */
    fun normalize(input: String): String? {
        val t = input.trim()
        return when {
            t.startsWith("blackdemon://import/") -> Uri.decode(t.removePrefix("blackdemon://import/"))
            t.startsWith("https://") -> t
            Regex("[A-Za-z0-9_-]{6,}").matches(t) -> "${Config.SUB_HOST}/$t"
            else -> null
        }
    }

    suspend fun fetch(url: String): Fetched = withContext(Dispatchers.IO) {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000; c.readTimeout = 15_000
        c.setRequestProperty("User-Agent", Config.USER_AGENT)
        try {
            if (c.responseCode != 200) error("HTTP ${c.responseCode}")
            val body = c.inputStream.bufferedReader().readText().trim()
            val servers = parse(body)
            if (servers.isEmpty()) error("В подписке нет серверов")
            var title = c.getHeaderField("profile-title") ?: URL(url).host
            if (title.startsWith("base64:")) title = String(Base64.decode(title.removePrefix("base64:"), Base64.DEFAULT))
            Fetched(servers, title, c.getHeaderField("subscription-userinfo") ?: "")
        } finally { c.disconnect() }
    }

    fun parse(body: String): List<Server> {
        val text = if (body.contains("://")) body else
            try { String(Base64.decode(body, Base64.DEFAULT)) } catch (e: Exception) { String(Base64.decode(body, Base64.URL_SAFE)) }
        return text.lines().mapNotNull { parseLink(it) }
    }

    fun parseLink(l: String): Server? {
        val s = l.trim()
        if (!(s.startsWith("vless://") || s.startsWith("trojan://"))) return null
        val u = Uri.parse(s)
        val host = u.host ?: return null
        return Server(u.fragment ?: host, host, if (u.port > 0) u.port else 443, s)
    }

    suspend fun tcpPing(host: String, port: Int): Int = withContext(Dispatchers.IO) {
        try {
            val t = System.nanoTime()
            Socket().use { it.connect(InetSocketAddress(host, port), 3000) }
            ((System.nanoTime() - t) / 1_000_000).toInt()
        } catch (e: Exception) { -1 }
    }
}
