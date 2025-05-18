package moe.emi.finite.components.upgrade.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult

internal val BillingResult.isSuccess: Boolean
	get() = responseCode == BillingClient.BillingResponseCode.OK

const val proUpgradeSku = "pro"