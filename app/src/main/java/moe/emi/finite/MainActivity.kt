package moe.emi.finite

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.core.view.isInvisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.motion.MotionUtils
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import moe.emi.finite.databinding.ActivityMainBinding
import moe.emi.finite.dump.FastOutExtraSlowInInterpolator
import moe.emi.finite.dump.HasSnackbarAnchor
import moe.emi.finite.dump.Length
import moe.emi.finite.dump.collectOn
import moe.emi.finite.dump.fDp
import moe.emi.finite.dump.snackbar
import moe.emi.finite.service.datastore.AppTheme
import moe.emi.finite.service.datastore.appSettings
import moe.emi.finite.ui.editor.SubscriptionEditorActivity
import moe.emi.finite.ui.home.DisplayOptionsSheet
import moe.emi.finite.ui.settings.SettingsSheetFragment
import moe.emi.finite.ui.settings.backup.Status
import com.google.android.material.R as GR

@AndroidEntryPoint
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
				}
				else {
					binding.bottomAppBar.performShow(true)
					binding.fab.show()
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
	}
	
	
	fun setBottomBarVisibility(visible: Boolean) {
		if (navController.currentDestination?.id != R.id.FirstFragment) return
		if (visible) binding.bottomAppBar.performShow(true)
		else binding.bottomAppBar.performHide(true)
	}
	
	
	var isShifted = false
	val normalOffset by lazy { binding.bottomAppBar.cradleVerticalOffset }
	fun setFabShifted(shifted: Boolean) {
		isShifted = shifted
		
		val animator = if (isShifted) ValueAnimator.ofFloat(0f, 100.fDp)
		else  ValueAnimator.ofFloat(100.fDp, 0f)
		
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