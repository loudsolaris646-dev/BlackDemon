package online.layersync.blackdemon.vpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray

enum class Status { OFF, CONNECTING, ON }
object VpnState { val status = MutableStateFlow(Status.OFF) }

class BlackDemonVpnService : VpnService() {
    companion object { const val ACTION_STOP = "online.layersync.blackdemon.STOP" }

    private var iface: ParcelFileDescriptor? = null
    private var core: CoreController? = null

    private val handler = object : CoreCallbackHandler {
        override fun startup(): Long = 0
        override fun shutdown(): Long { stopVpn(); return 0 }
        override fun onEmitStatus(l: Long, s: String?): Long = 0
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopVpn(); return START_NOT_STICKY }
        val link = intent?.getStringExtra("link") ?: return START_NOT_STICKY
        startVpn(link)
        return START_NOT_STICKY
    }

    private fun startVpn(link: String) {
        VpnState.status.value = Status.CONNECTING
        try {
            stopCore()
            val b = Builder().setSession("BlackDemon").setMtu(1500)
                .addAddress("10.7.0.1", 30).addRoute("0.0.0.0", 0)
                .addAddress("fd00:7::1", 126).addRoute("::", 0)
                .addDnsServer("1.1.1.1").addDnsServer("8.8.8.8")
            b.addDisallowedApplication(packageName)          // трафик самого ядра идёт мимо туннеля
            iface = b.establish() ?: error("VPN permission denied")
            Libv2ray.initCoreEnv(filesDir.absolutePath, "")
            core = Libv2ray.newCoreController(handler).also { it.startLoop(XrayConfig.build(link), iface!!.fd) }
            VpnState.status.value = Status.ON
        } catch (e: Throwable) {
            Log.e("BlackDemon", "start failed", e)
            stopVpn()
        }
    }

    private fun stopCore() { try { core?.stopLoop() } catch (_: Throwable) {}; core = null }

    private fun stopVpn() {
        stopCore()
        try { iface?.close() } catch (_: Throwable) {}
        iface = null
        VpnState.status.value = Status.OFF
        stopSelf()
    }

    override fun onRevoke() { stopVpn() }
    override fun onDestroy() { stopVpn(); super.onDestroy() }
}
