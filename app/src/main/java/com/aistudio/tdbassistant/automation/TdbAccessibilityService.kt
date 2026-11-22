package com.aistudio.tdbassistant.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

class TdbAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = WeakReference(this)
        _isConnected.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Deliberately passive: only inspects active window when user issues an explicit voice/tap command.
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        if (instanceRef?.get() === this) {
            instanceRef = null
        }
        _isConnected.value = false
    }

    fun performBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)

    fun performHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)

    fun performScroll(down: Boolean): Boolean {
        val dm = resources.displayMetrics
        val x = dm.widthPixels / 2f
        val startY = if (down) dm.heightPixels * 0.72f else dm.heightPixels * 0.30f
        val endY = if (down) dm.heightPixels * 0.28f else dm.heightPixels * 0.74f

        val path = Path().apply {
            moveTo(x, startY)
            lineTo(x, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 280L))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    fun tryClickByTextOrDescription(keywords: List<String>): Boolean {
        val root = rootInActiveWindow ?: return false
        return try {
            findAndClick(root, keywords)
        } finally {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                @Suppress("DEPRECATION")
                root.recycle()
            }
        }
    }

    fun tryOpenFirstYouTubeVideo(): Boolean {
        val root = rootInActiveWindow ?: return false
        val pkg = root.packageName?.toString().orEmpty()
        if (!pkg.contains("youtube", ignoreCase = true)) return false
        return findFirstClickableVideo(root)
    }

    private fun findAndClick(node: AccessibilityNodeInfo, keywords: List<String>): Boolean {
        val text = node.text?.toString()?.lowercase().orEmpty()
        val desc = node.contentDescription?.toString()?.lowercase().orEmpty()
        if (keywords.any { kw -> text.contains(kw) || desc.contains(kw) }) {
            var current: AccessibilityNodeInfo? = node
            while (current != null) {
                if (current.isClickable && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true
                }
                current = current.parent
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClick(child, keywords)) return true
        }
        return false
    }

    private fun findFirstClickableVideo(node: AccessibilityNodeInfo): Boolean {
        val viewId = node.viewIdResourceName.orEmpty()
        val desc = node.contentDescription?.toString().orEmpty()
        if (node.isClickable && (viewId.contains("thumbnail") || desc.length > 18)) {
            if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findFirstClickableVideo(child)) return true
        }
        return false
    }

    companion object {
        private var instanceRef: WeakReference<TdbAccessibilityService>? = null
        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        fun current(): TdbAccessibilityService? = instanceRef?.get()
    }
}
