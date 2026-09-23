package com.danielealbano.androidremotecontrolmcp.services.accessibility

import android.os.Bundle
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import javax.inject.Inject

@Suppress("ReturnCount", "NestedBlockDepth")
class TypeInputControllerImpl
    @Inject
    constructor(private val accessibilityServiceProvider: AccessibilityServiceProvider) : TypeInputController {
        @Volatile private var selectionStart = -1
        @Volatile private var selectionEnd = -1

        private fun focusedNode(): AccessibilityNodeInfo? = findFocusedEditableNodeForInput(accessibilityServiceProvider)

        override fun isReady(): Boolean {
            if (!accessibilityServiceProvider.isReady()) return false
            val node = focusedNode() ?: return false
            return try { node.isEditable } finally { recycle(node) }
        }

        override fun commitText(text: CharSequence, newCursorPosition: Int): Boolean {
            val node = focusedNode() ?: return false
            return try {
                val current = node.text?.toString().orEmpty()
                val start = selectionStart.coerceIn(0, current.length)
                val end = selectionEnd.coerceIn(start, current.length)
                val updated = current.substring(0, start) + text + current.substring(end)
                if (!setNodeText(node, updated)) return false
                val cursor = (start + text.length).coerceIn(0, updated.length)
                selectionStart = cursor; selectionEnd = cursor; true
            } finally { recycle(node) }
        }

        override fun setSelection(start: Int, end: Int): Boolean {
            val node = focusedNode() ?: return false
            return try {
                val length = node.text?.length ?: 0
                if (start < 0 || end < start || end > length) return false
                selectionStart = start; selectionEnd = end; true
            } finally { recycle(node) }
        }

        override fun getSurroundingText(beforeLength: Int, afterLength: Int, flags: Int): TextSnapshot? {
            if (!accessibilityServiceProvider.isReady()) return null
            val node = focusedNode() ?: return null
            return try {
                val full = node.text?.toString().orEmpty()
                val start = if (selectionStart >= 0) selectionStart.coerceIn(0, full.length) else full.length
                val end = if (selectionEnd >= 0) selectionEnd.coerceIn(start, full.length) else start
                val from = (start - beforeLength).coerceAtLeast(0)
                val to = (end + afterLength).coerceAtMost(full.length)
                TextSnapshot(full.substring(from, to), from, start - from, end - from)
            } finally { recycle(node) }
        }

        override fun performContextMenuAction(id: Int): Boolean {
            if (id != android.R.id.selectAll) return false
            val node = focusedNode() ?: return false
            return try { val length=node.text?.length ?: 0; selectionStart=0; selectionEnd=length; true } finally { recycle(node) }
        }

        override fun sendKeyEvent(event: KeyEvent): Boolean {
            if (event.keyCode != KeyEvent.KEYCODE_DEL || event.action != KeyEvent.ACTION_UP) return event.keyCode == KeyEvent.KEYCODE_DEL
            val node = focusedNode() ?: return false
            return try {
                val current=node.text?.toString().orEmpty(); val start=selectionStart.coerceIn(0,current.length); val end=selectionEnd.coerceIn(start,current.length)
                if (start==end) { if (start==0) return true; selectionStart=start-1; selectionEnd=start }
                val deleteStart=selectionStart.coerceIn(0,current.length); val deleteEnd=selectionEnd.coerceIn(deleteStart,current.length)
                if (!setNodeText(node,current.removeRange(deleteStart,deleteEnd))) return false
                selectionStart=deleteStart; selectionEnd=deleteStart; true
            } finally { recycle(node) }
        }

        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
            val node=focusedNode() ?: return false
            return try {
                val current=node.text?.toString().orEmpty(); val cursor=selectionStart.coerceIn(0,current.length)
                val from=(cursor-beforeLength).coerceAtLeast(0); val to=(cursor+afterLength).coerceAtMost(current.length)
                if (!setNodeText(node,current.removeRange(from,to))) return false
                selectionStart=from; selectionEnd=from; true
            } finally { recycle(node) }
        }

        private fun setNodeText(node: AccessibilityNodeInfo,text:String):Boolean { val args=Bundle(); args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text); return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args) }
        private fun recycle(node:AccessibilityNodeInfo){ @Suppress("DEPRECATION") node.recycle() }

        private fun findFocusedEditableNodeForInput(provider: AccessibilityServiceProvider): AccessibilityNodeInfo? = synchronized(AccessibilityTreeLock.monitor) {
            if (!provider.isReady()) return@synchronized null
            val windows=provider.getAccessibilityWindows()
            try {
                for (window in windows) {
                    val root=window.root ?: continue; val focused=root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                    if (focused != null && focused.isEditable) { @Suppress("DEPRECATION") root.recycle(); return@synchronized focused }
                    @Suppress("DEPRECATION") root.recycle(); if (focused != null) { @Suppress("DEPRECATION") focused.recycle() }
                }
                if (windows.isEmpty()) { val root=provider.getRootNode() ?: return@synchronized null; val focused=root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT); @Suppress("DEPRECATION") root.recycle(); return@synchronized focused?.takeIf{it.isEditable} }
                null
            } finally { for (window in windows) { @Suppress("DEPRECATION") window.recycle() } }
        }
    }
