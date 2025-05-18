package moe.emi.finite.components.upgrade.billing

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PurchaseState
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import moe.emi.finite.components.upgrade.acknowledgePurchase
import moe.emi.finite.core.preferences.Pref
import moe.emi.finite.core.preferences.set
import java.time.Instant

class BillingConnection(
	val client: BillingClient,
	val purchaseEvents: Flow<Pair<BillingResult, Collection<Purchase>?>?>,
	val cachedPurchases: Either<BillingResult, List<Purchase>>
) {
	
	val allPurchases = purchaseEvents
		.map {
			
			val (result, purchases) = it ?: return@map emptyList()
			
			val map = if (result.isSuccess) purchases.orEmpty() else emptyList()
			Log.d("Billing", "New purchases: $map")
			map
		}
		.map { purchases ->
			(purchases + cachedPurchases.getOrElse { emptyList() })
		}
}

class ServiceDisconnected() : RuntimeException()
class BillingSetupUnsuccessful() : RuntimeException()

fun createBillingConnection(context: Context) = callbackFlow {
	
	val purchases = MutableStateFlow<Pair<BillingResult, Collection<Purchase>?>?>(null)
	
	val client = BillingClient.newBuilder(context)
		.enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
		.setListener { result, purchase ->
			purchases.value = result to purchase
		}
		.build()
	
	val clientStateListener = object : BillingClientStateListener {
		
		override fun onBillingSetupFinished(result: BillingResult) {
			Log.d("Billing", "Billing Setup Finished $result")
			if (result.isSuccess) {
				trySendBlocking(client to purchases)
			}
			else close(BillingSetupUnsuccessful())
		}
		
		override fun onBillingServiceDisconnected() {
			Log.d("Billing", "Disconnected")
			close(ServiceDisconnected())
		}
	}
	
	client.startConnection(clientStateListener)
	
	awaitClose { client.endConnection() }
}
	.retryWhen { cause, attempt ->
		cause !is CancellationException && attempt < 5
	}
	.map { (client, purchaseEvents) ->
		
		val cachedResult = either {
			client.queryPurchasesAsync(
				QueryPurchasesParams.newBuilder()
					.setProductType(BillingClient.ProductType.INAPP)
					.build()
			)
				.also { if (!it.billingResult.isSuccess) raise(it.billingResult) }
				.purchasesList
		}
		
		BillingConnection(client, purchaseEvents, cachedResult)
	}

/**
 * Acknowledge all incoming purchases that are successful and not pending.
 * Extend pro access if a purchase is found or successful purchase was made.
 * Revoke pro access if no purchase is found.
 */
suspend fun processPurchasesForever(
	connectionFlow: Flow<BillingConnection>,
	dataStore: DataStore<Preferences>
) = coroutineScope {
	connectionFlow.collectLatest { conn ->
		
		conn.cachedPurchases
			.onRight {
				// If the list is empty, no purchase was made or it was refunded.
				if (it.isEmpty()) dataStore.set(Pref.proLastSeenAt, Instant.EPOCH)
				else dataStore.set(Pref.proLastSeenAt, Instant.now())
			}
		
		launch {
			conn.allPurchases
				.map { all ->
					all.filter { it.purchaseState == PurchaseState.PURCHASED && !it.isAcknowledged }
				}
				.collect { unacknowledged ->
					unacknowledged.onEach { conn.client.acknowledgePurchase(it) }
				}
		}
		
		launch {
			conn.purchaseEvents
				.map { event ->
					val (result, purchases) = event ?: return@map emptyList()
					if (!result.isSuccess) emptyList()
					else purchases
						.orEmpty()
						.filter { it.purchaseState == PurchaseState.PURCHASED }
				}
				.collect { purchased ->
					if (purchased.isNotEmpty()) dataStore.set(Pref.proLastSeenAt, Instant.now())
				}
		}
	}
}
	
