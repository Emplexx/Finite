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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.transition.Slide
import androidx.transition.TransitionManager
import com.google.android.material.chip.Chip
import com.google.android.material.motion.MotionUtils
import dev.chrisbanes.insetter.applyInsetter
import io.github.vshnv.adapt.dsl.ViewSource.SimpleViewSource
import io.github.vshnv.adapt.dsl.adapt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import moe.emi.convenience.Duration
import moe.emi.convenience.Interpolator
import moe.emi.convenience.TonalColor
import moe.emi.convenience.materialColor
import moe.emi.convenience.materialDuration
import moe.emi.convenience.materialInterpolator
import moe.emi.finite.databinding.ActivityMainBinding
import moe.emi.finite.dump.FastOutExtraSlowInInterpolator
import moe.emi.finite.dump.HasSnackbarAnchor
import moe.emi.finite.dump.Length
import moe.emi.finite.dump.collectOn
import moe.emi.finite.dump.fDp
import moe.emi.finite.dump.snackbar
import moe.emi.finite.dump.visible
import moe.emi.finite.service.datastore.AppTheme
import moe.emi.finite.service.datastore.appSettings
import moe.emi.finite.ui.editor.SubscriptionEditorActivity
import moe.emi.finite.ui.home.DisplayOptionsSheet
import moe.emi.finite.ui.settings.SettingsSheetFragment
import moe.emi.finite.ui.settings.backup.Status
import java.util.Locale
import com.google.android.material.R as GR

class MainActivity : AppCompatActivity(), HasSnackbarAnchor {
	
	private val viewModel by viewModels<MainViewModel>()
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
					
					binding.chipBackgroundColor = materialColor(
						com.google.android.material.R.attr.colorSurfaceBright
					).let { ColorStateList.valueOf(it) }
					materialColor(TonalColor.onSurface).let {
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
				if (destination.id != R.id.FirstFragment) {
					binding.bottomAppBar.performHide(true)
					binding.fab.hide()
					
					binding.card.animate()
						.setDuration(materialDuration(Duration.medium4))
						.setInterpolator(materialInterpolator(Interpolator.emphasized))
						.translationY(
							binding.card.height +
									window.decorView.rootWindowInsets
										.getInsets(WindowInsetsCompat.Type.navigationBars())
										.bottom
										.toFloat()
						)
						.start()
				}
				else {
					
					binding.bottomAppBar.performShow(true)
					binding.fab.show()
					
					binding.card.animate()
						.setDuration(materialDuration(Duration.long2))
						.setInterpolator(materialInterpolator(Interpolator.emphasized))
						.translationY(0f)
						.start()
				}
			}
		
		
		
		binding.fab.setOnClickListener { view ->
			launcherEditor.launch(Intent(this, SubscriptionEditorActivity::class.java))
		}
		binding.bottomAppBar.setOnMenuItemClickListener {
			when (it.itemId) {
				R.id.action_view_options -> {
					DisplayOptionsSheet(this).show()
					true
				}
				R.id.action_settings -> {
//					startActivity(Intent(this, SettingsActivity::class.java))
//					SettingsSheet(this).show()
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
			binding.cardContainer.visible = true
		}
		else {
			binding.bottomAppBar.performHide(true)
			TransitionManager.beginDelayedTransition(binding.root, slide)
			binding.cardContainer.visible = false
		}
	}
	
	
	var isShifted = false
	val normalOffset by lazy { binding.bottomAppBar.cradleVerticalOffset }
	fun setFabShifted(shifted: Boolean) {
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
	
//	override fun onCreateOptionsMenu(menu: Menu): Boolean {
//		// Inflate the menu; this adds items to the action bar if it is present.
//		menuInflater.inflate(R.menu.menu_main, menu)
//		return true
//	}
//
//	override fun onOptionsItemSelected(item: MenuItem): Boolean {
//		// Handle action bar item clicks here. The action bar will
//		// automatically handle clicks on the Home/Up button, so long
//		// as you specify a parent activity in AndroidManifest.xml.
//		return when (item.itemId) {
//			R.id.action_settings -> true
//			else -> super.onOptionsItemSelected(item)
//		}
//	}
	
	override val snackbarAnchor: View
		get() = binding.fab
}