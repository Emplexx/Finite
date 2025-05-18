package moe.emi.finite.components.list

import moe.emi.finite.components.list.domain.ConvertedAmount
import moe.emi.finite.components.list.domain.SubscriptionItemUiModel
import moe.emi.finite.components.list.domain.SubscriptionListUiModel
import moe.emi.finite.components.list.domain.TotalView
import moe.emi.finite.components.settings.store.AppSettings
import moe.emi.finite.components.settings.store.colorOptions
import moe.emi.finite.core.model.Period
import moe.emi.finite.core.model.Timespan
import moe.emi.finite.core.rates.model.FetchedRates
import moe.emi.finite.core.model.Subscription

fun makeSubscriptionListUiModel(
	subscriptions: List<Subscription>,
	fetchedRates: FetchedRates?,
	totalView: TotalView,
	settings: AppSettings,
): SubscriptionListUiModel {
	
	val (active, inactive) = convertPrices(
		subscriptions,
		fetchedRates,
		totalView,
		settings
	).partition { it.model.isActive }
	
	val total = active.sumOf { it.convertedAmount.amountMatchedToTimeframe }
	
	return SubscriptionListUiModel(
		active,
		inactive,
		totalView,
		total,
		settings.preferredCurrency,
		settings.colorOptions
	)
}

private fun convertPrices(
	subscriptions: List<Subscription>,
	fetchedRates: FetchedRates?,
	totalView: TotalView,
	settings: AppSettings
) = subscriptions
	// TODO important: support prices that couldn't be converted instead of filtering them out
	.mapNotNull { subscription ->
		
		val amount = fetchedRates
			?.convert(subscription.price, subscription.currency, settings.preferredCurrency)
			?.let {
				it to when (totalView) {
					TotalView.Yearly -> subscription.period.priceEveryYear(it)
					TotalView.Monthly -> subscription.period.priceEveryMonth(it)
					TotalView.Weekly -> subscription.period.priceEveryWeek(it)
				}
			}
			?.let { (converted, convertedMatched) ->
				ConvertedAmount(
					totalView,
					convertedMatched,
					converted,
					subscription.currency != settings.preferredCurrency
				)
			}
			?: return@mapNotNull null
		
		SubscriptionItemUiModel(
			subscription,
			settings.preferredCurrency,
			amount,
			settings.showTimeLeft
		)
	}

fun Period.priceEveryYear(price: Double): Double =
	when (this.unit) {
		Timespan.Year -> price / this.length
		Timespan.Month -> price * (12 / this.length)
		Timespan.Week -> price * (52.14 / this.length)
		Timespan.Day -> price * (365 / this.length)
	}

fun Period.priceEveryMonth(price: Double): Double =
	when (this.unit) {
		Timespan.Year -> price / this.length / 12
		Timespan.Month -> price / this.length
		Timespan.Week -> price * (4.35 / this.length)
		Timespan.Day -> price * (30.42 / this.length)
	}

fun Period.priceEveryWeek(price: Double): Double =
	when (this.unit) {
		Timespan.Year -> price / this.length / 52.14
		Timespan.Month -> price / this.length / 4.35
		Timespan.Week -> price / this.length
		Timespan.Day -> price * (7 / this.length)
	}

