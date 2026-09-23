package com.danielealbano.androidremotecontrolmcp.services.accessibility

import android.view.KeyEvent

data class TextSnapshot(
    val text: CharSequence,
    val offset: Int,
    val selectionStart: Int,
    val selectionEnd: Int,
)

interface TypeInputController {
    fun isReady(): Boolean
    fun commitText(text: CharSequence, newCursorPosition: Int): Boolean
    fun setSelection(start: Int, end: Int): Boolean
    fun getSurroundingText(beforeLength: Int, afterLength: Int, flags: Int): TextSnapshot?
    fun performContextMenuAction(id: Int): Boolean
    fun sendKeyEvent(event: KeyEvent): Boolean
    fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean
}
