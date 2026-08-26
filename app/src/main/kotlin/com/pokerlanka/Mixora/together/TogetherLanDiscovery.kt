/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.together

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.net.InetAddress
import java.net.NetworkInterface
import java.nio.charset.StandardCharsets

object TogetherLanDiscovery {
    private const val TAG = "TogetherLanDiscovery"
    private const val SERVICE_TYPE = "_mixora-tgt._tcp."
    private const val SERVICE_PREFIX = "Mixora_"

    private val mutex = Mutex()
    private var activeRegistrationListener: NsdManager.RegistrationListener? = null
    private var registeredContext: Context? = null

    private val httpClient by lazy {
        HttpClient(CIO) {
            engine {
                requestTimeout = 3000
            }
        }
    }

    suspend fun registerRoom(
        context: Context,
        code: String,
        port: Int,
        sessionId: String,
        sessionKey: String,
        displayName: String,
    ) = withContext(Dispatchers.IO) {
        unregisterRoom()
        val nsdManager = context.applicationContext.getSystemService(Context.NSD_SERVICE) as? NsdManager ?: return@withContext

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "$SERVICE_PREFIX$code"
            serviceType = SERVICE_TYPE
            this.port = port
            setAttribute("code", code)
            setAttribute("sid", sessionId)
            setAttribute("key", sessionKey)
            setAttribute("name", displayName)
        }

        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                Timber.tag(TAG).d("NSD Service registered: ${NsdServiceInfo.serviceName}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Timber.tag(TAG).e("NSD Service registration failed: error code $errorCode")
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                Timber.tag(TAG).d("NSD Service unregistered")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Timber.tag(TAG).e("NSD Service unregistration failed: error code $errorCode")
            }
        }

        mutex.withLock {
            activeRegistrationListener = listener
            registeredContext = context.applicationContext
            try {
                nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to register NSD service")
            }
        }
    }

    suspend fun unregisterRoom() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val listener = activeRegistrationListener ?: return@withLock
            val ctx = registeredContext ?: return@withLock
            val nsdManager = ctx.getSystemService(Context.NSD_SERVICE) as? NsdManager
            try {
                nsdManager?.unregisterService(listener)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to unregister NSD service")
            } finally {
                activeRegistrationListener = null
                registeredContext = null
            }
        }
    }

    suspend fun resolveCode(
        context: Context,
        rawCode: String,
        timeoutMs: Long = 6000L,
    ): TogetherJoinInfo? = withContext(Dispatchers.IO) {
        val targetCode = rawCode.replace("\\s+".toRegex(), "").trim()
        if (targetCode.length != 6 || !targetCode.all { it.isDigit() }) return@withContext null

        val resultFromNsd = withTimeoutOrNull(timeoutMs) {
            discoverViaNsd(context, targetCode)
        }
        if (resultFromNsd != null) return@withContext resultFromNsd

        // Fallback: local subnet probe on common default ports
        withTimeoutOrNull(2500L) {
            probeLocalSubnet(targetCode)
        }
    }

    private suspend fun discoverViaNsd(
        context: Context,
        targetCode: String,
    ): TogetherJoinInfo? = coroutineScope {
        val nsdManager = context.applicationContext.getSystemService(Context.NSD_SERVICE) as? NsdManager ?: return@coroutineScope null
        var foundInfo: TogetherJoinInfo? = null

        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Timber.tag(TAG).d("NSD discovery started: $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Timber.tag(TAG).d("NSD service found: ${service.serviceName}")
                if (foundInfo != null) return

                val name = service.serviceName ?: ""
                val matchesName = name.contains(targetCode)

                try {
                    nsdManager.resolveService(
                        service,
                        object : NsdManager.ResolveListener {
                            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                                Timber.tag(TAG).w("NSD resolve failed: $errorCode")
                            }

                            override fun onServiceResolved(resolved: NsdServiceInfo) {
                                val host = resolved.host?.hostAddress ?: return
                                val port = resolved.port
                                val attrCode = resolved.attributes["code"]?.let { String(it, StandardCharsets.UTF_8) }
                                val sid = resolved.attributes["sid"]?.let { String(it, StandardCharsets.UTF_8) }
                                val key = resolved.attributes["key"]?.let { String(it, StandardCharsets.UTF_8) }

                                if (attrCode == targetCode || matchesName) {
                                    if (!sid.isNullOrBlank() && !key.isNullOrBlank()) {
                                        foundInfo = TogetherJoinInfo(
                                            host = host,
                                            port = port,
                                            sessionId = sid,
                                            sessionKey = key,
                                        )
                                    }
                                }
                            }
                        },
                    )
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Error resolving service")
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
            while (foundInfo == null) {
                delay(100)
            }
            foundInfo
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error during NSD discovery")
            null
        } finally {
            runCatching { nsdManager.stopServiceDiscovery(discoveryListener) }
        }
    }

    private suspend fun probeLocalSubnet(targetCode: String): TogetherJoinInfo? = coroutineScope {
        val localIp = getLocalIpAddress() ?: return@coroutineScope null
        val subnetPrefix = localIp.substringBeforeLast('.') + "."

        val candidates = (1..254).map { "$subnetPrefix$it" }
        val portsToProbe = listOf(42117, 42118, 42119, 42120)

        val jobs = candidates.flatMap { ip ->
            portsToProbe.map { port ->
                async(Dispatchers.IO) {
                    try {
                        val responseText = httpClient.get("http://$ip:$port/together/info").bodyAsText()
                        val roomInfo = TogetherJson.decodeFromString<TogetherRoomInfo>(responseText)
                        if (roomInfo.code == targetCode) {
                            TogetherJoinInfo(
                                host = ip,
                                port = port,
                                sessionId = roomInfo.sessionId,
                                sessionKey = roomInfo.sessionKey,
                            )
                        } else null
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }

        jobs.awaitAll().firstOrNull { it != null }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = intf.inetAddresses ?: continue
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }
}
