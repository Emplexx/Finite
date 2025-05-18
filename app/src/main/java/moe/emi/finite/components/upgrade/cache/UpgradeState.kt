package moe.emi.finite.components.upgrade.cache

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import moe.emi.finite.components.upgrade.billing.BillingConnection
import moe.emi.finite.components.upgrade.billing.proUpgradeSku
import moe.emi.finite.core.preferences.Pref
import moe.emi.finite.core.preferences.get
import moe.emi.finite.core.db.SubscriptionDao
import java.time.Instant
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalCoroutinesApi::class)
fun createUpgradeState(
	dataStore: DataStore<Preferences>,
	connFlow: Flow<BillingConnection>,
	subscriptionsDao: SubscriptionDao,
): Flow<UpgradeState> =
	connFlow.flatMapLatest { conn ->
		
		val purchasesFlow = conn.allPurchases
			.map { purchases ->
				purchases.filter {
					it.purchaseState == Purchase.PurchaseState.PURCHASED
							&& it.products.contains(proUpgradeSku)
				}
			}
		
		val subCountFlow = subscriptionsDao.getAllObservable().map { it.size }
		
		combine(purchasesFlow, dataStore[Pref.proLastSeenAt], subCountFlow) { purchases, lastSeenAt, subCount ->
			UpgradeState(
				purchase = purchases.firstOrNull(),
				proLastSeenAt = lastSeenAt,
				dbSubscriptionCount = subCount
			)
		}
		
	}

data class UpgradeState(
	val purchase: Purchase?,
	val proLastSeenAt: Instant,
	val dbSubscriptionCount: Int
) {
	
	val isPro = purchase != null || ChronoUnit.DAYS.between(proLastSeenAt, Instant.now()) < 14
	val isIllegalPro = !isPro && dbSubscriptionCount > 5
}