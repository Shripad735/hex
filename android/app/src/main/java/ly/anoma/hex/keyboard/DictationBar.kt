package ly.anoma.hex.keyboard

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import ly.anoma.hex.R
import ly.anoma.hex.speech.SpeechPhase
import ly.anoma.hex.useInter
import kotlin.math.abs
import kotlin.math.roundToInt

/** Slim alternative to the full KeyPlane: Hex key, level, finish/cancel,
 * keyboard switcher and a jump to Hex settings. No typing, no suggestions. */
internal class DictationBar(context: Context, private val ime: HexKeyboard) : LinearLayout(context), ImeView {
    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()
    private fun color(id: Int) = context.getColor(id)
    private var idle = true
    private val ink = color(R.color.ink)
    private val statusText = TextView(context)
    private val scope = WaveScope(context)
    private lateinit var check: ImageView
    private val spinner = ProgressBar(context, null, 0, android.R.style.Widget_Material_ProgressBar_Small)
    private lateinit var doneKey: FrameLayout
    private lateinit var cancelKey: FrameLayout
    private var held = false
    private var tracking = false
    private var originX = 0f
    private var originY = 0f
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private val hold = Runnable { if (tracking) held = ime.beginSpeech() }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(4), dp(4), dp(4), dp(4))
        setBackgroundColor(color(R.color.keyboard))
        val hex = FrameLayout(context).apply {
            contentDescription = "Hex dictation"
            isFocusable = true
            setOnClickListener { ime.toggleSpeech() }
            background = StateListDrawable().apply {
                fun face(c: Int) = GradientDrawable().apply { cornerRadius = dp(14).toFloat(); setColor(c) }
                addState(intArrayOf(android.R.attr.state_pressed), face(color(R.color.pressed)))
                addState(intArrayOf(), face(ink))
            }
            addView(ImageView(context).apply {
                setImageResource(R.drawable.hex_mark)
                imageTintList = ColorStateList.valueOf(color(R.color.page))
            }, FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER))
            setOnTouchListener { v, e ->
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        held = false; tracking = true; originX = e.x; originY = e.y; v.isPressed = true
                        postDelayed(hold, 350)
                    }
                    MotionEvent.ACTION_MOVE -> if (!held && (abs(originX - e.x) > slop || abs(originY - e.y) > slop)) {
                        removeCallbacks(hold); tracking = false; v.isPressed = false
                    }
                    MotionEvent.ACTION_UP -> {
                        removeCallbacks(hold); v.isPressed = false
                        if (held) ime.finishSpeech() else if (tracking) v.performClick()
                        held = false; tracking = false
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        removeCallbacks(hold); if (held) ime.cancelSpeech()
                        held = false; tracking = false; v.isPressed = false
                    }
                }
                true
            }
        }
        addView(hex, LayoutParams(dp(52), dp(48)))
        statusText.apply {
            useInter(500); setTextColor(color(R.color.quiet)); textSize = 14f; maxLines = 1
            setPadding(dp(10), 0, dp(4), 0)
        }
        addView(statusText, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        scope.background = GradientDrawable().apply { setColor(color(R.color.sheet)); cornerRadius = dp(18).toFloat() }
        scope.clipToOutline = true
        addView(scope, LayoutParams(0, dp(32), 1f))
        cancelKey = barButton("Cancel dictation", R.drawable.ic_cancel, color(R.color.modifier), ime::cancelSpeech)
        doneKey = barButton("Finish dictation", R.drawable.ic_check, color(R.color.key), ime::finishSpeech)
        check = doneKey.getChildAt(0) as ImageView
        spinner.indeterminateTintList = ColorStateList.valueOf(ink)
        spinner.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        doneKey.addView(spinner, FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER))
        barButton("Next keyboard", R.drawable.ic_globe, color(R.color.modifier)) { ime.nextIme() }
        barButton("Hex settings", R.drawable.ic_settings, color(R.color.modifier)) {
            context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let(context::startActivity)
        }
        status(SpeechPhase.Idle, "Hold Hex to talk")
    }

    private fun barButton(description: String, icon: Int, fill: Int, click: () -> Unit): FrameLayout {
        val target = FrameLayout(context).apply {
            contentDescription = description; isFocusable = true; setOnClickListener { click() }
            background = StateListDrawable().apply {
                fun face(c: Int) = GradientDrawable().apply { cornerRadius = dp(16).toFloat(); setColor(c) }
                addState(intArrayOf(android.R.attr.state_pressed), face(color(R.color.pressed)))
                addState(intArrayOf(), face(fill))
            }
            addView(ImageView(context).apply {
                setImageResource(icon); imageTintList = ColorStateList.valueOf(ink)
            }, FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER))
        }
        addView(target, LayoutParams(dp(44), dp(44)))
        return target
    }

    override fun status(phase: SpeechPhase, hint: String) {
        idle = phase == SpeechPhase.Idle
        val processing = phase == SpeechPhase.Transcribing
        statusText.visibility = if (idle) VISIBLE else GONE
        statusText.text = hint
        scope.visibility = if (idle) GONE else VISIBLE
        cancelKey.visibility = if (idle) GONE else VISIBLE
        doneKey.visibility = if (idle) GONE else VISIBLE
        spinner.visibility = if (processing) VISIBLE else GONE
        check.visibility = if (processing) GONE else VISIBLE
        doneKey.isEnabled = !processing
        doneKey.contentDescription = if (processing) "Transcribing" else "Finish dictation"
        if (idle || phase == SpeechPhase.Opening) scope.clear()
    }

    override fun setLevel(value: Float) = scope.push(value)

    override fun cancelGestures() {
        removeCallbacks(hold)
        held = false; tracking = false
    }
}
