package com.example.maya

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class MayaAccessibilityService : AccessibilityService() {

    companion object {
        var instance: MayaAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        // Handle global wake word "Maya" detection results
        // When "Maya" is heard, this service can overlay UI or perform actions
    }

    fun clickElementByText(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val nodes = rootNode.findAccessibilityNodeInfosByText(text)
        if (nodes.isNullOrEmpty()) return false
        
        for (node in nodes) {
            if (node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
        }
        return false
    }

    fun scroll(forward: Boolean): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        return rootNode.performAction(if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
    }

    fun inputText(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        val bundle = android.os.Bundle()
        bundle.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        return focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
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

    fun triggerGlobalAction(action: Int): Boolean {
        return performGlobalAction(action)
    }
}
