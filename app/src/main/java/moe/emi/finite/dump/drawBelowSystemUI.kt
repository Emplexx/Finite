package moe.emi.finite.dump

import android.graphics.Color
import android.os.Build.VERSION.SDK_INT
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.core.view.marginTop
import androidx.core.view.marginBottom
import androidx.core.view.updateLayoutParams
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.R as MdcR

fun BottomSheetDialog.drawBelowSystemUi(viewInDialog: View) {
	if (SDK_INT < 21) return // Unsupported
	
	// We are not using padding but margin because using padding hasn't been successful.
	// See this request: https://github.com/chrisbanes/insetter/issues/66
	
	// We defer getting the initial margin to avoid retrieving it before the view is added,
	// where the LayoutParams that contain the margin values would not be there yet.
	
	val initialTopMargin by lazy(LazyThreadSafetyMode.NONE) { viewInDialog.marginTop }
	val initialBottomMargin by lazy(LazyThreadSafetyMode.NONE) { viewInDialog.marginBottom }
	
	viewInDialog.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
		@RequiresApi(21)
		override fun onViewAttachedToWindow(v: View) {
			val designBottomSheet = findViewById<FrameLayout>(MdcR.id.design_bottom_sheet)!!
			designBottomSheet.setBackgroundColor(Color.TRANSPARENT)
			designBottomSheet.setOnApplyWindowInsetsListener { _, insets ->
				viewInDialog.updateLayoutParams<ViewGroup.MarginLayoutParams> {
					topMargin = initialTopMargin + insets.systemWindowInsetTop
					bottomMargin = initialBottomMargin + insets.systemWindowInsetBottom
				}
				insets.consumeSystemWindowInsets()
			}
			val container = window!!.peekDecorView().findViewById<ViewGroup>(MdcR.id.container)!!
			container.fitsSystemWindows = false
		}
		
		override fun onViewDetachedFromWindow(v: View) = Unit
	})
}