package surge.debug

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.WebtoonLayoutManager
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonFrame
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonRecyclerView

/**
 * Debug-only screen with the long strip scroller used by the reader (same frame, recycler view and
 * layout manager), filled with tall pages. WebtoonScrollTest swipes on it and records how far the
 * strip has scrolled on every frame.
 */
class ScrollTestActivity : Activity() {

    lateinit var recycler: WebtoonRecyclerView
        private set

    private var scrolled = 0L
    private val samples = ArrayList<DoubleArray>()

    @Volatile
    private var recording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screenHeight = resources.displayMetrics.heightPixels

        recycler = WebtoonRecyclerView(this).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            layoutManager = WebtoonLayoutManager(this@ScrollTestActivity, screenHeight * 2)
            itemAnimator = null
            adapter = PageAdapter(pageHeight = (screenHeight * 2.5).toInt())
            addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        scrolled += dy
                    }
                },
            )
            viewTreeObserver.addOnPreDrawListener {
                if (recording) samples += doubleArrayOf(System.nanoTime() / 1_000_000.0, scrolled.toDouble())
                true
            }
        }
        val frame = WebtoonFrame(this)
        frame.addView(recycler)
        setContentView(frame)
        moveToMiddle()
    }

    /** Must be called on the main thread. */
    fun moveToMiddle() {
        recycler.stopScroll()
        (recycler.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(PAGE_COUNT / 2, 0)
    }

    /** Must be called on the main thread. */
    fun startRecording() {
        samples.clear()
        scrolled = 0
        recording = true
    }

    /** Must be called on the main thread. Returns (time in ms, total scrolled px) per frame. */
    fun stopRecording(): List<DoubleArray> {
        recording = false
        return samples.toList()
    }

    private class PageAdapter(private val pageHeight: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount() = PAGE_COUNT

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = View(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, pageHeight)
            }
            return object : RecyclerView.ViewHolder(view) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val hue = (position * 37 % 360).toFloat()
            val top = android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.5f, 0.9f))
            val bottom = android.graphics.Color.HSVToColor(floatArrayOf((hue + 60) % 360, 0.6f, 0.6f))
            holder.itemView.background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(top, bottom))
        }
    }

    companion object {
        const val PAGE_COUNT = 400
    }
}
