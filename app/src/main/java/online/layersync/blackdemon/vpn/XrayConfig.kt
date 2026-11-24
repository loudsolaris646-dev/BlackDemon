package online.layersync.blackdemon.vpn

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/** Превращает vless:// или trojan:// ссылку в конфиг Xray с TUN-входом. */
object XrayConfig {
    fun build(link: String): String {
        val u = Uri.parse(link)
        fun q(k: String) = u.getQueryParameter(k)
        val net = q("type") ?: "tcp"
        val sec = q("security") ?: "none"
        val port = if (u.port > 0) u.port else 443

        val stream = JSONObject().put("network", net).put("security", sec)
        when (sec) {
            "reality" -> stream.put("realitySettings", JSONObject()
                .put("serverName", q("sni") ?: "").put("fingerprint", q("fp") ?: "chrome")
                .put("publicKey", q("pbk") ?: "").put("shortId", q("sid") ?: "").put("spiderX", q("spx") ?: ""))
            "tls" -> stream.put("tlsSettings", JSONObject()
                .put("serverName", q("sni") ?: u.host).put("fingerprint", q("fp") ?: "chrome")
                .apply { q("alpn")?.let { put("alpn", JSONArray(it.split(","))) } })
        }
        when (net) {
            "ws" -> stream.put("wsSettings", JSONObject().put("path", q("path") ?: "/")
                .put("headers", JSONObject().put("Host", q("host") ?: "")))
            "grpc" -> stream.put("grpcSettings", JSONObject().put("serviceName", q("serviceName") ?: ""))
            "xhttp", "splithttp" -> stream.put("xhttpSettings", JSONObject()
                .put("path", q("path") ?: "/").put("host", q("host") ?: "").put("mode", q("mode") ?: "auto"))
        }

        val proxy = JSONObject().put("tag", "proxy").put("streamSettings", stream)
        if (link.startsWith("vless://")) {
            val user = JSONObject().put("id", u.userInfo).put("encryption", q("encryption") ?: "none")
            q("flow")?.takeIf { it.isNotEmpty() }?.let { user.put("flow", it) }
            proxy.put("protocol", "vless").put("settings", JSONObject().put("vnext", JSONArray().put(
                JSONObject().put("address", u.host).put("port", port).put("users", JSONArray().put(user)))))
        } else {
            proxy.put("protocol", "trojan").put("settings", JSONObject().put("servers", JSONArray().put(
                JSONObject().put("address", u.host).put("port", port).put("password", u.userInfo))))
        }

        return JSONObject()
            .put("log", JSONObject().put("loglevel", "warning"))
            .put("dns", JSONObject().put("servers", JSONArray().put("1.1.1.1").put("8.8.8.8")))
            .put("inbounds", JSONArray().put(JSONObject().put("tag", "tun").put("protocol", "tun")
                .put("settings", JSONObject().put("name", "xray0").put("MTU", 1500))
                .put("sniffing", JSONObject().put("enabled", true)
                    .put("destOverride", JSONArray().put("http").put("tls").put("quic")))))
            .put("outbounds", JSONArray().put(proxy)
                .put(JSONObject().put("tag", "direct").put("protocol", "freedom"))
                .put(JSONObject().put("tag", "block").put("protocol", "blackhole")))
            .put("routing", JSONObject().put("domainStrategy", "IPIfNonMatch").put("rules", JSONArray().put(
                JSONObject().put("type", "field").put("ip", JSONArray().put("geoip:private")).put("outboundTag", "direct"))))
            .toString()
    }
}
