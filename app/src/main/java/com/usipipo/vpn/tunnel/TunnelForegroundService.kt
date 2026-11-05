package com.usipipo.vpn.tunnel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.usipipo.vpn.AppContainer
import com.usipipo.vpn.MainActivity
import com.usipipo.vpn.R
import com.usipipo.vpn.UsipipoApp
import com.usipipo.vpn.diagnostics.AppLog
import com.usipipo.vpn.domain.TunnelState
import com.usipipo.vpn.domain.activeConfigId
import com.usipipo.vpn.domain.isActive
import com.usipipo.vpn.settings.PaletteVariant
import com.usipipo.vpn.settings.ThemeMode
import com.usipipo.vpn.settings.darkTheme
import com.usipipo.vpn.settings.tokens
import com.usipipo.vpn.ui.settings.bubble.TrafficBubbleText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Servicio en primer plano que hospeda el túnel WireGuard.
 *
 * Es el camino único de conexión en segundo plano (widget y mosaico): el sistema exige
 * entrar en primer plano antes de tocar el túnel, y de paso el usuario ve que la VPN
 * sigue viva. Por eso [startForeground] es lo primero que hace el servicio y de forma
 * síncrona: la plataforma da 5 s.
 *
 * El texto del `.conf` nunca viaja por un Intent: el servicio solo recibe el
 * identificador del perfil y lo lee del almacén cifrado dentro de [serviceScope].
 */
class TunnelForegroundService : Service() {

    private val serviceScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Job de la notificación de tráfico; se cancela en cuanto el túnel deja de ir bien. */
    private var trafficJob: Job? = null

    /** Ventana overlay de la burbuja; null cuando no está dibujada. */
    private var bubbleView: View? = null

    /** Textos de la burbuja: se guardan para no buscarlos por id en cada muestra. */
    private var bubbleSpeed: TextView? = null
    private var bubbleTime: TextView? = null
    private var bubbleCountry: TextView? = null

    /** Parámetros de la ventana: se guardan para poder arrastrar la burbuja. */
    private var bubbleParams: WindowManager.LayoutParams? = null

