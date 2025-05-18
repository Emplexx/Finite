package moe.emi.finite.dump.android

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import moe.emi.finite.R
import moe.emi.finite.dump.fDp

class ReceiptEdgeView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyle: Int = 0,
) : View(context, attrs, defStyle) {

	var color = 0xFFFFFFFF.toInt()
		set(value) {
			field = value
			invalidate()
		}
	
	init {
		val array: TypedArray = context.obtainStyledAttributes(
			attrs, R.styleable.ReceiptEdgeView, defStyle, 0)
		
		color = array.getColor(R.styleable.ReceiptEdgeView_color, 0xFFFFFFFF.toInt())
		array.recycle()
	}
	
	private val path = Path()
	private val paint = Paint()
	private val size = 8.fDp
	
	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		
		var count = measuredWidth / size.toInt()
		val rem = measuredWidth % size
		if (rem > 0) count += 1
		if (count % 2 == 1) count += 1
		
		
		drawTriangles(count)
		
		val width = count * size
		
		canvas.save()
		canvas.translate((measuredWidth-width) / 2, 0f)
		
		paint.color = color
		canvas.drawPath(path, paint)
		canvas.restore()
	}
	
	private fun drawTriangles(times: Int) {
		
		path.moveTo(0f, 0f)
		
		val offset = 16.fDp
		
		path.lineTo(0f, offset)
		
		for (i in 1..times) {
			val isEven = i % 2
			path.lineTo(i * size, size * isEven + offset)
		}
		
		path.lineTo(times * size, 0f)
	}
	
}