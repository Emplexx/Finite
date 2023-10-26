package moe.emi.finite.dump

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.text.TextPaint
import android.util.AttributeSet
import com.google.android.material.color.MaterialColors
import com.google.android.material.textfield.TextInputEditText
import moe.emi.finite.R
import kotlin.math.roundToInt

public open class TextField : TextInputEditText {
	
	// Removing those java-ass style constructor overloads makes text fields behave weirdly
	// and frankly i don't care enough to debug it so i guess those are staying
	constructor(context: Context) : super(context)
	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
		getAttributes(context, attrs, 0)
	}
	constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
		context,
		attrs,
		defStyleAttr
	) { getAttributes(context, attrs, defStyleAttr) }
	
	
	
	var textPaint = TextPaint()
	var suffixSpacer: String? = " "
	var suffixText: String? = ""
	var suffixPadding = 0f
	var suffixTextColor = currentTextColor
	var suffixTypeface = typeface
	
	var hintAlpha = MaterialColors.ALPHA_MEDIUM
	
	override fun onDraw(c: Canvas) {
		super.onDraw(c)
		
		drawSuffix(c)
	}
	
	protected open fun drawSuffix(c: Canvas) {
		var suffixXPosition = textPaint.measureText(text.toString()).toInt() + textPaint.measureText(suffixSpacer).toInt() + paddingLeft
		if (text.toString().isEmpty() && hint.toString().isNotEmpty()) {
			suffixXPosition = textPaint.measureText(hint.toString()).toInt() + textPaint.measureText(suffixSpacer).toInt() + paddingLeft
		}
		c.drawText(
			suffixText!!,
			Math.max(suffixXPosition.toFloat(), suffixPadding),
			baseline.toFloat(),
			
			textPaint
		)
	}

	override fun onFinishInflate() {
		super.onFinishInflate()

		textPaint.color = suffixTextColor
		textPaint.textSize = textSize
		textPaint.textAlign = Paint.Align.LEFT
		textPaint.typeface = typeface
		textPaint.letterSpacing = letterSpacing
		
		val alpha = (MaterialColors.ALPHA_MEDIUM * 255.0).roundToInt()
		
		textPaint.alpha = alpha // TODO: better alpha handling?
		
		//val suffixTextColor = Color.argb(alpha, Color.red(suffixTextColor), Color.green(suffixTextColor), Color.blue(suffixTextColor))
		
		// TODO ?
		val hintColor = this.currentHintTextColor.alpha(hintAlpha)
		
		this.setHintTextColor(hintColor)
	}
	
	private fun getAttributes(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
		val a: TypedArray =
			context.obtainStyledAttributes(attrs, R.styleable.TextField, defStyleAttr, 0)
		if (a != null) {
			
			suffixText = a.getString(R.styleable.TextField_suffix)
			if (suffixText == null) {
				suffixText = ""
			}
			suffixPadding = a.getDimension(R.styleable.TextField_suffixPadding, 0F)
			suffixTextColor = a.getColor(R.styleable.TextField_suffixTextColor, currentTextColor)
			
			hintAlpha = a.getFloat(R.styleable.TextField_hintAlpha, MaterialColors.ALPHA_MEDIUM)
			
			//suffixSpacer = a.getString(R.styleable.TextField_suffixSpacer)
		}
		a.recycle()
	}
}