    private val container: AppContainer
        get() = (applicationContext as UsipipoApp).container

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        when (intent.action) {
            ACTION_CONNECT -> connect(intent)
            ACTION_DISCONNECT -> container.tunnelManager.disconnect()
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    /** No es un servicio vinculado: nadie lo enlaza, solo lo arranca. */
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        // El aviso de tráfico caduca solo, pero si el servicio cae por otra causa se
        // retira aquí para no dejarlo huérfano. La burbuja se va con el servicio.
        container.notificationsManager.cancelTraffic()
        hideBubble()
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * Entra en primer plano (síncrono) y luego, en la coroutine del servicio, lee el
     * perfil y llama al túnel: nada del túnel se toca antes de estar en primer plano.
     */
    private fun connect(intent: Intent) {
        val configId = intent.getStringExtra(EXTRA_CONFIG_ID)
        if (configId.isNullOrEmpty()) {
            AppLog.w(TAG, "Petición de conexión sin identificador de perfil")
            stopSelf()
            return
        }

        val profileName = container.configStore.configs.value
            .firstOrNull { it.id == configId }
            ?.name
            .orEmpty()

        startForegroundCompat(buildNotification(TunnelState.Connecting(configId), profileName))

        serviceScope.launch {
            val text = container.configTextReader.readForTunnel(configId)
            if (text == null) {
                AppLog.w(TAG, "Configuración no legible en el servicio (cfg ${configId.take(8)})")
            }
            container.tunnelManager.connect(configId, text.orEmpty())
            followTunnelState(profileName)
        }
    }

    /**
     * Mantiene la notificación al día y apaga el servicio cuando el túnel se cae.
     *
     * El primer «Desconectado» se ignora: el túnel arranca en otra coroutine y puede que
     * todavía no haya emitido su estado cuando el servicio empieza a mirar.
     */
    private suspend fun followTunnelState(profileName: String) {
        container.tunnelManager.state
            .dropWhile { it is TunnelState.Disconnected }
            .collect { state ->
                when (state) {
                    TunnelState.Disconnected -> {
                        stopTrafficNotice()
                        hideBubble()
                        stopHostingTunnel()
                    }
                    is TunnelState.Failed -> {
                        stopTrafficNotice()
                        hideBubble()
                        container.notificationsManager.publishDisconnect(state)
                        stopHostingTunnel()
                    }
                    is TunnelState.Connecting -> {
                        stopTrafficNotice()
                        refreshBubble(state)
                        updateNotification(state, profileName)
                    }
                    is TunnelState.Connected -> {
                        startTrafficNotice(profileName, state)
                        refreshBubble(state)
                        updateNotification(state, profileName)
                    }
                    is TunnelState.NoResponse -> {
                        // El túnel sigue en pie pero el servidor no responde: se avisa
                        // y se retira la notificación de tráfico, que ya no dice nada.
                        stopTrafficNotice()
                        container.notificationsManager.publishDisconnect(state)
                        refreshBubble(state)
                        updateNotification(state, profileName)
                    }
                }
            }
    }

    /**
     * Notificación de estado con tráfico: velocidad de subida y bajada, y tiempo de
     * conexión, con las muestras del contador del túnel. Solo corre mientras el túnel
     * está conectado; fuera de ahí no hay nada que contar. La misma muestra alimenta
     * la burbuja, que no tiene su propio contador.
     */
    private fun startTrafficNotice(profileName: String, state: TunnelState.Connected) {
        trafficJob?.cancel()
        trafficJob = serviceScope.launch {
            val calculator = SpeedCalculator()
            container.tunnelManager.counters().collect { counters ->
                val speed = calculator.add(counters)
                container.notificationsManager.publishTraffic(
                    profileName = profileName,
                    throughput = speed,
                    sinceMs = state.sinceMs,
                    nowMs = System.currentTimeMillis(),
                )
                updateBubbleContent(state, speed)
            }
        }
    }

    private fun stopTrafficNotice() {
        trafficJob?.cancel()
        trafficJob = null
        container.notificationsManager.cancelTraffic()
    }

    // --- Burbuja de tráfico (overlay sobre otras apps) ---

    /**
     * Muestra, actualiza o retira la burbuja según el estado del túnel.
     *
     * Se dibuja solo con el interruptor encendido, permiso de superposición
     * concedido y túnel activo; fuera de eso se retira sin molestar al usuario.
     */
    private fun refreshBubble(state: TunnelState) {
        val settings = container.appSettings
        val activo = state.isActive
        if (!activo || !settings.bubbleEnabled || !Settings.canDrawOverlays(this)) {
            hideBubble()
            return
        }
        if (bubbleView == null) showBubble()
        updateBubbleContent(state, null)
    }

    /** Añade la ventana overlay: no roba el foco ni los toques de las demás apps. */
    private fun showBubble() {
        if (bubbleView != null) return
        val manager = getSystemService(WindowManager::class.java) ?: return
        val view = buildBubbleView()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.START or Gravity.TOP
            x = savedPositionX()
            y = savedPositionY()
        }
        view.setOnTouchListener(bubbleTouchListener(params, manager))
        try {
            manager.addView(view, params)
        } catch (e: Exception) {
            // El sistema puede denegar la ventana (permiso retirado, pantalla de
            // sistema al frente): se registra y la app sigue funcionando.
            AppLog.w(TAG, "No se pudo dibujar la burbuja de tráfico", e)
            return
        }
        bubbleView = view
        bubbleParams = params
    }

