package moe.emi.finite

import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.transition.Slide
import androidx.transition.TransitionManager
import com.google.android.material.chip.Chip
import com.google.android.material.motion.MotionUtils
import convenience.resources.Token
import convenience.resources.colorAttr
import convenience.resources.durationAttr
import convenience.resources.easingAttr
import dev.chrisbanes.insetter.applyInsetter
import io.github.vshnv.adapt.dsl.ViewSource.SimpleViewSource
import io.github.vshnv.adapt.dsl.adapt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import moe.emi.finite.components.list.ui.DisplayOptionsSheet
import moe.emi.finite.components.settings.store.AppTheme
import moe.emi.finite.components.settings.store.appSettings
import moe.emi.finite.components.settings.ui.SettingsSheetFragment
import moe.emi.finite.components.upgrade.UpgradeSheet
import moe.emi.finite.databinding.ActivityMainBinding
import moe.emi.finite.dump.FastOutExtraSlowInInterpolator
import moe.emi.finite.dump.android.HasSnackbarAnchor
import moe.emi.finite.dump.android.Length
import moe.emi.finite.dump.collectOn
import moe.emi.finite.dump.fDp
import moe.emi.finite.dump.android.snackbar
import moe.emi.finite.ui.editor.SubscriptionEditorActivity
import java.util.Locale
import com.google.android.material.R as GR

class MainActivity : AppCompatActivity(), HasSnackbarAnchor {
	
	private val viewModel by viewModels<MainViewModel> { MainViewModel }
	private lateinit var binding: ActivityMainBinding
	
	val launcherEditor = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
	onResult@ {
		it.data?.getStringExtra("Message")?.let {
			binding.root.snackbar(it)
		}
	}
	
	private lateinit var navController: NavController
	
	private val filtersAdapter by lazy {
		adapt<String> {
			create { SimpleViewSource(Chip(it.context)) }
				.bind {
					binding.text = data.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
					
					binding.chipBackgroundColor = colorAttr(Token.color.surfaceBright).let { ColorStateList.valueOf(it) }
					colorAttr(Token.color.onSurface).let {
						binding.setTextColor(it)
						binding.closeIconTint = ColorStateList.valueOf(it)
					}
					
					binding.chipStrokeWidth = 0f
					
					binding.isCloseIconVisible = true
					binding.setCloseIconResource(R.drawable.ic_cancel_fill_24)
					binding.setOnCloseIconClickListener {
						viewModel.removeFilter(this.index)
					}
				}
			
			contentEquals { data, otherData ->
				data == otherData
			}
			
			itemEquals { data, otherData ->
				data == otherData
			}
		}
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		WindowCompat.setDecorFitsSystemWindows(window, false)
		super.onCreate(savedInstanceState)
		
		runBlocking {
			when (appSettings.first().appTheme) {
				AppTheme.Light ->
					AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
				AppTheme.Dark ->
					AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
				else -> Unit
			}
		}
		
		binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)
		binding.bottomAppBar.behavior
		
		binding.cardContainer.applyInsetter {
			type(navigationBars = true) {
				margin(bottom = true)
			}
		}
		
		binding.bottomAppBar.bringToFront()
		
		binding.bottomAppBar.addOnScrollStateChangedListener { bottomView, newState ->
			Log.d("TAG", "bottomView $bottomView, newState $newState")
			if (newState == 1) { // Hidden
				MotionUtils.resolveThemeDuration(this, GR.attr.motionDurationMedium4,400).let {
					lifecycleScope.launch {
						delay(it.toLong())
						runOnUiThread { bottomView.isInvisible = true }
					}
				}
			} else if (newState == 2) {
				bottomView.isInvisible = false
//				MotionUtils.resolveThemeDuration(this, GR.attr.motionDurationLong2, 500)
			}
		}
		
//		binding.bottomAppBar.performHide(false)
//		binding.bottomAppBar.fabCradleMargin = 16.fDp
//		binding.bottomAppBar.cradleVerticalOffset = 0.1f
		
		val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
		navController = navHostFragment.navController
		
