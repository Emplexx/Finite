package moe.emi.finite.components.upgrade

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import moe.emi.finite.components.upgrade.billing.BillingConnection
import moe.emi.finite.components.upgrade.billing.isSuccess
import moe.emi.finite.components.upgrade.billing.proUpgradeSku
import moe.emi.finite.components.upgrade.cache.UpgradeState
import moe.emi.finite.di.VMFactory
import moe.emi.finite.di.container
import moe.emi.finite.di.singleViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class UpgradeViewModel(
	val billingConnection: Flow<BillingConnection>,
	val upgradeState: Flow<UpgradeState>,
) : ViewModel() {
	
	val isPro = upgradeState.map { it.isPro }
	
	val purchases = billingConnection
		.flatMapLatest { it.purchaseEvents }
		.filterNotNull()
		.map { (result, purchases) ->
			when {
				purchases?.firstOrNull()?.purchaseState == Purchase.PurchaseState.PENDING && result.isSuccess ->
					PurchaseResult.Pending
				
				result.isSuccess -> PurchaseResult.Success
				
				result.responseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE ->
					PurchaseResult.BillingError
				
				else -> PurchaseResult.Other
			}
		}
	
	private val _mockState = MutableStateFlow<PurchaseResult?>(null)
	val mockState = _mockState.filterNotNull()
	
	val productDetails = billingConnection
		.map {
			it.client.getUpgradeProductDetails()
		}
		.shareIn(viewModelScope, SharingStarted.Eagerly, 1)
	
	fun onPurchaseUpgrade(activity: Activity) = viewModelScope.launch {
		billingConnection
			.map {
				launchUpgradeBillingFlow(activity, it.client)
			}
			.take(1)
			.single()
	}
	
	fun testPurchaseDebug() {
		
		_mockState.value = PurchaseResult.BillingError
		
		return
		
		_mockState.value = PurchaseResult.Pending
		viewModelScope.launch {
			delay(5000)
			_mockState.value = PurchaseResult.Success
		}
	}
	
	companion object : VMFactory by singleViewModel({
		UpgradeViewModel(container.billingConnection, container.upgradeState)
	})
	
}

suspend fun BillingClient.getUpgradeProductDetails() = either {
	
	val (result, products) = queryProductDetails(
		QueryProductDetailsParams.newBuilder()
			.setProductList(
				listOf(
					QueryProductDetailsParams.Product.newBuilder()
						.setProductId(proUpgradeSku)
						.setProductType(ProductType.INAPP)
						.build()
				)
			)
			.build()
	)
	
	if (result.responseCode != BillingClient.BillingResponseCode.OK) raise(BillingError(result))
	
	val product = products.orEmpty().singleOrNull { it.productId == proUpgradeSku }
	
	ensureNotNull(product) { IllegalError }
}

sealed interface IBillingError

data class BillingError(val result: BillingResult) : IBillingError
data object IllegalError : IBillingError

// wrappers?

suspend fun launchUpgradeBillingFlow(
	activity: Activity,
	client: BillingClient
) = either {
	
	val result = client.getUpgradeProductDetails().bind()
		.let {
			BillingFlowParams.ProductDetailsParams.newBuilder()
				.setProductDetails(it)
				.build()
		}
		.let(::listOf)
		.let { BillingFlowParams.newBuilder().setProductDetailsParamsList(it).build() }
		.let { client.launchBillingFlow(activity, it) }
	
	if (result.responseCode != BillingClient.BillingResponseCode.OK) raise(BillingError(result))
}

suspend fun BillingClient.acknowledgePurchase(purchase: Purchase) = either {
	
	val result = acknowledgePurchase(
		AcknowledgePurchaseParams.newBuilder()
			.setPurchaseToken(purchase.purchaseToken)
			.build()
	)
	
	if (result.responseCode != BillingClient.BillingResponseCode.OK) raise(BillingError(result))
	
}