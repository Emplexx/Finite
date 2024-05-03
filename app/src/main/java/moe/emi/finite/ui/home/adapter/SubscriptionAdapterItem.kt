package moe.emi.finite.ui.home.adapter

import android.animation.Animator
import android.content.res.ColorStateList
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.google.android.material.card.MaterialCardView
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem
import moe.emi.finite.R
import moe.emi.finite.databinding.ItemSubscriptionBinding
import moe.emi.finite.dump.FastOutExtraSlowInInterpolator
import moe.emi.finite.service.model.Currency
import moe.emi.finite.service.model.Subscription
import moe.emi.finite.service.model.Timespan
import moe.emi.finite.ui.colors.ItemColors
import moe.emi.finite.ui.home.model.ConvertedAmount
import java.math.RoundingMode
import java.text.DecimalFormat

class SubscriptionAdapterItem(
	val model: Subscription,
	private var preferredCurrency: Currency,
	
	private var convertedAmount: ConvertedAmount,
	private var showUpcomingTime: Boolean,
	var palette: ItemColors,
	
	val onClick: (MaterialCardView) -> Unit,
	val onLongClick: () -> Unit,
) : BindableItem<ItemSubscriptionBinding>() {
	
	override fun bind(binding: ItemSubscriptionBinding, position: Int) {
		bindView(binding)
		bindPalette(binding)
		bindAmount(binding, false)
		bindTimeLeft(binding)
	}
	
	private fun bindView(binding: ItemSubscriptionBinding) {
		binding.root.transitionName = "card_${model.id}"
		binding.textName.text = model.name
		binding.textDescription.isVisible = model.description.isNotBlank()
		binding.textDescription.text = model.description
		
		binding.textCurrencySign.text = preferredCurrency.symbol
		binding.textPrice.setCharacterLists("0123456789.")
		binding.textPrice.animationInterpolator = FastOutExtraSlowInInterpolator()
		val font = ResourcesCompat.getFont(binding.root.context, R.font.font_dm_ticker_small)
		binding.textPrice.typeface = font
		
		binding.root.setOnClickListener {
			onClick(binding.root)
		}
		binding.root.setOnLongClickListener { onLongClick(); true }
	}
	
	private fun bindTimeLeft(binding: ItemSubscriptionBinding) {
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
	
	private fun bindPalette(binding: ItemSubscriptionBinding) {
		binding.root.backgroundTintList = ColorStateList.valueOf(palette.container)
		
		val onContainer = palette.onContainer
		binding.textName.setTextColor(onContainer)
		binding.textPrice.textColor = onContainer
		binding.textTimeLeft.setTextColor(onContainer)
		
		binding.textDescription.setTextColor(palette.onContainerVariant)
		binding.textPriceSubtitle.setTextColor(palette.onContainerVariant)
	}
	
	private fun bindAmount(binding: ItemSubscriptionBinding, animate: Boolean = true) {
	
//		val changeClipBounds = ChangeClipBounds().apply {
//			this.addTarget(binding.textPrice)
//		}
//		TransitionManager.beginDelayedTransition(binding.root, changeClipBounds)
		
		binding.textPrice.setText(buildString {
			
			if (convertedAmount.from.currency != convertedAmount.to.currency) {
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
		}, animate)
		
		binding.textPriceSubtitle.text = buildString {
			
			append(DecimalFormat("0.00")
				.apply { roundingMode = RoundingMode.CEILING }
				.format(convertedAmount.amountOriginal))
			append(" ")
			
			val period = model.period.stringId
				?.let { binding.root.context.getString(it) }
				?: buildString {
					
					append("/ ")
					if (model.period.count > 1) append(model.period.count)
					append(when (model.period.timespan) {
						Timespan.Day -> "d"
						Timespan.Week -> "w"
						Timespan.Month -> "m"
						Timespan.Year -> "y"
					})
				}
			append(period)
		}
		
		if (!animate) {
			if (convertedAmount.amountMatchedToTimeframe != convertedAmount.amountOriginal) {
				binding.textPriceSubtitle.isVisible = true
			} else {
				binding.textPriceSubtitle.isGone = true
			}
		}
		
		
		
		val changeBounds = ChangeBounds().apply {
//			excludeTarget(binding.textPrice, true)
//			excludeTarget(binding.textCurrencySign, true)
//			excludeTarget(binding.layoutPrice, true)
//			addTarget(binding.layoutPriceTogether)
//			addTarget(binding.textPriceSubtitle)
		}
		
		if (convertedAmount.amountMatchedToTimeframe != convertedAmount.amountOriginal) {
			if (binding.textPriceSubtitle.isVisible) return
			val set = TransitionSet().apply {
				addTransition(changeBounds)
//				addTransition(changeClipBounds)
				interpolator = FastOutExtraSlowInInterpolator()
				duration = 400
			}
			TransitionManager.beginDelayedTransition(binding.root, set)
			
			
			binding.textPriceSubtitle.isVisible = true
			binding.textPriceSubtitle.alpha = 0f
			binding.textPriceSubtitle.animate()
				.alpha(1f)
				.setListener(object : Animator.AnimatorListener {
					
					override fun onAnimationEnd(animation: Animator) {
						binding.textPriceSubtitle.isVisible = true
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
			binding.textPriceSubtitle.isGone = true
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
		val changes = payloads.find { it is List<*> }
			.let { it as? List<*> }
			?.filterIsInstance<String>()
			?: return super.bind(viewBinding, position, payloads)
		
		bindView(viewBinding)
		if (changes.contains("Amount")) bindAmount(viewBinding)
		if (changes.contains("TimeLeft")) bindTimeLeft(viewBinding)
		if (changes.contains("Palette")) bindPalette(viewBinding)
	}
	
	
	override fun getChangePayload(new: Item<*>): Any? {
		if (new !is SubscriptionAdapterItem) return super.getChangePayload(new)
		
		return buildList {
			if (convertedAmount != new.convertedAmount
				|| preferredCurrency != new.preferredCurrency) add("Amount")
			if (showUpcomingTime != new.showUpcomingTime) add("TimeLeft")
			if (palette != new.palette) add("Palette")
		}
	}
	
	override fun hasSameContentAs(other: Item<*>): Boolean =
		other is SubscriptionAdapterItem && model == other.model
				&& preferredCurrency == other.preferredCurrency
				&& convertedAmount == other.convertedAmount
				&& showUpcomingTime == other.showUpcomingTime
				&& palette == other.palette
	
	override fun isSameAs(other: Item<*>): Boolean =
		other is SubscriptionAdapterItem && model.id == other.model.id
	
	
	override fun getExtras(): MutableMap<String, Any> =
		mutableMapOf("CurrencyItem" to 0)
	
	override fun getLayout() = R.layout.item_subscription
	override fun initializeViewBinding(view: View) = ItemSubscriptionBinding.bind(view)
	
	companion object {
//		fun ItemSubscriptionBinding.hideSubtitle() {
//			this.layoutPrice.animate()
//				.translationY(0.fDp)
//				.apply {
//					interpolator = FastOutExtraSlowInInterpolator()
//					duration = 400
//				}
//			this.textPriceSubtitle.animate()
//				.alpha(0f)
//		}
	}
	
}