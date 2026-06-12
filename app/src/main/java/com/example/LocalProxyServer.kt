package com.example

import android.util.Log
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class LocalProxyServer(private val port: Int = 8282) {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private var proxyThread: Thread? = null

    @Synchronized
    fun start() {
        if (isRunning) return
        isRunning = true
        proxyThread = thread(start = true, name = "FlashVpnProxy") {
            try {
                serverSocket = ServerSocket(port)
                Log.d("FlashProxyServer", "Local Proxy Server started on port $port")
                while (isRunning) {
                    val clientSocket = serverSocket?.accept() ?: break
                    thread {
                        handleClient(clientSocket)
                    }
                }
            } catch (e: Exception) {
                Log.e("FlashProxyServer", "Server exception: ${e.message}")
            } finally {
                isRunning = false
            }
        }
    }

    @Synchronized
    fun stop() {
        if (!isRunning) return
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e("FlashProxyServer", "Error closing ServerSocket: ${e.message}")
        }
        serverSocket = null
        proxyThread = null
        Log.d("FlashProxyServer", "Local Proxy Server stopped")
    }

    private fun handleClient(clientSocket: Socket) {
        try {
            clientSocket.soTimeout = 15000 // 15 seconds read timeout
            val input = clientSocket.getInputStream()
            val output = clientSocket.getOutputStream()

            // Read the first line of the HTTP request
            val firstLine = readLine(input)
            if (firstLine == null || firstLine.trim().isEmpty()) {
                clientSocket.close()
                return
            }

            val parts = firstLine.split(" ")
            if (parts.size < 2) {
                clientSocket.close()
                return
            }

            val method = parts[0]
            val urlString = parts[1]

            if (method.equals("CONNECT", ignoreCase = true)) {
                // HTTPS Proxy: CONNECT google.com:443 HTTP/1.1
                val hostAndPort = urlString.split(":")
                val host = hostAndPort[0]
                val targetPort = if (hostAndPort.size > 1) hostAndPort[1].toInt() else 443

                try {
                    val targetSocket = Socket(host, targetPort)
                    targetSocket.soTimeout = 30000 // 30 seconds target timeout

                    // Send success response
                    output.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())
                    output.flush()

                    // Forward bidirectional streams
                    val clientToTarget = thread {
                        transferData(input, targetSocket.getOutputStream())
                    }
                    val targetToClient = thread {
                        transferData(targetSocket.getInputStream(), output)
                    }
                    clientToTarget.join()
                    targetToClient.join()
                    try { targetSocket.close() } catch (ignored: Exception) {}
                } catch (e: Exception) {
                    try {
                        output.write("HTTP/1.1 502 Bad Gateway\r\nProxy-Error: ${e.localizedMessage}\r\n\r\n".toByteArray())
                        output.flush()
                    } catch (ignored: Exception) {}
                }
            } else {
                // HTTP Proxy: GET http://example.com/index.html HTTP/1.1
                var url = urlString
                if (url.startsWith("http://", ignoreCase = true)) {
                    url = url.substring(7)
                }
                val slashIdx = url.indexOf('/')
                val hostAndPort = (if (slashIdx >= 0) url.substring(0, slashIdx) else url).split(":")
                val host = hostAndPort[0]
                val targetPort = if (hostAndPort.size > 1) hostAndPort[1].toInt() else 80

                try {
                    val targetSocket = Socket(host, targetPort)
                    targetSocket.soTimeout = 30000
                    val targetOutput = targetSocket.getOutputStream()

                    // Rewrite first request line to use only the relative path/URI
                    val relativeUrl = if (slashIdx >= 0) url.substring(slashIdx) else "/"
                    val newFirstLine = "$method $relativeUrl ${if (parts.size > 2) parts[2] else "HTTP/1.1"}\r\n"
                    targetOutput.write(newFirstLine.toByteArray())

                    // Start bidirectional transfer
                    val clientToTarget = thread {
                        transferData(input, targetOutput)
                    }
                    val targetToClient = thread {
                        transferData(targetSocket.getInputStream(), output)
                    }
                    clientToTarget.join()
                    targetToClient.join()
                    try { targetSocket.close() } catch (ignored: Exception) {}
                } catch (e: Exception) {
                    try {
                        output.write("HTTP/1.1 502 Bad Gateway\r\nProxy-Error: ${e.localizedMessage}\r\n\r\n".toByteArray())
                        output.flush()
                    } catch (ignored: Exception) {}
                }
            }
        } catch (e: Exception) {
            // Ignore socket level telemetry errors
        } finally {
            try { clientSocket.close() } catch (ignored: Exception) {}
        }
    }

    private fun transferData(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(16384)
        try {
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                output.flush()
            }
        } catch (e: Exception) {
            // Connection closed elegantly
        }
    }

    private fun readLine(input: InputStream): String? {
        val bos = java.io.ByteArrayOutputStream()
        try {
            while (true) {
                val c = input.read()
                if (c == -1) break
                if (c == '\n'.code) break
                if (c == '\r'.code) continue
                bos.write(c)
            }
        } catch (e: Exception) {
            return null
        }
        return if (bos.size() == 0) null else bos.toString("UTF-8")
    }
}
