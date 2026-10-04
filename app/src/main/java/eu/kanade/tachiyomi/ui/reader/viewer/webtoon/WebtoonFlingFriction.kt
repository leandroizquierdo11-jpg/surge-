@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

/**
 * Sets the friction of the scroller used by [RecyclerView.fling].
 *
 * This uses the same package name as the support library in order to reach its package
 * protected fling scroller, which has no public API.
 */
fun RecyclerView.setFlingFriction(friction: Float) {
    mViewFlinger.mOverScroller.setFriction(friction)
}

/**
 * Starts a fling even if [velocityY] is below RecyclerView's minimum fling velocity, which
 * [RecyclerView.fling] would ignore.
 */
fun RecyclerView.flingWithoutMinimum(velocityY: Int) {
    mViewFlinger.fling(0, velocityY)
}

/**
 * Sets how far a finger has to move before [RecyclerView] starts scrolling. RecyclerView only offers
 * two fixed values publicly, so this sets its private field. The field is kept by R8 in
 * proguard-rules.pro; if it can't be found the default is kept.
 */
fun RecyclerView.setTouchSlopCompat(touchSlop: Int) {
    runCatching {
        RecyclerView::class.java.getDeclaredField("mTouchSlop").apply { isAccessible = true }.setInt(this, touchSlop)
    }
}
