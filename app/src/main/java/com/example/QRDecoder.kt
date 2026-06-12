package com.example

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.json.JSONObject
import java.io.InputStream

object QRDecoder {

    /**
     * Decode a Bitmap containing a QR code.
     * Returns the decoded text string, or null if decoding fails.
     */
    fun decodeBitmap(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val source: LuminanceSource = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        return try {
            val reader = MultiFormatReader()
            val result = reader.decode(binaryBitmap)
            result.text
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Parses the decoded text string into a VpnServer object.
     * Supports:
     * - JSON: {"country":"Cuba Fast","city":"Port 500","ip":"1.2.3.4","isUdp":true}
     * - URI: flashvpn://add?country=Cuba&city=Port&ip=1.2.3.4&isUdp=true
     * - Simple text line: "Name|City|IP|UDP" (e.g. "MyServer|Port 12|1.2.3.4|true")
     */
    fun parseServerCode(text: String): VpnServer? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        try {
            // 1. Try to parse as JSON
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                val json = JSONObject(trimmed)
                val country = json.optString("country", "Servidor QR")
                val city = json.optString("city", "Puerto UDP")
                val ip = json.optString("ip", "")
                val isUdp = json.optBoolean("isUdp", true)
                if (ip.isNotEmpty()) {
                    return VpnServer(
                        id = 0, // id will be dynamic
                        country = country,
                        city = city,
                        flag = if (isUdp) "⚡" else "⚙️",
                        ping = (15..45).random(),
                        load = (2..15).random(),
                        ip = ip,
                        isUdp = isUdp
                    )
                }
            }

            // 2. WireGuard configuration file format
            if (trimmed.contains("[Interface]", ignoreCase = true) && trimmed.contains("Endpoint", ignoreCase = true)) {
                val lines = trimmed.split("\n")
                var host = ""
                var port = ""
                for (line in lines) {
                    val cleaned = line.trim()
                    if (cleaned.startsWith("Endpoint", ignoreCase = true)) {
                        val valueInLine = cleaned.substringAfter("=").trim()
                        if (valueInLine.contains(":")) {
                            host = valueInLine.substringBeforeLast(":").trim()
                            port = valueInLine.substringAfterLast(":").trim()
                        } else {
                            host = valueInLine
                        }
                    }
                }
                if (host.isNotEmpty()) {
                    return VpnServer(
                        id = 0,
                        country = "WireGuard UDP",
                        city = if (port.isNotEmpty()) "Puerto $port" else "Filtro UDP",
                        flag = "⚡",
                        ping = (16..42).random(),
                        load = (2..12).random(),
                        ip = host,
                        isUdp = true
                    )
                }
            }

            // 3. Try format flashvpn:// URL
            if (trimmed.startsWith("flashvpn://", ignoreCase = true)) {
                val uri = Uri.parse(trimmed)
                val country = uri.getQueryParameter("country") ?: "Servidor QR"
                val city = uri.getQueryParameter("city") ?: "Puerto Especial"
                val ip = uri.getQueryParameter("ip")
                val isUdpStr = uri.getQueryParameter("isUdp") ?: "true"
                val isUdp = isUdpStr.equals("true", ignoreCase = true)
                if (!ip.isNullOrEmpty()) {
                    return VpnServer(
                        id = 0,
                        country = country,
                        city = city,
                        flag = if (isUdp) "⚡" else "⚙️",
                        ping = (15..45).random(),
                        load = (2..15).random(),
                        ip = ip,
                        isUdp = isUdp
                    )
                }
            }

            // 4. Try format ss:// shadowsocks URL (UDP forwarding)
            if (trimmed.startsWith("ss://", ignoreCase = true)) {
                val withoutPrefix = trimmed.removePrefix("ss://")
                val hashtagIndex = withoutPrefix.indexOf("#")
                val cleanUrl = if (hashtagIndex != -1) withoutPrefix.substring(0, hashtagIndex) else withoutPrefix
                val serverName = if (hashtagIndex != -1) {
                    Uri.decode(withoutPrefix.substring(hashtagIndex + 1))
                } else {
                    "Proxy Shadowsocks"
                }

                if (cleanUrl.contains("@")) {
                    val parts = cleanUrl.split("@")
                    val hostPort = parts[1]
                    val host = if (hostPort.contains(":")) hostPort.substringBeforeLast(":") else hostPort
                    val port = if (hostPort.contains(":")) hostPort.substringAfterLast(":") else "8388"
                    return VpnServer(
                        id = 0,
                        country = serverName,
                        city = "SS Puerto $port",
                        flag = "⚡",
                        ping = (14..36).random(),
                        load = (4..15).random(),
                        ip = host,
                        isUdp = true
                    )
                } else {
                    try {
                        val decodedBytes = android.util.Base64.decode(cleanUrl, android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE)
                        val decodedStr = String(decodedBytes)
                        if (decodedStr.contains("@")) {
                            val hostPort = decodedStr.substringAfter("@")
                            val host = if (hostPort.contains(":")) hostPort.substringBeforeLast(":") else hostPort
                            val port = if (hostPort.contains(":")) hostPort.substringAfterLast(":") else "8388"
                            return VpnServer(
                                id = 0,
                                country = serverName,
                                city = "SS Puerto $port",
                                flag = "⚡",
                                ping = (14..36).random(),
                                load = (3..12).random(),
                                ip = host,
                                isUdp = true
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // 5. Try format vmess://, vless://, or trojan:// URLs
            if (trimmed.startsWith("vmess://", ignoreCase = true) || 
                trimmed.startsWith("vless://", ignoreCase = true) || 
                trimmed.startsWith("trojan://", ignoreCase = true)) {
                
                val uri = Uri.parse(trimmed)
                val scheme = uri.scheme ?: "vless"
                val host = uri.host ?: ""
                val port = if (uri.port != -1) uri.port.toString() else "443"
                var label = uri.fragment ?: "V2Ray UDP"
                if (label.isEmpty()) {
                    label = "$scheme Server"
                } else {
                    label = Uri.decode(label)
                }

                if (host.isNotEmpty()) {
                    return VpnServer(
                        id = 0,
                        country = label,
                        city = "Puerto $port",
                        flag = "⚡",
                        ping = (15..38).random(),
                        load = (2..10).random(),
                        ip = host,
                        isUdp = true
                    )
                }
            }

            // 6. Try format udp:// URL
            if (trimmed.startsWith("udp://", ignoreCase = true)) {
                val cleanUrl = trimmed.removePrefix("udp://")
                val host = if (cleanUrl.contains(":")) cleanUrl.substringBeforeLast(":") else cleanUrl
                val port = if (cleanUrl.contains(":")) cleanUrl.substringAfterLast(":") else "995"
                return VpnServer(
                    id = 0,
                    country = "Servidor UDP",
                    city = "Puerto $port",
                    flag = "⚡",
                    ping = (12..33).random(),
                    load = (3..11).random(),
                    ip = host,
                    isUdp = true
                )
            }

            // 7. Try format "country|city|ip|isUdp"
            val parts = trimmed.split("|")
            if (parts.size >= 3) {
                val country = parts[0]
                val city = parts[1]
                val ip = parts[2]
                val isUdp = if (parts.size >= 4) parts[3].equals("true", ignoreCase = true) else true
                return VpnServer(
                    id = 0,
                    country = country,
                    city = city,
                    flag = if (isUdp) "⚡" else "⚙️",
                    ping = (15..45).random(),
                    load = (2..15).random(),
                    ip = ip,
                    isUdp = isUdp
                )
            } else if (trimmed.matches(Regex("""^[0-9a-zA-Z.-]+$"""))) {
                // If it is just an IP or Domain, import it quickly
                return VpnServer(
                    id = 0,
                    country = "Importado IP",
                    city = "Filtro UDP",
                    flag = "⚡",
                    ping = 22,
                    load = 5,
                    ip = trimmed,
                    isUdp = true
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
