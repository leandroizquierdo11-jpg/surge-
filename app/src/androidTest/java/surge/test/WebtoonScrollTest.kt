package surge.test

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import surge.debug.ScrollTestActivity
import kotlin.math.abs

/**
 * Swipes on the long strip scroller like a finger would (short and long gestures, through the real
 * input pipeline) and measures, frame by frame, how the strip follows:
 *
 * - arranque: how far the finger moved before the strip started moving.
 * - continuidad: strip speed right after lifting the finger / right before (1.0 = no sudden change).
 * - deslizamiento: how far and how long the strip keeps moving after lifting the finger.
 * - tirones: frames where the speed changes abruptly compared to the frames around them.
 *
 * Results are written to logcat with the tag [TAG], once with the short gesture smoothing off (as in
 * Android) and once on.
 */
@RunWith(AndroidJUnit4::class)
class WebtoonScrollTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    /**
     * @param endSpeed finger speed when it's lifted, relative to its top speed (1 = flick at full
     * speed, 0 = the finger stops before lifting).
     */
    private class Gesture(val name: String, val cm: Float, val durationMs: Long, val endSpeed: Float)

    private class Result(
        val lagMm: Double,
        val fps: Double,
        val startMm: Double,
        val continuity: Double,
        val glideMm: Double,
        val glideMs: Double,
        val jerks: Int,
    )

    @Test
    fun shortAndLongSwipes() {
        ActivityScenario.launch(ScrollTestActivity::class.java).use { scenario ->
            instrumentation.waitForIdleSync()
            var pxPerCm = 0f
            var width = 0
            var height = 0
            scenario.onActivity {
                val metrics = it.resources.displayMetrics
                pxPerCm = metrics.ydpi / 2.54f
                width = metrics.widthPixels
                height = metrics.heightPixels
            }
            log("Pantalla ${width}x$height, ${"%.1f".format(pxPerCm)} px/cm")

            val gestures = listOf(
                Gesture("corto rapido", 1.5f, 90, 1f),
                Gesture("corto", 2.5f, 140, 0.7f),
                Gesture("corto lento", 2.5f, 450, 0.25f),
                Gesture("medio", 5f, 180, 0.9f),
                Gesture("largo", 9.5f, 260, 1f),
            )

            for (smooth in listOf(false, true)) {
                scenario.onActivity { it.recycler.smoothShortGestures = smooth }
                log("=== Suavizado de gestos cortos: ${if (smooth) "SI" else "NO"} ===")
                for (gesture in gestures) {
                    val results = (1..REPEATS).map {
                        scenario.onActivity { activity -> activity.moveToMiddle() }
                        instrumentation.waitForIdleSync()
                        SystemClock.sleep(300)
                        measure(scenario, gesture, pxPerCm, width / 2f, height * 0.93f)
                    }
                    log(
                        "%-13s | arranque %.1f mm | retraso al soltar %.1f mm | continuidad %.2f | desliza %.1f mm en %.0f ms | tirones %.1f | %.0f fps".format(
                            gesture.name,
                            results.map { it.startMm }.average(),
                            results.map { it.lagMm }.average(),
                            results.map { it.continuity }.average(),
                            results.map { it.glideMm }.average(),
                            results.map { it.glideMs }.average(),
                            results.map { it.jerks }.average(),
                            results.map { it.fps }.average(),
                        ),
                    )
                }
            }
        }
    }

    private fun measure(
        scenario: ActivityScenario<ScrollTestActivity>,
        gesture: Gesture,
        pxPerCm: Float,
        x: Float,
        startY: Float,
    ): Result {
        var view: View? = null
        scenario.onActivity {
            view = it.window.decorView
            it.startRecording()
        }
        val finger = swipe(view!!, gesture, pxPerCm, x, startY)
        // Let the strip settle
        SystemClock.sleep(2000)
        var samples: List<DoubleArray> = emptyList()
        scenario.onActivity { samples = it.stopRecording() }

        val mm = 10.0 / pxPerCm
        val upTime = finger.last().first.toDouble()

        // How far the finger had moved when the strip first moved
        val firstMove = samples.firstOrNull { it[1] > 0.5 }
        val startMm = (firstMove?.let { fingerTravelAt(finger, it[0]) } ?: finger.last().second.toDouble()) * mm

        val before = samples.filter { it[0] <= upTime }
        val after = samples.filter { it[0] > upTime }
        val speedBefore = speed(before.takeLast(3))
        val speedAfter = speed(before.takeLast(1) + after.take(2))
        val continuity = if (speedBefore > 0) speedAfter / speedBefore else 0.0

        val atUp = before.lastOrNull()?.get(1) ?: 0.0
        val end = samples.lastOrNull()?.get(1) ?: 0.0
        val lastMove = samples.zipWithNext().lastOrNull { (a, b) -> b[1] - a[1] > 0.5 }?.second?.get(0)
        val glideMs = lastMove?.let { (it - upTime).coerceAtLeast(0.0) } ?: 0.0

        // Frame speeds; a jerk is a frame whose speed is far from the average of its neighbours
        val speeds = samples.zipWithNext().mapNotNull { (a, b) ->
            val dt = b[0] - a[0]
            if (dt > 0) (b[1] - a[1]) / dt else null
        }
        val top = speeds.maxOfOrNull { abs(it) } ?: 0.0
        val jerks = speeds.windowed(3).count { (a, b, c) ->
            top > 0 && abs(b - (a + c) / 2) > top * 0.25
        }

        val lagMm = (finger.last().second - atUp) * mm
        val frameMs = samples.zipWithNext().map { (a, b) -> b[0] - a[0] }.sorted().let { if (it.isEmpty()) 0.0 else it[it.size / 2] }
        val fps = if (frameMs > 0) 1000 / frameMs else 0.0
        return Result(lagMm, fps, startMm, continuity, (end - atUp) * mm, glideMs, jerks)
    }

    /** Average speed (px/ms) between the first and last sample. */
    private fun speed(samples: List<DoubleArray>): Double {
        if (samples.size < 2) return 0.0
        val dt = samples.last()[0] - samples.first()[0]
        return if (dt > 0) (samples.last()[1] - samples.first()[1]) / dt else 0.0
    }

    private fun fingerTravelAt(finger: List<Pair<Long, Float>>, time: Double): Double {
        val after = finger.indexOfFirst { it.first >= time }
        if (after <= 0) return if (after == 0) finger[0].second.toDouble() else finger.last().second.toDouble()
        val (t0, p0) = finger[after - 1]
        val (t1, p1) = finger[after]
        if (t1 == t0) return p1.toDouble()
        return p0 + (p1 - p0) * (time - t0) / (t1 - t0)
    }

    /**
     * Swipes up (scrolling the strip down). Returns (event time, finger travel in px).
     *
     * The events are handed to the window on the main thread at their exact times, like Android does
     * with touchscreen input, instead of going through the emulator's input system: on CI the
     * emulator is slow enough for system dialogs ("app not responding") to pop up and take the touches.
     */
    private fun swipe(view: View, gesture: Gesture, pxPerCm: Float, x: Float, startY: Float): List<Pair<Long, Float>> {
        val distance = gesture.cm * pxPerCm
        val downTime = SystemClock.uptimeMillis() + 100
        val finger = ArrayList<Pair<Long, Float>>()
        val handler = Handler(Looper.getMainLooper())

        fun post(action: Int, time: Long, travel: Float) {
            val event = MotionEvent.obtain(downTime, time, action, x, startY - travel, 0)
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            handler.postAtTime({
                view.dispatchTouchEvent(event)
                event.recycle()
            }, time)
            finger += time to travel
        }

        post(MotionEvent.ACTION_DOWN, downTime, 0f)
        var time = downTime
        while (true) {
            time += INPUT_INTERVAL_MS
            val progress = ((time - downTime).toFloat() / gesture.durationMs).coerceAtMost(1f)
            post(MotionEvent.ACTION_MOVE, time, distance * position(progress, gesture.endSpeed))
            if (progress >= 1f) break
        }
        // The finger lifts right after its last movement
        post(MotionEvent.ACTION_UP, time + 2, distance)
        SystemClock.sleep(time + 2 - SystemClock.uptimeMillis() + 50)
        return finger
    }

    /**
     * Finger position (0..1) at [progress] (0..1): it speeds up during the first 30% of the gesture,
     * then its speed changes linearly to [endSpeed] times the top speed.
     */
    private fun position(progress: Float, endSpeed: Float): Float {
        fun integral(s: Float): Float = if (s <= RAMP) {
            s * s / (2 * RAMP)
        } else {
            val t = s - RAMP
            RAMP / 2 + t - (1 - endSpeed) * t * t / (2 * (1 - RAMP))
        }
        return integral(progress) / integral(1f)
    }

    private fun log(message: String) {
        Log.i(TAG, message)
    }

    companion object {
        const val TAG = "SurgeScrollTest"
        private const val REPEATS = 3
        private const val INPUT_INTERVAL_MS = 8L
        private const val RAMP = 0.3f
    }
}
