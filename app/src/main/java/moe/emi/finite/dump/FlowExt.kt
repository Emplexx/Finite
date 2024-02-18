package moe.emi.finite.dump

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Collect a flow on a lifecycle owner's scope, similar to LiveData's `observe`
 */
fun <T> Flow<T>.collectOn(owner: LifecycleOwner, collector: FlowCollector<T>) {
	owner.lifecycleScope.launch {
		owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
			withContext(Dispatchers.Main.immediate) {
				this@collectOn.collect(collector)
			}
		}
	}
}