package com.infiniterunner.app

import android.content.Context
import android.graphics.Canvas
import android.view.View

/**
 * Legacy compatibility view. The active game uses GameViewV3.
 * Kept intentionally minimal so old references do not break the build.
 */
class GameViewV2(context: Context) : View(context) {
    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(android.graphics.Color.BLACK)
    }
}
