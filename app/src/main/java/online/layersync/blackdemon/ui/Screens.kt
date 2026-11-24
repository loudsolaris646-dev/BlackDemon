package online.layersync.blackdemon.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.layersync.blackdemon.MainViewModel
import online.layersync.blackdemon.R
import online.layersync.blackdemon.data.Config
import online.layersync.blackdemon.vpn.Status
import online.layersync.blackdemon.vpn.VpnState

enum class Tab(val icon: Int) { DASH(R.drawable.ic_dashboard), SUB(R.drawable.ic_layers), SERVERS(R.drawable.ic_globe), SETTINGS(R.drawable.ic_settings) }

@Composable
fun BlackDemonApp(vm: MainViewModel, onToggle: () -> Unit, onShop: () -> Unit) {
    var tab by remember { mutableStateOf(Tab.DASH) }
    Box(Modifier.fillMaxSize().background(BD.bg)) {
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(
            listOf(BD.redDark.copy(alpha = .5f), Color.Transparent), center = Offset(540f, 800f), radius = 1000f)))
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(bottom = 120.dp)) {
            when (tab) {
                Tab.DASH -> Dashboard(vm, onToggle) { tab = Tab.SERVERS }
                Tab.SUB -> SubscriptionScreen(vm)
                Tab.SERVERS -> ServersScreen(vm) { tab = Tab.DASH }
                Tab.SETTINGS -> SettingsScreen(vm)
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp).fillMaxWidth()
            .clip(RoundedCornerShape(36.dp)).background(Color(0xFF0A0A0D)).border(1.dp, BD.border, RoundedCornerShape(36.dp))
            .padding(10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Tab.values().forEach { t -> NavIcon(t.icon, t == tab) { tab = t } }
            NavIcon(R.drawable.ic_cart, false, onShop)
        }
    }
}

@Composable private fun NavIcon(icon: Int, selected: Boolean, onClick: () -> Unit) =
    Box(Modifier.size(58.dp).clip(RoundedCornerShape(22.dp))
        .background(if (selected) BD.red.copy(alpha = .18f) else Color.Transparent).clickable(onClick = onClick),
        contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), null, Modifier.size(26.dp), tint = if (selected) BD.red else Color(0xFF3A3A45))
    }

@Composable private fun Panel(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(modifier.clip(shape).background(BD.surface).border(1.dp, BD.border, shape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(20.dp)) { content() }
}

@Composable private fun Label(t: String, color: Color = BD.muted) =
    Text(t, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)

@Composable private fun Title(t: String) =
    Text(t, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 16.dp, bottom = 12.dp))

@Composable
private fun Dashboard(vm: MainViewModel, onToggle: () -> Unit, goServers: () -> Unit) {
    val status by VpnState.status.collectAsState()
    val servers by vm.servers.collectAsState()
    val sel by vm.selected.collectAsState()
    val provider by vm.provider.collectAsState()
    val lat by vm.latency.collectAsState()
    val msg by vm.message.collectAsState()
    val cur = servers.getOrNull(sel)

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Дашборд", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(BD.surface).border(1.dp, BD.border, RoundedCornerShape(18.dp))
                .clickable { vm.clear() }, contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_logout), null, tint = BD.muted)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { PowerButton(status, onToggle) }
        if (msg.isNotBlank()) Text(msg, color = BD.muted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), Arrangement.SpaceBetween) {
            Label("ВЫБРАННЫЙ СЕРВЕР")
            Text("ИЗМЕНИТЬ", color = BD.red, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
                modifier = Modifier.clickable(onClick = goServers))
        }
        Spacer(Modifier.height(10.dp))
        Panel(Modifier.fillMaxWidth(), onClick = goServers) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(60.dp).clip(RoundedCornerShape(20.dp)).background(BD.red.copy(alpha = .14f)), Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_globe), null, tint = BD.red)
                }
                Column(Modifier.padding(start = 18.dp).weight(1f)) {
                    Text(cur?.name ?: "Выберите сервер", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(cur?.let { "${it.host}:${it.port}" } ?: "Локация не заполнена", color = BD.muted, fontSize = 14.sp, maxLines = 1)
                }
                Icon(painterResource(R.drawable.ic_chevron), null, tint = BD.muted)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Panel(Modifier.weight(1f)) { Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_layers), null, Modifier.size(22.dp), tint = BD.red)
                    Spacer(Modifier.width(10.dp)); Label("ПРОВАЙДЕР")
                }
                Spacer(Modifier.height(12.dp))
                Text(provider.ifBlank { "—" }, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
            } }
            Panel(Modifier.weight(1f)) { Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_wifi), null, Modifier.size(22.dp), tint = Color(0xFF2ECC71))
                    Spacer(Modifier.width(10.dp)); Label("ЗАДЕРЖКА")
                }
                Spacer(Modifier.height(12.dp))
                Text(lat?.let { "$it мс" } ?: "—", color = Color.White, fontWeight = FontWeight.Bold)
            } }
        }
    }
}

