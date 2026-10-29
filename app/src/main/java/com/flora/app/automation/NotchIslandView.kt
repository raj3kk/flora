package com.flora.app.automation

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import com.flora.app.MainActivity
import com.flora.app.voice.IslandState
import com.flora.app.voice.NotchState

/**
 * Notch island view (N1, user order 2026-10-29) — Flora ka
 * apna original design, koi creator/Apple asset ya text nahi.
 * Top-center pill: baayin taraf orb (FloraOrb wali default
 * gulabi rang-jodi, badli NAHI gayi), daayin taraf 5 wave
 * bars jo sirf asli amplitude par hilti hain (chuppi me
 * chhoti-flat — koi idle animation nahi, battery ka khayal).
 *
 * - Ye view khud kabhi mic NAHI kholta (NOTCH-08 / R-N-6):
 *   amplitude NotchState se aati hai, jo active listener
 *   likhta hai. View sirf state/amplitude badalne par
 *   invalidate hota hai, timer par nahi.
 * - Tap (R-N-2): bolte waqt = interrupt + listen (registered
 *   handler), warna Flora chat aage. Kabhi mute toggle nahi.
 * - Bounds sirf pill jitne hain + FLAG_NOT_FOCUSABLE (host
 *   service lagata hai) — pill ke neeche ke touches app ko
 *   milte hain, island unhe rokta nahi.
 */
class NotchIslandView(
    context: Context,
    initialState: IslandState,
) : View(context) {

    private var state: IslandState = initialState
    private var amplitude: Float = NotchState.amplitude

    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DD1F2937")
        style = Paint.Style.FILL
    }
    // Paint.color Int (@ColorInt) chahta hai — android.graphics
    // .Color object nahi (CI error tha: lines 45/49/53). Hex
    // literal Long hota hai, isliye .toInt() se bit pattern
    // wahi rehta hai (rang FloraOrb default jodi se unchanged).
    private val orbOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7C4A03.toInt()
        style = Paint.Style.FILL
    }
    private val orbInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFC978.toInt()
        style = Paint.Style.FILL
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFC978.toInt()
        style = Paint.Style.FILL
    }
    private val pillRect = RectF()

    private val stateListener: (IslandState, Float) -> Unit = { newState, newAmp ->
        state = newState
        amplitude = newAmp
        postInvalidate()
    }

    init {
        contentDescription = "Flora baat-cheet"
        setOnClickListener {
            val handler = NotchState.onIslandTap
            if (handler != null) {
                try {
                    handler()
                } catch (e: Exception) {
                }
            } else {
                try {
                    val launch = Intent(context, MainActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                        )
                    }
                    context.startActivity(launch)
                } catch (e: Exception) {
                }
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        state = NotchState.islandState
        amplitude = NotchState.amplitude
        NotchState.addListener(stateListener)
    }

    override fun onDetachedFromWindow() {
        try {
            NotchState.removeListener(stateListener)
        } catch (e: Exception) {
        }
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        pillRect.set(0f, 0f, w, h)
        canvas.drawRoundRect(pillRect, h / 2f, h / 2f, pillPaint)
        // Orb: bolte/sochte waqt thoda bada, baaki samanya.
        val orbBase = h * 0.30f
        val orbRadius = when (state) {
            IslandState.SPEAKING -> orbBase * 1.25f
            IslandState.THINKING -> orbBase * 1.10f
            else -> orbBase
        }
        val orbCx = h * 0.55f
        val orbCy = h / 2f
        canvas.drawCircle(orbCx, orbCy, orbRadius, orbOuterPaint)
        canvas.drawCircle(orbCx, orbCy, orbRadius * 0.6f, orbInnerPaint)
        // Wave bars: amplitude-driven, har bar ka factor fixed
        // (koi animation nahi) — chuppi me sab chhoti-flat.
        val barFactors = floatArrayOf(0.7f, 1.0f, 0.85f, 1.0f, 0.6f)
        val barWidth = h * 0.09f
        val gap = h * 0.10f
        val startX = h * 1.05f
        val maxBarH = h * 0.55f
        val minBarH = h * 0.10f
        for (i in barFactors.indices) {
            val barH = minBarH + amplitude.coerceIn(0f, 1f) * barFactors[i] * maxBarH
            val left = startX + i * (barWidth + gap)
            val top = orbCy - barH / 2f
            val bottom = orbCy + barH / 2f
            pillRect.set(left, top, left + barWidth, bottom)
            canvas.drawRoundRect(pillRect, barWidth / 2f, barWidth / 2f, barPaint)
        }
    }
}
