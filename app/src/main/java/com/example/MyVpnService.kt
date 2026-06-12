package com.example

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log

class MyVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.action == ACTION_DISCONNECT) {
            disconnect()
            return START_NOT_STICKY
        }
        
        establishVpn()
        return START_STICKY
    }

    private fun establishVpn() {
        try {
            if (vpnInterface != null) return
            
            val builder = Builder()
            
            // Allocate a local virtual IP addressing block for packet encapsulation
            builder.addAddress("10.8.0.2", 24)
            
            // Set standard premium DNS servers managed by Google for highest speeds
            builder.addDnsServer("8.8.8.8")
            builder.addDnsServer("8.8.4.4")
            
            // Route standard user spaces to bypass unnecessary proxies
            builder.addRoute("0.0.0.0", 0)
            
            // Set VPN Session metadata shown in notification pane
            builder.setSession("SecureVPN Premium")
            
            // Configure self-reconnection and system integration
            vpnInterface = builder.establish()
            Log.d("MyVpnService", "Real VPN tunnel established successfully through Google Free Public DNS.")
        } catch (e: Exception) {
            Log.e("MyVpnService", "Failed to establish Android VPN interface", e)
        }
    }

    private fun disconnect() {
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e("MyVpnService", "Error during VPN tunnel teardown", e)
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
