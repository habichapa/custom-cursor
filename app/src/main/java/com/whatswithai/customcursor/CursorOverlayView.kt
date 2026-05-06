package com.whatswithai.customcursor

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.PointerIcon
import android.view.View

/**
 * Full-screen transparent overlay view that:
 *  - Hides the system cursor (PointerIcon.TYPE_NULL)
 *  - Tracks mouse hover position via ACTION_HOVER_MOVE
 *  - Draws a custom cursor at that position using Canvas
 */
class CursorOverlayView(context: Context) : View(context) {

    // ── Position ──────────────────────────────────────────────────────────────
    private var cursorX = -200f
    private var cursorY = -200f

    // ── Cursor style ──────────────────────────────────────────────────────────
    enum class CursorStyle { ARROW, CROSSHAIR, DOT, RING }
    var cursorStyle: CursorStyle = CursorStyle.ARROW
    var cursorColor: Int = Color.WHITE
    var cursorSize: Float = 1.0f   // scale multiplier

    // ── Paints ────────────────────────────────────────────────────────────────
    private val fillPaint   = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // ── Cursor geometry constants ─────────────────────────────────────────────
    // Arrow cursor: tip at (0,0), pointing up-left
    private val arrowPath = Path()
    private val arrowShadowPath = Path()

    init {
        // Suppress system cursor while this view is under the pointer
        pointerIcon = PointerIcon.getSystemIcon(context, PointerIcon.TYPE_NULL)

        buildArrowPath()
        updatePaints()
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun updateStyle(style: CursorStyle, color: Int, size: Float) {
        cursorStyle = style
        cursorColor = color
        cursorSize  = size
        updatePaints()
        invalidate()
    }

    // ── Path builders ─────────────────────────────────────────────────────────

    private fun buildArrowPath() {
        // Classic arrow: tip at origin, 28px × 33px body
        arrowPath.reset()
        arrowPath.moveTo(0f,  0f)
        arrowPath.lineTo(0f,  28f)
        arrowPath.lineTo(7f,  21f)
        arrowPath.lineTo(13f, 33f)
        arrowPath.lineTo(17f, 31f)
        arrowPath.lineTo(11f, 19f)
        arrowPath.lineTo(20f, 19f)
        arrowPath.close()

        arrowShadowPath.set(arrowPath)
    }

    // ── Paint setup ───────────────────────────────────────────────────────────

    private fun updatePaints() {
        fillPaint.apply {
            style = Paint.Style.FILL
            color = cursorColor
        }
        strokePaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
            color = Color.argb(200, 0, 0, 0)
        }
        shadowPaint.apply {
            style = Paint.Style.FILL
            color = Color.argb(60, 0, 0, 0)
            maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL)
        }
    }

    // ── Event handling ────────────────────────────────────────────────────────

    override fun onHoverEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_HOVER_MOVE,
            MotionEvent.ACTION_HOVER_ENTER -> {
                cursorX = event.x
                cursorY = event.y
                invalidate()
            }
            MotionEvent.ACTION_HOVER_EXIT -> {
                cursorX = -200f
                cursorY = -200f
                invalidate()
            }
        }
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Update cursor position for clicks too
        cursorX = event.x
        cursorY = event.y
        invalidate()
        // Return false so the overlay doesn't consume click events
        return false
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        if (cursorX < 0 || cursorY < 0) return

        val scale = cursorSize
        canvas.save()
        canvas.translate(cursorX, cursorY)
        canvas.scale(scale, scale)

        when (cursorStyle) {
            CursorStyle.ARROW     -> drawArrow(canvas)
            CursorStyle.CROSSHAIR -> drawCrosshair(canvas)
            CursorStyle.DOT       -> drawDot(canvas)
            CursorStyle.RING      -> drawRing(canvas)
        }

        canvas.restore()
    }

    // Arrow cursor
    private fun drawArrow(canvas: Canvas) {
        // Drop shadow (shift slightly)
        canvas.save()
        canvas.translate(2f, 2f)
        canvas.drawPath(arrowShadowPath, shadowPaint)
        canvas.restore()

        // Fill
        canvas.drawPath(arrowPath, fillPaint)
        // Outline
        canvas.drawPath(arrowPath, strokePaint)
    }

    // Crosshair cursor
    private fun drawCrosshair(canvas: Canvas) {
        val r = 14f
        val gap = 4f

        strokePaint.strokeWidth = 2f
        strokePaint.color = Color.argb(180, 0, 0, 0)
        fillPaint.strokeWidth = 1.5f

        fun lines(paint: Paint) {
            canvas.drawLine(-r, 0f, -gap, 0f, paint)
            canvas.drawLine( gap, 0f,  r, 0f, paint)
            canvas.drawLine(0f, -r, 0f, -gap, paint)
            canvas.drawLine(0f,  gap, 0f,  r, paint)
        }

        // Shadow offset
        canvas.save(); canvas.translate(1f, 1f)
        lines(shadowPaint.apply { style = Paint.Style.STROKE; strokeWidth = 3f })
        canvas.restore()

        // White lines
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 2f; color = cursorColor
        }
        lines(linePaint)

        // Center dot
        canvas.drawCircle(0f, 0f, 2.5f, fillPaint)
    }

    // Big dot cursor
    private fun drawDot(canvas: Canvas) {
        canvas.drawCircle(1f, 1f, 8f, shadowPaint)
        canvas.drawCircle(0f, 0f, 7f, fillPaint)
        strokePaint.strokeWidth = 1.5f
        strokePaint.style = Paint.Style.STROKE
        canvas.drawCircle(0f, 0f, 7f, strokePaint)
    }

    // Ring/circle cursor
    private fun drawRing(canvas: Canvas) {
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = cursorColor
        }
        // Shadow
        canvas.drawCircle(1f, 1f, 12f, shadowPaint.apply { style = Paint.Style.STROKE; strokeWidth = 4f })
        canvas.drawCircle(0f, 0f, 12f, ringPaint)
        // Center tiny dot
        canvas.drawCircle(0f, 0f, 2f, fillPaint)
    }
}
