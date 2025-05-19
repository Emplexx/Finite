package moe.emi.finite.components.upgrade

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import arrow.core.Either
import com.android.billingclient.api.BillingClient
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import convenience.resources.Token
import convenience.resources.easingAttr
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import moe.emi.finite.BuildConfig
import moe.emi.finite.databinding.ActivityUpgradeBinding
import moe.emi.finite.dump.collectOn
import moe.emi.finite.dump.iDp
import moe.emi.finite.dump.zipWithLast
import moe.emi.finite.components.editor.ui.SubscriptionEditorActivity
import nl.dionsegijn.konfetti.core.Angle
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.Spread
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

class UpgradeSheet : BottomSheetDialogFragment() {
	
	companion object {
		fun openWithCreateContext() = UpgradeSheet().apply {
			arguments = Bundle().also {
				it.putBoolean("createContext", true)
			}
		}
	}
	
	private lateinit var binding: ActivityUpgradeBinding
	private val viewModel by viewModels<UpgradeViewModel> { UpgradeViewModel }
	
	private val isCreateContext get() = arguments?.getBoolean("createContext", false) ?: false
	
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		binding = ActivityUpgradeBinding.inflate(layoutInflater)
		return binding.root
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		
		ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//			v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
			binding.receiptHeight.updateLayoutParams {
				height = 112.iDp + systemBars.bottom
			}
			binding.mainLayout.setPadding(0, 0, 0, systemBars.bottom)
			binding.thanksButtonContainer.setPadding(0, 0, 0, systemBars.bottom)
			insets
		}
		
		if (isCreateContext) {
			binding.textTitle.text = "Limit reached"
//			binding.textDescrption.text = "You can add up to 5 subscriptions in the free version of Finite. Please remove existing subscriptions, or consider upgrading:"
			binding.textDescrption.text = "Upgrade to Finite Unlimited to manage more than 5 subscriptions"
		}
		else {
			binding.textTitle.text = "Finite Unlimited"
			binding.textDescrption.text = "Manage all your subscriptions in one place"
		}
		
		binding.buttonUpgrade.setOnClickListener {
			if (BuildConfig.DEBUG) viewModel.testPurchaseDebug()
			else viewModel.onPurchaseUpgrade(requireActivity())
		}
		
		binding.buttonContinue.setOnClickListener {
			lifecycleScope.launch {
				if (isCreateContext && viewModel.isPro.first()) {
					startActivity(Intent(requireActivity(), SubscriptionEditorActivity::class.java))
				}
				dismiss()
			}
		}
		
		viewModel.productDetails.collectOn(this) {
			when (it) {
				is Either.Left -> {
					
					when (val err = it.value) {
						is BillingError -> {
							if (err.result.responseCode == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED) {
								Snackbar.make(
									binding.root,
									"Could not connect to Google Play",
									Snackbar.LENGTH_INDEFINITE
								).show()
							}
						}
						
						IllegalError -> {
//							Toast.makeText(this, "$it", Toast.LENGTH_LONG).show()
						}
					}
					
					if (!BuildConfig.DEBUG) binding.buttonUpgrade.isEnabled = false
					binding.buttonUpgrade.text = "Upgrade"
				}
				
				is Either.Right -> {
					val price = it.value.oneTimePurchaseOfferDetails?.formattedPrice ?: "Unknown"
					
					binding.textPrice.text = price
					
					binding.buttonUpgrade.isEnabled = true
					binding.buttonUpgrade.text = "Upgrade · $price"
				}
			}
		}
		
		val resultFlow = if (BuildConfig.DEBUG) viewModel.mockState else viewModel.purchases
		
		resultFlow.zipWithLast().collectOn(this) { (last, it) ->
			when (it) {
				PurchaseResult.Success -> {
					
					val fromPending = last == PurchaseResult.Pending
					
					if (fromPending) {
						beginDelayedHideTransition()
						binding.textPurchasePending.isVisible = false
						
						delay(500)
						binding.konfettiView.updateLayoutParams {
							height = binding.thanksLayout.measuredHeight
						}
						binding.konfettiView.post {
							binding.konfettiView.start(rain())
						}
					}
					else {
						val willPlayTransition = beginDelayedTransition()
						
						binding.mainLayout.isVisible = false
						binding.receiptView.isVisible = false
						
						binding.textPurchasePending.isVisible = false
						binding.thanksLayout.isVisible = true
						
						binding.thanksLayout.post {
							binding.konfettiView.updateLayoutParams {
								height = binding.thanksLayout.measuredHeight
							}
						}
						
						if (willPlayTransition) delay(1000)
						
						binding.konfettiView.post {
							binding.konfettiView.start(rain())
						}
					}
				}
				
				PurchaseResult.Pending -> {
					
					beginDelayedTransition()
					
					binding.mainLayout.isVisible = false
					binding.receiptView.isVisible = false
					
					binding.textPurchasePending.isVisible = true
					binding.thanksLayout.isVisible = true
					
				}
				
				PurchaseResult.BillingError -> {
					Snackbar.make(binding.root, "Something went wrong", Snackbar.LENGTH_INDEFINITE)
						.setAnchorView(binding.buttonUpgrade)
						.show()
				}
				
				PurchaseResult.Other -> Unit
			}
		}
	}
	
	private fun beginDelayedTransition(): Boolean {
		val root = binding.root.parent as? ViewGroup
		if (root != null) TransitionManager.beginDelayedTransition(root, TransitionSet().apply {
			
			startDelay = 1000
			
			addTransition(TransitionSet().apply {
				addTransition(Fade(Fade.MODE_OUT).apply {
					duration = 200
				})
				addTransition(ChangeBounds().apply {
					duration = 600
					interpolator = easingAttr(Token.easing.standard)
				})
				ordering = TransitionSet.ORDERING_TOGETHER
			})
			addTransition(Fade(Fade.MODE_IN))
			ordering = TransitionSet.ORDERING_SEQUENTIAL
			
		})
		return root != null // will transition if true
	}
	
	private fun beginDelayedHideTransition() {
		val root = binding.root.parent as? ViewGroup
		if (root != null) TransitionManager.beginDelayedTransition(root, TransitionSet().apply {
			addTransition(Fade(Fade.OUT).apply {
				duration = 250
				interpolator = easingAttr(Token.easing.standard)
			})
			addTransition(Fade(Fade.IN).apply {
				duration = 250
				interpolator = easingAttr(Token.easing.standard)
			})
			addTransition(ChangeBounds().apply {
				duration = 500
				interpolator = easingAttr(Token.easing.standard)
			})
			ordering = TransitionSet.ORDERING_SEQUENTIAL
		})
	}
	
	private fun rain(): List<Party> {
		return listOf(
			Party(
				speed = 0f,
				maxSpeed = 15f,
				damping = 0.9f,
				angle = Angle.BOTTOM,
				spread = Spread.ROUND,
				colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
				emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(100),
				position = Position.Relative(0.0, 0.0).between(Position.Relative(1.0, 0.0))
			)
		)
	}
	
}