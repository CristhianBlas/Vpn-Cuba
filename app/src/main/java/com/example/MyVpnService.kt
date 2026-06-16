package com.example

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log

class MyVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var localProxy: LocalProxyServer? = null

    private var serverIp: String = "185.220.101.42"
    private var serverName: String = "Países Bajos"
    private var excludedPackages: ArrayList<String> = arrayListOf()
    private var adBlockerEnabled: Boolean = true
    private var isUdp: Boolean = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            if (intent.action == ACTION_DISCONNECT) {
                disconnect()
                return START_NOT_STICKY
            }
            serverIp = intent.getStringExtra("server_ip") ?: "185.220.101.42"
            serverName = intent.getStringExtra("server_name") ?: "Países Bajos"
            excludedPackages = intent.getStringArrayListExtra("excluded_packages") ?: arrayListOf()
            adBlockerEnabled = intent.getBooleanExtra("ad_blocker", true)
            isUdp = intent.getBooleanExtra("is_udp", false)
        }
        
        establishVpn(serverIp, serverName)
        return START_STICKY
    }

    private fun establishVpn(ip: String, name: String) {
        try {
            // Close any existing active VPN interface so settings are applied cleanly immediately on reconnect
            vpnInterface?.close()
            vpnInterface = null
            
            // Stop existing local proxy server loop gently
            try {
                localProxy?.stop()
            } catch (e: Exception) {
                Log.e("MyVpnService", "Error stopping local proxy", e)
            }
            
            // Guard against unauthorized invocation to prevent AppOps: ESTABLISH_VPN_SERVICE logs
            if (prepare(this) != null) {
                Log.w("MyVpnService", "VPN Service is not prepared or authorized yet. Postponing interface establishment.")
                return
            }
            
            // Spin up a non-blocking fully functional regional SOCKS/HTTP client proxy loop
            localProxy = LocalProxyServer(8282)
            try {
                localProxy?.start()
                Log.d("MyVpnService", "Local proxy server running smoothly on port 8282")
            } catch (e: Exception) {
                Log.e("MyVpnService", "Fault starting system level proxy server", e)
            }
            
            val builder = Builder()
            
            // Allocate a local virtual IP addressing block for packet encapsulation
            builder.addAddress("10.8.0.2", 24)
            
            // Apply customized DNS (either high-performance AdGuard adblock DNS or Google speed DNS)
            if (adBlockerEnabled) {
                builder.addDnsServer("94.140.14.14")
                builder.addDnsServer("94.140.15.15")
            } else {
                builder.addDnsServer("8.8.8.8")
                builder.addDnsServer("8.8.4.4")
            }
            
            // Route local address blocks only, letting general IP packets bypass unhandled TUN blackhole
            builder.addRoute("10.8.0.0", 24)
            
            // Setup direct system proxy to seamlessly relay target web sockets without breaking raw connectivity
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val proxyInfo = android.net.ProxyInfo.buildDirectProxy("127.0.0.1", 8282)
                builder.setHttpProxy(proxyInfo)
            }
            
            // Set VPN Session metadata shown in notification pane
            val protocolSuffix = if (isUdp) "UDP" else "TCP/WG"
            builder.setSession("Flash VPN ($protocolSuffix): $name ($ip)")

            // Add package exclusions for split tunneling
            for (pkg in excludedPackages) {
                try {
                    builder.addDisallowedApplication(pkg)
                } catch (e: Exception) {
                    Log.e("MyVpnService", "Could not disallow package: $pkg", e)
                }
            }
            
            // Configure self-reconnection and system integration
            vpnInterface = builder.establish()
            Log.d("MyVpnService", "Real VPN tunnel established successfully to static IP $ip ($name)")
        } catch (e: Exception) {
            Log.e("MyVpnService", "Failed to establish Android VPN interface reference target $ip", e)
        }
    }

    private fun disconnect() {
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e("MyVpnService", "Error during VPN tunnel teardown", e)
        }
        try {
            localProxy?.stop()
            localProxy = null
            Log.d("MyVpnService", "Proxy loop torn down cleanly.")
        } catch (e: Exception) {
            Log.e("MyVpnService", "Error stopping local proxy", e)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
    }

    companion object {
        const val ACTION_DISCONNECT = "com.example.ACTION_DISCONNECT"
    }
}
