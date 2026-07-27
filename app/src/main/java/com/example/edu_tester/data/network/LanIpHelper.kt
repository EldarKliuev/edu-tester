package com.example.edu_tester.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

class LanIpHelper(private val context: Context) {

    fun resolveLanIp(): String? {
        resolveFromConnectivityManager()?.let { return it }
        resolveFromWifiManager()?.let { return it }
        return resolveFromNetworkInterfaces()
    }

    private fun resolveFromConnectivityManager(): String? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return null
        val props: LinkProperties = cm.getLinkProperties(network) ?: return null
        return props.linkAddresses
            .mapNotNull { it.address }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress }
            ?.hostAddress
    }

    @Suppress("DEPRECATION")
    private fun resolveFromWifiManager(): String? {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return null
        val ip = wifi.connectionInfo?.ipAddress ?: return null
        if (ip == 0) return null
        return String.format(
            "%d.%d.%d.%d",
            ip and 0xff,
            ip shr 8 and 0xff,
            ip shr 16 and 0xff,
            ip shr 24 and 0xff,
        )
    }

    private fun resolveFromNetworkInterfaces(): String? {
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        for (intf in interfaces) {
            if (!intf.isUp || intf.isLoopback) continue
            val addresses = Collections.list(intf.inetAddresses)
            for (addr in addresses) {
                if (addr is Inet4Address && !addr.isLoopbackAddress) {
                    return addr.hostAddress
                }
            }
        }
        return null
    }
}
