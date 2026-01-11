package com.example.maya

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class MayaAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        // Example: Capture text from the screen to "read" notification or content
        val rootNode = rootInActiveWindow
        if (rootNode != null) {
            val sb = StringBuilder()
            extractText(rootNode, sb)
            // Log.d("MayaAccess", "Screen content: ${sb.toString()}")
        }
    }

    private fun extractText(node: AccessibilityNodeInfo, sb: StringBuilder) {
        node.text?.let { sb.append(it).append(" ") }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                extractText(child, sb)
                child.recycle()
            }
        }
    }

    override fun onInterrupt() {
        Log.e("MayaAccess", "Service Interrupted")
    }

    fun performGlobalAction(action: Int): Boolean {
        return performGlobalAction(action)
    }
}
