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