    /** Retira la ventana overlay y suelta las referencias a sus vistas. */
    private fun hideBubble() {
        val view = bubbleView ?: return
        val manager = getSystemService(WindowManager::class.java)
        try {
            manager?.removeView(view)
        } catch (e: Exception) {
            AppLog.w(TAG, "No se pudo retirar la burbuja de tráfico", e)
        }
        bubbleView = null
        bubbleParams = null
    }

    /**
     * Rellena velocidad, tiempo de conexión y país. Sin datos muestra «—», así que
     * la burbuja nunca se ve vacía ni se cierra.
     */
    private fun updateBubbleContent(state: TunnelState, throughput: Throughput?) {
        if (bubbleView == null) return
        val configId = state.activeConfigId
        val config = configId?.let { id ->
            container.configStore.configs.value.firstOrNull { it.id == id }
        }
        val speed = TrafficBubbleText.speed(throughput?.downBps)
        val time = TrafficBubbleText.time(
            sinceMs = (state as? TunnelState.Connected)?.sinceMs,
            nowMs = System.currentTimeMillis(),
        )
        val country = TrafficBubbleText.country(config?.countryName, config?.countryCode)
        bubbleSpeed?.text = speed
        bubbleTime?.text = time
        bubbleCountry?.text = country
        // Un icono sin nombre es inaccesible para TalkBack: la burbuja se describe.
        bubbleView?.contentDescription = getString(
            R.string.bubble_content_description,
            speed,
            time,
            country,
        )
    }

