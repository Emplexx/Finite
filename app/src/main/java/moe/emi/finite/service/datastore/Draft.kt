package moe.emi.finite.service.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import moe.emi.finite.FiniteApp
import moe.emi.finite.dump.jsonSerializer
import moe.emi.finite.service.model.Subscription

val Context.storedDraft: DataStore<Subscription?> by dataStore("draft.json", jsonSerializer(null))

suspend fun setDraft(draft: Subscription) {
	FiniteApp.instance.storedDraft.updateData { draft }
//	FiniteApp.instance.storeGeneral.write(Keys.Draft, Json.encodeToString(draft))
}

fun getDraft(): Flow<Subscription?> {
	
	return FiniteApp.instance.storedDraft.data
	
//	return FiniteApp.instance.storeGeneral.read(Keys.Draft).map { encoded ->
//		encoded?.let { Json.decodeFromString(it) }
//	}
}

suspend fun clearDraft() {
	FiniteApp.instance.storedDraft.updateData { null }
//	FiniteApp.instance.storeGeneral.edit { it.remove(Keys.Draft) }
}

