package com.aistudio.tdbassistant.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import com.aistudio.tdbassistant.MainActivity
import com.aistudio.tdbassistant.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

class WakeWordForegroundService : Service() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private val sessionActive = AtomicBoolean(false)
    private var languageTag: String = "bn-BD"
    private var shuttingDown = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelfCleanly()
            return START_NOT_STICKY
        }
        languageTag = intent?.getStringExtra(EXTRA_LANGUAGE) ?: "bn-BD"
        startForeground(NOTIFICATION_ID, buildNotification())
        _isRunning.value = true
        scheduleListenCycle(delayMs = 150L)
        return START_STICKY
    }

    override fun onDestroy() {
        stopSelfCleanly()
        super.onDestroy()
    }

    private fun stopSelfCleanly() {
        shuttingDown = true
        mainHandler.removeCallbacksAndMessages(null)
        destroyRecognizer()
        _isRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun scheduleListenCycle(delayMs: Long) {
        if (shuttingDown) return
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({ startSingleListenSession() }, delayMs)
    }

    private fun startSingleListenSession() {
        if (shuttingDown || !sessionActive.compareAndSet(false, true)) return
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            sessionActive.set(false)
            _wakeEvents.tryEmit(WakeEvent.Unavailable("Speech recognition service unavailable on this device."))
            stopSelfCleanly()
            return
        }

        destroyRecognizer()
        val sr = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer = sr
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                sessionActive.set(false)
                if (!shuttingDown) {
                    val backoff = if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 1200L else 600L
                    scheduleListenCycle(backoff)
                }
            }

            override fun onResults(results: Bundle?) {
                sessionActive.set(false)
                val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (heard.isNotEmpty()) {
                    _wakeEvents.tryEmit(WakeEvent.Transcript(heard))
                }
                if (!shuttingDown) {
                    scheduleListenCycle(700L)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val recIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        runCatching { sr.startListening(recIntent) }.onFailure {
            sessionActive.set(false)
            scheduleListenCycle(1200L)
        }
    }

    private fun destroyRecognizer() {
        recognizer?.let {
            runCatching { it.stopListening() }
            runCatching { it.destroy() }
        }
        recognizer = null
        sessionActive.set(false)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TDB Voice Wake Listener",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active only when user enables Always-Available Hey TDB voice listening."
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            10,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this,
            11,
            Intent(this, WakeWordForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TDB Voice Assistant Active")
            .setContentText("Listening for \"Hey TDB\" or \"হ্যালো টিডিবি\". Tap Stop anytime.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .build()
    }

    sealed interface WakeEvent {
        data class Transcript(val text: String) : WakeEvent
        data class Unavailable(val reason: String) : WakeEvent
    }

    companion object {
        private const val CHANNEL_ID = "tdb_wake_voice_channel"
        private const val NOTIFICATION_ID = 3001
        const val ACTION_STOP = "com.aistudio.tdbassistant.action.STOP_WAKE"
        const val EXTRA_LANGUAGE = "extra_language"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _wakeEvents = MutableSharedFlow<WakeEvent>(extraBufferCapacity = 8)
        val wakeEvents: SharedFlow<WakeEvent> = _wakeEvents.asSharedFlow()

        fun start(context: Context, languageTag: String) {
            val intent = Intent(context, WakeWordForegroundService::class.java).apply {
                putExtra(EXTRA_LANGUAGE, languageTag)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WakeWordForegroundService::class.java))
        }
    }
}
