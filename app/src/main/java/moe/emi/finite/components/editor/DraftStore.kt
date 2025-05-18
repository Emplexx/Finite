package moe.emi.finite.components.editor

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import moe.emi.finite.dump.jsonSerializer
import moe.emi.finite.core.model.Subscription

private val Context.draftDataStore: DataStore<Subscription?> by dataStore(
	"draft.json",
	jsonSerializer(null)
)

fun Context.newDraftStore(scope: CoroutineScope) = DraftStore(draftDataStore, scope)

class DraftStore(
	private val store: DataStore<Subscription?>,
	scope: CoroutineScope
) : DataStore<Subscription?> by store {
	
	private val draftFlow = data.stateIn(scope, SharingStarted.Eagerly, null)
	val draft get() = draftFlow.value
	
	suspend fun clear() = updateData { null }
	
}