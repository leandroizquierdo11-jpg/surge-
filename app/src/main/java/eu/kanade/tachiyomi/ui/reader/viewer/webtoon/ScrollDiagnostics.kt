package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.FrameMetrics
import android.view.Window
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.BuildConfig
import kotlin.math.hypot
import kotlin.math.max

/**
 * Optional diagnostics of the long strip reader, turned on in the advanced settings. While reading it
 * records how long every frame took (and what else happened around slow frames) and how each scroll
 * gesture behaved, so a report from the user's own device shows where scrolling isn't smooth.
 */
object ScrollDiagnostics {

    @Volatile
    var enabled = false

    @Volatile
    var scrollState = RecyclerView.SCROLL_STATE_IDLE

    /** Refresh rate of the display while reading. */
    @Volatile
    var refreshRate = 0f

    /** Refresh rates the display supports. */
    @Volatile
    var supportedRefreshRates: List<Float> = emptyList()

    enum class Event(val label: String) {
        BIND("pagina preparada"),
        IMAGE("imagen mostrada"),
        PAGE_CHANGE("cambio de pagina"),
    }

    private class Frame(
        val vsyncNanos: Long,
        val totalMs: Double,
        val deadlineMs: Double,
        val inputMs: Double,
        val animationMs: Double,
        val layoutMs: Double,
        val drawMs: Double,
        val syncMs: Double,
        val gpuMs: Double,
        val state: Int,
    ) {
        val slow get() = totalMs > deadlineMs
    }

    class Gesture(
        val fingerMm: Double,
        val durationMs: Long,
        val startDelayMs: Long,
        val startFingerMm: Double,
        val trackerSpeed: Double,
        val fingerSpeed: Double,
        val glideMm: Double,
        val glideMs: Long,
    )

    private val lock = Any()
    private val frames = ArrayDeque<Frame>()
    private val events = ArrayDeque<Pair<Long, Event>>()
    private val gestures = ArrayDeque<Gesture>()

    private val thread by lazy { HandlerThread("ScrollDiagnostics").apply { start() } }

    private val frameListener = Window.OnFrameMetricsAvailableListener { _, metrics, _ -> onFrame(metrics) }

    fun attach(window: Window) {
        window.addOnFrameMetricsAvailableListener(frameListener, Handler(thread.looper))
    }

    fun detach(window: Window) {
        runCatching { window.removeOnFrameMetricsAvailableListener(frameListener) }
    }

    fun onEvent(event: Event) {
        if (!enabled) return
        synchronized(lock) {
            events.addLast(System.nanoTime() to event)
            while (events.size > MAX_EVENTS) events.removeFirst()
        }
    }

    fun onGesture(gesture: Gesture) {
        synchronized(lock) {
            gestures.addLast(gesture)
            while (gestures.size > MAX_GESTURES) gestures.removeFirst()
        }
    }

    fun clear() {
        synchronized(lock) {
            frames.clear()
            events.clear()
            gestures.clear()
        }
    }

