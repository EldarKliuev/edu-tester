package com.example.edu_tester.data.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.edu_tester.EduTesterApp
import com.example.edu_tester.MainActivity
import com.example.edu_tester.R
import com.example.edu_tester.data.session.ActiveSessionState
import com.example.edu_tester.domain.model.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import java.io.IOException

class TestSessionService : Service() {

    private val binder = LocalBinder()
    private var httpServer: LocalTestHttpServer? = null

    private val _runtime = MutableStateFlow<SessionRuntime?>(null)
    val runtime: StateFlow<SessionRuntime?> = _runtime.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    inner class LocalBinder : Binder() {
        fun getService(): TestSessionService = this@TestSessionService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureNotificationChannel()
        when (intent?.action) {
            ACTION_STOP -> {
                stopSessionInternal()
                stopSelf()
            }
            else -> promoteToForeground(
                _runtime.value?.active?.baseUrl
                    ?: getString(R.string.session_notification_text)
            )
        }
        return START_STICKY
    }

    fun startSession(test: Test, port: Int = DEFAULT_PORT): Result<ActiveSessionState> {
        if (httpServer != null) {
            stopSessionInternal(keepServiceAlive = true)
        }
        val app = application as EduTesterApp
        val container = app.container
        val ip = container.lanIpHelper.resolveLanIp()
            ?: return Result.failure(
                IllegalStateException(
                    "Нет Wi‑Fi / локального IP. Подключитесь к одной сети со студентами."
                )
            )

        return try {
            val baseUrl = "http://$ip:$port/"
            promoteToForeground(baseUrl)

            val server = LocalTestHttpServer(
                port = port,
                assets = assets,
                sessionManager = container.activeSessionManager,
                gson = container.gson,
            )
            server.start(SOCKET_READ_TIMEOUT, false)
            httpServer = server

            val active = container.activeSessionManager.start(test, port, baseUrl)
            runBlocking(Dispatchers.IO) {
                container.sessionRepository.saveSession(active.session)
            }
            _runtime.value = SessionRuntime(active = active, port = port)
            _error.value = null
            Result.success(active)
        } catch (e: IOException) {
            cleanupAfterFailure()
            val message = "Не удалось открыть порт $port: ${e.message}"
            _error.value = message
            Result.failure(IllegalStateException(message, e))
        } catch (e: Exception) {
            cleanupAfterFailure()
            _error.value = e.message
            Result.failure(e)
        }
    }

    fun stopSession() {
        stopSessionInternal()
        stopSelf()
    }

    private fun stopSessionInternal(keepServiceAlive: Boolean = false) {
        val container = (application as EduTesterApp).container
        val sessionId = container.activeSessionManager.current()?.session?.id
        httpServer?.stop()
        httpServer = null
        container.activeSessionManager.stop()
        if (sessionId != null) {
            runBlocking(Dispatchers.IO) {
                container.sessionRepository.endSession(sessionId, System.currentTimeMillis())
            }
        }
        _runtime.value = null
        if (!keepServiceAlive) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    private fun cleanupAfterFailure() {
        val container = (application as EduTesterApp).container
        val sessionId = container.activeSessionManager.current()?.session?.id
        httpServer?.stop()
        httpServer = null
        container.activeSessionManager.stop()
        if (sessionId != null) {
            runBlocking(Dispatchers.IO) {
                container.sessionRepository.endSession(sessionId, System.currentTimeMillis())
            }
        }
        _runtime.value = null
    }

    override fun onDestroy() {
        stopSessionInternal()
        super.onDestroy()
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.session_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    private fun promoteToForeground(detail: String) {
        val notification = buildNotification(detail)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    private fun buildNotification(detail: String): Notification {
        val pending = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.session_notification_title))
            .setContentText(getString(R.string.session_notification_text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }

    data class SessionRuntime(
        val active: ActiveSessionState,
        val port: Int,
    )

    companion object {
        const val DEFAULT_PORT = 8080
        private const val CHANNEL_ID = "test_session"
        private const val NOTIFICATION_ID = 42
        const val ACTION_STOP = "com.example.edu_tester.STOP_SESSION"
        private const val SOCKET_READ_TIMEOUT = 5_000

        fun startIntent(context: Context): Intent =
            Intent(context, TestSessionService::class.java)
    }
}

/**
 * Helper that binds/starts the service from UI layer.
 */
class SessionController(private val context: Context) {
    private var service: TestSessionService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as TestSessionService.LocalBinder).getService()
            bound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            bound = false
        }
    }

    fun bind() {
        if (!bound) {
            context.bindService(
                TestSessionService.startIntent(context),
                connection,
                Context.BIND_AUTO_CREATE,
            )
        }
    }

    fun unbind() {
        if (bound) {
            runCatching { context.unbindService(connection) }
            bound = false
            service = null
        }
    }

    suspend fun awaitService(timeoutMs: Long = 3_000): TestSessionService? {
        if (service != null) return service
        bind()
        val start = System.currentTimeMillis()
        while (service == null && System.currentTimeMillis() - start < timeoutMs) {
            delay(50)
        }
        return service
    }

    fun startForegroundShell() {
        val intent = TestSessionService.startIntent(context)
        context.startForegroundService(intent)
    }
}
