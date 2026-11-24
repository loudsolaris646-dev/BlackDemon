package online.layersync.blackdemon

import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import online.layersync.blackdemon.data.Config
import online.layersync.blackdemon.ui.BlackDemonApp
import online.layersync.blackdemon.ui.BlackDemonTheme
import online.layersync.blackdemon.vpn.BlackDemonVpnService
import online.layersync.blackdemon.vpn.Status
import online.layersync.blackdemon.vpn.VpnState

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private var lastClip = ""

    private val vpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) startVpn()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        intent?.data?.toString()?.let { vm.addSubscription(it) }
        setContent {
            BlackDemonTheme {
                BlackDemonApp(vm, onToggle = ::toggle,
                    onShop = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Config.SHOP_URL))) })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.toString()?.let { vm.addSubscription(it) }
    }

    /** Авто-добавление: если в буфере обмена ссылка sub.layersync.online — подписка добавляется сама. */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) return
        val cm = getSystemService(ClipboardManager::class.java)
        val t = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()?.trim() ?: return
        if (t.startsWith(Config.SUB_HOST) && t != lastClip && t != vm.subUrl.value) {
            lastClip = t; vm.addSubscription(t)
        }
    }

    private fun toggle() {
        if (VpnState.status.value == Status.OFF) {
            val p = VpnService.prepare(this)
            if (p != null) vpnPermission.launch(p) else startVpn()
        } else {
            startService(Intent(this, BlackDemonVpnService::class.java).setAction(BlackDemonVpnService.ACTION_STOP))
        }
    }

    private fun startVpn() {
        val s = vm.current() ?: run { vm.message.value = "Сначала добавьте подписку и выберите сервер"; return }
        startService(Intent(this, BlackDemonVpnService::class.java).putExtra("link", s.link))
    }
}
