package moe.emi.finite

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.motion.MotionUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import moe.emi.finite.databinding.ActivityMainBinding
import moe.emi.finite.dump.HasSnackbarAnchor
import moe.emi.finite.dump.invisible
import moe.emi.finite.dump.snackbar
import moe.emi.finite.service.datastore.AppTheme
import moe.emi.finite.service.datastore.appSettings
import moe.emi.finite.ui.editor.SubscriptionEditorActivity
import moe.emi.finite.ui.home.DisplayOptionsSheet
import moe.emi.finite.ui.settings.SettingsSheetFragment
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
		
		binding.bottomAppBar.addOnScrollStateChangedListener { bottomView, newState ->
			Log.d("TAG", "bottomView $bottomView, newState $newState")
			if (newState == 1) { // Hidden
				MotionUtils.resolveThemeDuration(this, GR.attr.motionDurationMedium4,400).let {
					lifecycleScope.launch {
						delay(it.toLong())
						runOnUiThread { bottomView.invisible = true }
					}
				}
			} else if (newState == 2) {
				bottomView.invisible = false
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
		
		viewModel.messages.observe(this) { it ?: return@observe
			if (!it.consumed) when (it.key) {
				"Delete" -> lifecycleScope.launch { delay(500)
					binding.root.snackbar("Subscription deleted") }
			}
		}
	}
	
	
	fun setBottomBarVisibility(visible: Boolean) {
		if (navController.currentDestination?.id != R.id.FirstFragment) return
		if (visible) binding.bottomAppBar.performShow(true)
		else binding.bottomAppBar.performHide(true)
	}
	
	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		// Inflate the menu; this adds items to the action bar if it is present.
		menuInflater.inflate(R.menu.menu_main, menu)
		return true
	}
	
	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		return when (item.itemId) {
			R.id.action_settings -> true
			else -> super.onOptionsItemSelected(item)
		}
	}
	
	override val snackbarAnchor: View
		get() = binding.fab
}