    private fun onFrame(metrics: FrameMetrics) {
        // Only frames while the strip is moving matter
        val state = scrollState
        if (state == RecyclerView.SCROLL_STATE_IDLE) return
        fun ms(id: Int) = metrics.getMetric(id) / 1_000_000.0
        val vsyncPeriodMs = if (refreshRate > 0) 1000.0 / refreshRate else 1000.0 / 60
        val frame = Frame(
            vsyncNanos = metrics.getMetric(FrameMetrics.INTENDED_VSYNC_TIMESTAMP),
            totalMs = ms(FrameMetrics.TOTAL_DURATION),
            deadlineMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ms(FrameMetrics.DEADLINE)
            } else {
                vsyncPeriodMs * 1.5
            },
            inputMs = ms(FrameMetrics.INPUT_HANDLING_DURATION),
            animationMs = ms(FrameMetrics.ANIMATION_DURATION),
            layoutMs = ms(FrameMetrics.LAYOUT_MEASURE_DURATION),
            drawMs = ms(FrameMetrics.DRAW_DURATION),
            syncMs = ms(FrameMetrics.SYNC_DURATION),
            gpuMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ms(FrameMetrics.GPU_DURATION) else 0.0,
            state = state,
        )
        synchronized(lock) {
            frames.addLast(frame)
            while (frames.size > MAX_FRAMES) frames.removeFirst()
        }
    }

    fun report(context: Context): String = synchronized(lock) {
        val metrics = context.resources.displayMetrics
        val inches = hypot(metrics.widthPixels / metrics.xdpi, metrics.heightPixels / metrics.ydpi)
        val out = StringBuilder()
        out.appendLine("Surge ${BuildConfig.VERSION_NAME} - diagnostico de scroll")
        out.appendLine(
            "Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL} | Android ${Build.VERSION.RELEASE} | " +
                "${metrics.widthPixels}x${metrics.heightPixels} | %.1f\" | densidad %d".format(inches, metrics.densityDpi),
        )
        out.appendLine(
            "Refresco al leer: %.0f Hz | soportados: %s".format(
                refreshRate,
                supportedRefreshRates.joinToString("/") { "%.0f".format(it) },
            ),
        )

        val slow = frames.filter { it.slow }
        val dragging = frames.count { it.state == RecyclerView.SCROLL_STATE_DRAGGING }
        val slowDragging = slow.count { it.state == RecyclerView.SCROLL_STATE_DRAGGING }
        out.appendLine(
            "Fotogramas en movimiento: ${frames.size} | lentos: ${slow.size} (%.1f%%) | arrastrando: %d de %d lentos | deslizando: %d de %d lentos".format(
                percent(slow.size, frames.size),
                slowDragging,
                dragging,
                slow.size - slowDragging,
                frames.size - dragging,
            ),
        )
        if (slow.isNotEmpty()) {
            fun avg(f: (Frame) -> Double) = slow.map(f).average()
            out.appendLine(
                "Media en lentos (ms): total %.1f | entrada %.1f | animacion %.1f | diseno %.1f | dibujo %.1f | sync %.1f | gpu %.1f".format(
                    avg { it.totalMs }, avg { it.inputMs }, avg { it.animationMs }, avg { it.layoutMs },
                    avg { it.drawMs }, avg { it.syncMs }, avg { it.gpuMs },
                ),
            )
            val worst = slow.maxBy { it.totalMs }
            out.appendLine(
                "Peor (ms): total %.1f | entrada %.1f | animacion %.1f | diseno %.1f | dibujo %.1f | sync %.1f | gpu %.1f".format(
                    worst.totalMs, worst.inputMs, worst.animationMs, worst.layoutMs, worst.drawMs, worst.syncMs, worst.gpuMs,
                ),
            )
            // What happened just before each slow frame
            val causes = Event.entries.associateWith { event ->
                slow.count { frame ->
                    events.any { (time, e) -> e == event && time in frame.vsyncNanos - CAUSE_WINDOW_NANOS..frame.vsyncNanos }
                }
            }
            val unexplained = slow.count { frame ->
                events.none { (time, _) -> time in frame.vsyncNanos - CAUSE_WINDOW_NANOS..frame.vsyncNanos }
            }
            out.appendLine(
                "Lentos justo despues de: " +
                    causes.entries.joinToString(" | ") { "${it.key.label} ${it.value}" } +
                    " | sin nada ${unexplained}",
            )
        }

        val (short, long) = gestures.partition { it.fingerMm < SHORT_GESTURE_MM }
        out.appendLine("Gestos: ${gestures.size} (cortos <3 cm: ${short.size}, largos: ${long.size})")
        for ((name, list) in listOf("Cortos" to short, "Largos" to long)) {
            if (list.isEmpty()) continue
            out.appendLine(
                "$name: dedo %.0f mm en %.0f ms | arranca a los %.0f ms y %.1f mm | velocidad al soltar Android %.0f mm/s, dedo %.0f mm/s | sigue %.0f mm en %.0f ms".format(
                    list.map { it.fingerMm }.average(),
                    list.map { it.durationMs.toDouble() }.average(),
                    list.map { it.startDelayMs.toDouble() }.average(),
                    list.map { it.startFingerMm }.average(),
                    list.map { it.trackerSpeed }.average(),
                    list.map { it.fingerSpeed }.average(),
                    list.map { it.glideMm }.average(),
                    list.map { it.glideMs.toDouble() }.average(),
                ),
            )
        }
        out.toString()
    }

    private fun percent(part: Int, total: Int) = if (total == 0) 0.0 else part * 100.0 / max(total, 1)

    private const val MAX_FRAMES = 20_000
    private const val MAX_EVENTS = 5_000
    private const val MAX_GESTURES = 1_000
    private const val CAUSE_WINDOW_NANOS = 100_000_000L
    private const val SHORT_GESTURE_MM = 30.0
}