    /**
     * Construye la tarjeta flotante con los colores de la paleta activa. Es una vista
     * de Android corriente: el servicio no puede alojar Compose.
     */
    private fun buildBubbleView(): View {
        val palette = PaletteVariant.fromKey(container.appSettings.palette)
            .tokens(isDark = isDarkTheme())
        // Sobre la superficie de la paleta, el texto legible se decide por luminancia:
        // los tokens no traen color de texto para la burbuja.
        val textColor = if (ColorUtils.calculateLuminance(palette.surface) > 0.5) {
            android.graphics.Color.BLACK
        } else {
            android.graphics.Color.WHITE
        }
        val density = resources.displayMetrics.density
        fun texto(pequeno: Boolean) = TextView(this).apply {
            setTextColor(textColor)
            textSize = if (pequeno) 10f else 12f
        }
        bubbleSpeed = texto(pequeno = false)
        bubbleTime = texto(pequeno = true)
        bubbleCountry = texto(pequeno = true)
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (12 * density).roundToInt()
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply {
                cornerRadius = 16 * density
                setColor(palette.surface)
                setStroke((density * 1f).roundToInt(), palette.outline)
            }
            addView(bubbleSpeed)
            addView(bubbleTime)
            addView(bubbleCountry)
        }
    }

    /**
     * Arrastre y toque de la burbuja. Al soltar se guarda la posición; si el dedo
     * apenas se movió, se abre la app en Detalles del perfil activo.
     */
    private fun bubbleTouchListener(
        params: WindowManager.LayoutParams,
        manager: WindowManager,
    ): View.OnTouchListener {
        var originX = 0
        var originY = 0
        var touchX = 0f
        var touchY = 0f
        var arrastrada = false
        return View.OnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    originX = params.x
                    originY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    arrastrada = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = originX + (event.rawX - touchX).toInt()
                    params.y = originY + (event.rawY - touchY).toInt()
                    manager.updateViewLayout(view, params)
                    arrastrada = true
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (arrastrada) {
                        container.appSettings.bubbleX = params.x.toFloat()
                        container.appSettings.bubbleY = params.y.toFloat()
                    } else {
                        openDetailsFromBubble()
                    }
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Tocar la burbuja abre la app con Detalles del perfil activo. Se lanza con un
     * [PendingIntent]: un servicio en segundo plano no puede llamar a
     * `startActivity` directamente.
     */
    private fun openDetailsFromBubble() {
        val intent = Intent(this, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_SHOW_DETAILS, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pending = PendingIntent.getActivity(
            this,
            REQUEST_BUBBLE_OPEN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            pending.send()
        } catch (e: PendingIntent.CanceledException) {
            AppLog.w(TAG, "No se pudo abrir Detalles desde la burbuja", e)
        }
    }

    /** Posición guardada; sin dato, la burbuja aparece arriba a la izquierda. */
    private fun savedPositionX(): Int = container.appSettings.bubbleX
        .takeIf { it >= 0f }?.toInt() ?: 0

    private fun savedPositionY(): Int = container.appSettings.bubbleY
        .takeIf { it >= 0f }?.toInt() ?: 0

    /** El tema que ve el usuario: modo elegido en Ajustes o el del sistema. */
    private fun isDarkTheme(): Boolean =
        ThemeMode.fromKey(container.appSettings.themeMode)
            .darkTheme(isSystemDark())
            .also { AppLog.d(TAG, "Tema oscuro para la burbuja: $it") }

    private fun isSystemDark(): Boolean =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    /** El túnel se apagó o falló: fuera de primer plano y sin servicio. */
    private fun stopHostingTunnel() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotification(state: TunnelState, profileName: String) {
        notificationManager()?.notify(NOTIFICATION_ID, buildNotification(state, profileName))
    }

    private fun buildNotification(state: TunnelState, profileName: String): Notification {
        val content = TunnelNotificationMapper.map(state)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield_check)
            .setContentTitle(getString(content.titleRes))
            .setContentText(getString(content.textRes))
            .setSubText(profileName)
            .setOngoing(content.ongoing)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentPendingIntent())
            .addAction(R.drawable.ic_power, getString(R.string.disconnect), disconnectPendingIntent())
            .build()
    }

    /** Al tocar la notificación se abre la app. */
    private fun contentPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            REQUEST_CONTENT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** La acción «Desconectar» va al propio servicio. */
    private fun disconnectPendingIntent(): PendingIntent {
        val intent = Intent(this, TunnelForegroundService::class.java).setAction(ACTION_DISCONNECT)
        return PendingIntent.getService(
            this,
            REQUEST_DISCONNECT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * El tipo `specialUse` solo existe desde la API 34; en las anteriores se entra en
     * primer plano sin tipo (el manifiesto ya declara `specialUse`).
     */
    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            setSound(null, null)
            enableVibration(false)
        }
        notificationManager()?.createNotificationChannel(channel)
    }

    /** El sistema siempre tiene gestor de notificaciones; el null es solo defensivo. */
    private fun notificationManager(): NotificationManager? =
        getSystemService(NotificationManager::class.java)

    companion object {
        const val ACTION_CONNECT: String = "com.usipipo.vpn.action.CONNECT"
        const val ACTION_DISCONNECT: String = "com.usipipo.vpn.action.DISCONNECT"
        const val EXTRA_CONFIG_ID: String = "com.usipipo.vpn.extra.CONFIG_ID"

        /**
         * Arranca el servicio en primer plano para conectar un perfil.
         *
         * Si el sistema lo deniega (app en segundo plano sin permiso), se traduce a
         * [BackgroundStartDeniedException] para que la app pueda reaccionar sin depender
         * de clases de Android. El mensaje no lleva ninguna carga útil del perfil.
         */
        fun start(context: Context, configId: String) {
            val intent = Intent(context, TunnelForegroundService::class.java).apply {
                action = ACTION_CONNECT
                putExtra(EXTRA_CONFIG_ID, configId)
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: IllegalStateException) {
                throw BackgroundStartDeniedException(
                    "El sistema denegó el arranque del servicio en primer plano",
                    e,
                )
            }
        }

        private const val CHANNEL_ID = "tunel"
        private const val NOTIFICATION_ID = 42
        private const val REQUEST_CONTENT = 0
        private const val REQUEST_DISCONNECT = 1
        private const val REQUEST_BUBBLE_OPEN = 3
        private const val TAG = "Servicio"
    }
}
