package moe.emi.finite.ui.home

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.View
import androidx.transition.ChangeBounds
import androidx.transition.ChangeClipBounds
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.google.android.material.card.MaterialCardView
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem
import moe.emi.finite.R
import moe.emi.finite.databinding.ItemSubscriptionBinding
import moe.emi.finite.dump.FastOutExtraSlowInInterpolator
import moe.emi.finite.dump.fDp
import moe.emi.finite.dump.gone
import moe.emi.finite.dump.visible
import moe.emi.finite.service.data.Currency
import moe.emi.finite.service.data.Subscription
import moe.emi.finite.service.data.Timespan
import moe.emi.finite.ui.colors.ItemColors
import java.math.RoundingMode
import java.text.DecimalFormat

@SuppressLint("RestrictedApi")
class SubscriptionAdapterItem(
	val model: Subscription,
	var preferredCurrency: Currency,
	
	var convertedAmount: SubscriptionsListFragment.ConvertedAmount,
	var showUpcomingTime: Boolean,
	var palette: ItemColors,
	
	val onClick: (MaterialCardView) -> Unit,
	val onLongClick: () -> Unit,
) : BindableItem<ItemSubscriptionBinding>() {
	
	fun updateShowTimeLeft(boolean: Boolean) {
		showUpcomingTime = boolean
		notifyChanged("TimeLeft")
	}
	
	fun updateAmount(amount: SubscriptionsListFragment.ConvertedAmount) {
		convertedAmount = amount
		notifyChanged("Amount")
	}
	
	fun updateCurrency(preferredCurrency: Currency) {
		this.preferredCurrency = preferredCurrency
		notifyChanged("Amount")
	}
	
	fun updatePalette(palette: ItemColors) {
		this.palette = palette
		notifyChanged("Palette")
	}
	
	override fun bind(binding: ItemSubscriptionBinding, position: Int) {
		binding.root.transitionName = "card_${model.id}"
		
		bindPalette(binding)
		
		binding.textName.text = model.name
		
		binding.textDescription.visible = model.description.isNotBlank()
		binding.textDescription.text = model.description
		
//		binding.textPrice.text = convertedAmount.amountMatchedToTimeframe.round(2).toString()
		
		
		
		
		binding.textCurrencySign.text = preferredCurrency.symbol
		
		binding.textPrice.setCharacterLists("0123456789.")
		binding.textPrice.animationInterpolator = FastOutExtraSlowInInterpolator()
//		binding.layoutPrice.enableAnimateChildren(200L)
		
		bindAmount(binding)
		bindTimeLeft(binding)
		
		binding.root.setOnClickListener {
			onClick(binding.root)
		}
		binding.root.setOnLongClickListener { onLongClick(); true }
	}
	
	fun bindTimeLeft(binding: ItemSubscriptionBinding) {
		binding.layoutTimeLeft.visibility = if (showUpcomingTime) View.VISIBLE else View.GONE
		
		binding.textTimeLeft.text = buildString {
			val period = model.periodUntilNextPayment() ?: return@buildString
			
			if (period.years > 0) {
				append(period.years)
				append("y")
			} else if (period.months > 0) {
				append(period.months)
				append("m")
			} else if (period.days / 7 > 0) {
				append(period.days / 7)
				append("w")
			} else {
				append(period.days)
				append("d")
			}
		}
	}
	
	fun bindPalette(binding: ItemSubscriptionBinding) {
		binding.root.backgroundTintList = ColorStateList.valueOf(palette.container)
		
		val onContainer = palette.onContainer
		binding.textName.setTextColor(onContainer)
		binding.textDescription.setTextColor(onContainer)
		binding.textPriceSubtitle.setTextColor(onContainer)
		binding.textPrice.textColor = onContainer
		binding.textTimeLeft.setTextColor(onContainer)
	}
	
	fun bindAmount(binding: ItemSubscriptionBinding) {
		
		val changeClipBounds = ChangeClipBounds().apply {
			this.addTarget(binding.textPrice)
		}
//		TransitionManager.beginDelayedTransition(binding.root, changeClipBounds)
		
		binding.textPrice.text = buildString {
			
			if (convertedAmount.from.code != convertedAmount.to.code) {
				append("≈ ")
			}
			
			preferredCurrency.symbol?.let {
				append(it)
				append(" ")
			}
			DecimalFormat("0.00")
				.apply { roundingMode = RoundingMode.CEILING }
				.format(convertedAmount.amountMatchedToTimeframe)
				.let(::append)
		}
		
		binding.textPriceSubtitle.text = buildString {
			
			append(DecimalFormat("0.00")
				.apply { roundingMode = RoundingMode.CEILING }
				.format(convertedAmount.amountOriginal))
			append(" ")
			
			val period = model.period.stringId
				?.let { binding.root.context.getString(it) }
				?: buildString {
					
					append("/ ")
					if (model.period.every > 1) append(model.period.every)
					append(when (model.period.unit) {
						Timespan.Day -> "d"
						Timespan.Week -> "w"
						Timespan.Month -> "m"
						Timespan.Year -> "y"
					})
				}
			append(period)
		}
		
		
		val changeBounds = ChangeBounds().apply {
//			excludeTarget(binding.textPrice, true)
//			excludeTarget(binding.textCurrencySign, true)
//			excludeTarget(binding.layoutPrice, true)
//			addTarget(binding.layoutPriceTogether)
//			addTarget(binding.textPriceSubtitle)
		}
		
		if (convertedAmount.amountMatchedToTimeframe != convertedAmount.amountOriginal) {
			if (binding.textPriceSubtitle.visible) return
			val set = TransitionSet().apply {
				addTransition(changeBounds)
//				addTransition(changeClipBounds)
				interpolator = FastOutExtraSlowInInterpolator()
				duration = 400
			}
			TransitionManager.beginDelayedTransition(binding.root, set)
			
			
			binding.textPriceSubtitle.visible = true
			binding.textPriceSubtitle.alpha = 0f
			binding.textPriceSubtitle.animate()
				.alpha(1f)
				.setListener(object : Animator.AnimatorListener {
					
					override fun onAnimationEnd(animation: Animator) {
						binding.textPriceSubtitle.visible = true
					}
					
					override fun onAnimationStart(animation: Animator) = Unit
					override fun onAnimationCancel(animation: Animator) = Unit
					override fun onAnimationRepeat(animation: Animator) = Unit
				})
			
//			binding.layoutPrice.animate()
//				.translationY(-8.fDp)
//				.apply {
//					interpolator = FastOutExtraSlowInInterpolator()
//					duration = 400
//				}
			
		} else {
			
			val fade = Fade().apply {
				addTarget(binding.textPriceSubtitle)
			}
			val set = TransitionSet().apply {
				addTransition(changeBounds)
				addTransition(fade)
//				addTransition(changeClipBounds)
				interpolator = FastOutExtraSlowInInterpolator()
				duration = 400
			}
			TransitionManager.beginDelayedTransition(binding.root, set)
			
//			binding.textPriceSubtitle.alpha = 1f
//			binding.textPriceSubtitle.animate()
//				.alpha(0f)
			
//				.setListener(object : Animator.AnimatorListener {
//
//					override fun onAnimationEnd(animation: Animator) {
//						TransitionManager.beginDelayedTransition(binding.root, changeBounds)
//						binding.textPriceSubtitle.visible = false
//					}
//
//					override fun onAnimationStart(animation: Animator) = Unit
//					override fun onAnimationCancel(animation: Animator) = Unit
//					override fun onAnimationRepeat(animation: Animator) = Unit
//				})
			binding.textPriceSubtitle.gone = true
		}
		
//		if (convertedAmount.amountMatchedToTimeframe != convertedAmount.amountOriginal
//			&& binding.textPriceSubtitle.visible) {
//
//			val changeClipBounds2 = ChangeClipBounds().apply {
//				this.addTarget(binding.textPrice)
//				this.addTarget(binding.textCurrencySign)
//				this.addTarget(binding.layoutPrice)
//			}
//			val set = TransitionSet().apply {
//				addTransition(changeBounds)
//				addTransition(changeClipBounds2)
//				interpolator = FastOutExtraSlowInInterpolator()
//				duration = 400
//			}
//			TransitionManager.beginDelayedTransition(binding.root, set)
//		}
		
//		binding.textPriceSubtitle.visible =
//			convertedAmount.amountMatchedToTimeframe != convertedAmount.amountOriginal
	}
	
	override fun bind(
		viewBinding: ItemSubscriptionBinding,
		position: Int,
		payloads: MutableList<Any>
	) {
		for (i in payloads) {
			when (i) {
				"TimeLeft" -> bindTimeLeft(viewBinding)
				"Amount" -> bindAmount(viewBinding)
				"Palette" -> bindPalette(viewBinding)
			}
		}
		if (payloads.isEmpty()) super.bind(viewBinding, position, payloads)
	}
	
	override fun isSameAs(other: Item<*>): Boolean =
		when (other) {
			is SubscriptionAdapterItem -> model.id == other.model.id
			else -> super.isSameAs(other)
		}
	
	override fun hasSameContentAs(other: Item<*>): Boolean =
		when (other) {
			is SubscriptionAdapterItem -> model == other.model
//					&& convertedAmount == other.convertedAmount
//					&& showUpcomingTime != other.showUpcomingTime
			else -> super.isSameAs(other)
		}
	
	override fun getExtras(): MutableMap<String, Any> =
		mutableMapOf(
			"CurrencyItem" to 0
		)
	
	override fun getLayout() = R.layout.item_subscription
	override fun initializeViewBinding(view: View) = ItemSubscriptionBinding.bind(view)
	
	companion object {
		fun ItemSubscriptionBinding.hideSubtitleAlt() {
			this.layoutPrice.animate()
				.translationY(0.fDp)
				.apply {
					interpolator = FastOutExtraSlowInInterpolator()
					duration = 400
				}
			this.textPriceSubtitle.animate()
				.alpha(0f)
		}
	}
	
}