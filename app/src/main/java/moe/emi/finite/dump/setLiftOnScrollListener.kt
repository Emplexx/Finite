package moe.emi.finite.dump

import android.util.Log
import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.setLiftOnScrollListener(
	listener: LiftOnScrollListener
) {
	this.addOnScrollListener(object : RecyclerView.OnScrollListener() {
		
		var shouldElevate = true
		var shouldSettle = false
		
		override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
			
			Log.d("dy", "$dy")
			
			if (!recyclerView.canScrollVertically(-1) && shouldSettle) {
				shouldElevate = true
				shouldSettle = false
				
				// elevation from 2 to 0
				listener.callback(false)
				
			} else if (recyclerView.canScrollVertically(-1) && shouldElevate) {
				shouldElevate = false
				shouldSettle = true
				
				// elevation from 0 to 2
				listener.callback(true)
				
			}
		}
	})
}