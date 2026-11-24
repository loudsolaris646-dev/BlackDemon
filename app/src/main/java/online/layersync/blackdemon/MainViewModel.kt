package online.layersync.blackdemon

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import online.layersync.blackdemon.data.*

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    val servers = MutableStateFlow(store.links.mapNotNull { SubscriptionRepo.parseLink(it) })
    val selected = MutableStateFlow(store.selected)
    val provider = MutableStateFlow(store.provider)
    val userInfo = MutableStateFlow(store.userInfo)
    val subUrl = MutableStateFlow(store.subUrl)
    val latency = MutableStateFlow<Int?>(null)
    val busy = MutableStateFlow(false)
    val message = MutableStateFlow("")

    init { if (subUrl.value.isNotBlank()) { refresh(); pingSelected() } }

    fun current(): Server? = servers.value.getOrNull(selected.value)

    fun addSubscription(input: String) {
        val url = SubscriptionRepo.normalize(input)
        if (url == null) { message.value = "Неверная ссылка или токен"; return }
        subUrl.value = url; store.subUrl = url
        refresh(reset = true)
    }

    fun refresh(reset: Boolean = false) = viewModelScope.launch {
        val url = subUrl.value.ifBlank { return@launch }
        busy.value = true; message.value = ""
        try {
            val r = SubscriptionRepo.fetch(url)
            servers.value = r.servers; store.links = r.servers.map { it.link }
            provider.value = r.title; store.provider = r.title
            userInfo.value = r.userInfo; store.userInfo = r.userInfo
            if (reset || selected.value >= r.servers.size) select(0)
            message.value = "Серверов: ${r.servers.size}"
            pingSelected()
        } catch (e: Exception) { message.value = "Ошибка: ${e.message}" }
        busy.value = false
    }

    fun select(i: Int) { selected.value = i; store.selected = i; pingSelected() }

    fun pingSelected() = viewModelScope.launch {
        latency.value = null
        current()?.let { s -> SubscriptionRepo.tcpPing(s.host, s.port).let { latency.value = if (it >= 0) it else null } }
    }

    fun clear() {
        store.clear(); servers.value = emptyList(); provider.value = ""; userInfo.value = ""
        subUrl.value = ""; latency.value = null; message.value = "Подписка удалена"
    }
}