@Composable
private fun PowerButton(status: Status, onClick: () -> Unit) {
    val on = status != Status.OFF
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        0.35f, 1f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "a")
    val ring = if (on) BD.red.copy(alpha = pulse) else Color(0xFF22222C)
    Box(Modifier.size(250.dp).clip(CircleShape).clickable(onClick = onClick), Alignment.Center) {
        Box(Modifier.fillMaxSize().border(14.dp, ring, CircleShape))
        Box(Modifier.size(200.dp).clip(CircleShape).background(
            Brush.radialGradient(listOf(if (on) BD.redDark else Color(0xFF14141C), BD.surface))))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painterResource(R.drawable.ic_demon), null, Modifier.size(96.dp), tint = if (on) BD.red else Color(0xFF4A4A58))
            Label(when (status) { Status.OFF -> "СТАРТ"; Status.CONNECTING -> "ПОДКЛЮЧЕНИЕ"; Status.ON -> "СТОП" },
                if (on) Color.White else BD.muted)
        }
    }
}

@Composable
private fun SubscriptionScreen(vm: MainViewModel) {
    val sub by vm.subUrl.collectAsState()
    val busy by vm.busy.collectAsState()
    val msg by vm.message.collectAsState()
    val info by vm.userInfo.collectAsState()
    var text by remember(sub) { mutableStateOf(sub.ifBlank { Config.SUB_HOST + "/" }) }
    Column(Modifier.padding(horizontal = 24.dp)) {
        Title("Подписка")
        Panel(Modifier.fillMaxWidth()) { Column {
            Label("ССЫЛКА ИЛИ ТОКЕН")
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BD.red, unfocusedBorderColor = BD.border))
            Spacer(Modifier.height(14.dp))
            Button({ vm.addSubscription(text) }, Modifier.fillMaxWidth().height(52.dp), enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = BD.red), shape = RoundedCornerShape(18.dp)) {
                Text(if (busy) "ЗАГРУЗКА…" else "ДОБАВИТЬ / ОБНОВИТЬ", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            if (msg.isNotBlank()) Text(msg, color = BD.muted, modifier = Modifier.padding(top = 12.dp))
            if (info.isNotBlank()) Text(info, color = BD.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        } }
        Text("Ссылка вида ${Config.SUB_HOST}/… в буфере обмена добавится автоматически при открытии приложения.",
            color = BD.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 14.dp, start = 6.dp))
    }
}

@Composable
private fun ServersScreen(vm: MainViewModel, back: () -> Unit) {
    val servers by vm.servers.collectAsState()
    val sel by vm.selected.collectAsState()
    Column(Modifier.padding(horizontal = 24.dp)) {
        Title("Серверы")
        if (servers.isEmpty()) Text("Список пуст — добавьте подписку.", color = BD.muted)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(servers) { i, s ->
                Panel(Modifier.fillMaxWidth(), onClick = { vm.select(i); back() }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("${s.host}:${s.port}", color = BD.muted, fontSize = 13.sp, maxLines = 1)
                        }
                        if (i == sel) Box(Modifier.size(12.dp).clip(CircleShape).background(BD.red))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(vm: MainViewModel) {
    Column(Modifier.padding(horizontal = 24.dp)) {
        Title("Настройки")
        Panel(Modifier.fillMaxWidth()) { Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_demon), null, Modifier.size(56.dp), tint = BD.red)
                Column(Modifier.padding(start = 12.dp)) {
                    Text("BlackDemon", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Версия 1.0 · ${Config.SUB_HOST.removePrefix("https://")}", color = BD.muted, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton({ vm.clear() }, Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(18.dp)) {
                Text("УДАЛИТЬ ПОДПИСКУ", color = BD.red, fontWeight = FontWeight.Bold)
            }
        } }
    }
}