		navController
			.addOnDestinationChangedListener { controller, destination, bundle ->
				
				Log.d("MainActivity", "destination $destination")
				
				if (destination.id != R.id.FirstFragment) {
					binding.bottomAppBar.performHide(true)
					binding.fab.hide()
					
					val bottom = window.decorView.rootWindowInsets
						?.getInsets(WindowInsetsCompat.Type.navigationBars())
						?.bottom
						?: 0
					
					binding.card.animate()
						.setDuration(durationAttr(Token.duration.medium4))
						.setInterpolator(easingAttr(Token.easing.emphasized))
						.translationY(
							binding.card.height +
									bottom.toFloat()
						)
						.start()
				}
				else {
					
					binding.bottomAppBar.performShow(true)
					binding.fab.show()
					
					binding.card.animate()
						.setDuration(durationAttr(Token.duration.long2))
						.setInterpolator(easingAttr(Token.easing.emphasized))
						.translationY(0f)
						.start()
				}
			}
		
		fun showUpgradeSheet() {
			UpgradeSheet.openWithCreateContext().show(supportFragmentManager, null)
		}
		
		binding.fab.setOnClickListener {
			lifecycleScope.launch {
				if (viewModel.isPro.first() || viewModel.subscriptionCount.first() < 5 || BuildConfig.DEBUG) {
					launcherEditor.launch(Intent(this@MainActivity, SubscriptionEditorActivity::class.java))
				}
				else showUpgradeSheet()
			}
		}
		
		binding.fab.setOnLongClickListener {
			if (BuildConfig.DEBUG) {
				showUpgradeSheet()
				true
			}
			else false
		}
		
		binding.bottomAppBar.setOnMenuItemClickListener {
			when (it.itemId) {
				R.id.action_view_options -> {
					DisplayOptionsSheet(this).show()
					true
				}
				R.id.action_settings -> {
					SettingsSheetFragment().show(supportFragmentManager, null)
					true
				}
				else -> false
			}
		}
		
		viewModel.tryUpdateRates()
		
		viewModel.messages.observe(this) { it ?: return@observe
			if (!it.consumed) when (it.key) {
				"Delete" -> lifecycleScope.launch { delay(500)
					binding.root.snackbar("Subscription deleted") }
			}
		}
		
		// TODO move to message bus
		viewModel.ratesUpdateState.filterNotNull().collectOn(this) {
			when (it) {
				Status.Loading -> binding.root.snackbar("Updating currency rates...")
				
				Status.Error -> binding.root.snackbar(
					"Error updating currency rates",
					"Retry",
					Length.Indefinite
				) { viewModel.tryUpdateRates() }
				
				Status.Success -> Unit
			}
		}
		
		binding.listFilters.adapter = filtersAdapter
		
		viewModel.selectedFilters
			.map { it.isNotEmpty() }
			.collectOn(this) {
				setFabShifted(it)
			}
		
		viewModel.selectedFilters.collectOn(this) { set ->
			Log.d("main", "set $set")
			filtersAdapter.submitData(set.toList())
		}
	}
	
	
	fun setBottomBarVisibility(visible: Boolean) {
		if (navController.currentDestination?.id != R.id.FirstFragment) return
		
		val slide = Slide(Gravity.BOTTOM).apply {
			addTarget(binding.cardContainer)
		}
		TransitionManager.beginDelayedTransition(binding.root, slide)
		
		if (visible) {
			binding.bottomAppBar.performShow(true)
			binding.cardContainer.isVisible = true
		}
		else {
			binding.bottomAppBar.performHide(true)
			TransitionManager.beginDelayedTransition(binding.root, slide)
			binding.cardContainer.isVisible = false
		}
	}
	
	
	var isShifted = false
	val normalOffset by lazy { binding.bottomAppBar.cradleVerticalOffset }
	
	fun setFabShifted(shifted: Boolean) {
		
		if (isShifted == shifted) return
		
		isShifted = shifted
		
		val animator = if (isShifted) ValueAnimator.ofFloat(0f, 100.fDp)
		else ValueAnimator.ofFloat(100.fDp, 0f)
		
		animator.apply {
			
			duration = 700
			interpolator = FastOutExtraSlowInInterpolator()
			
			addUpdateListener {
				val offset = it.animatedValue as Float
				val t = it.animatedFraction
				
				val raw = 3.fDp
				val elevation = if (!shifted) raw * t else raw - raw * t
				binding.bottomAppBar.cradleVerticalOffset = offset
				binding.bottomAppBar.elevation = elevation
				
				val height = binding.cardContainer.measuredHeight
				val trans = if (!shifted) height * t else height - height * t
				binding.cardContainer.translationY = trans
			}
			start()
		}
	}
	
	override val snackbarAnchor: View
		get() = binding.fab
}