package moe.emi.finite

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.emi.finite.databinding.ActivityMainBinding
import moe.emi.finite.dump.HasSnackbarAnchor
import moe.emi.finite.dump.snackbar
import moe.emi.finite.ui.editor.SubscriptionEditorActivity
import moe.emi.finite.ui.home.DisplayOptionsSheet

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
	
	
	override fun onCreate(savedInstanceState: Bundle?) {
		WindowCompat.setDecorFitsSystemWindows(window, false)
		super.onCreate(savedInstanceState)
		
		binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
//		binding.bottomAppBar.performHide(false)
//		binding.bottomAppBar.fabCradleMargin = 16.fDp
//		binding.bottomAppBar.cradleVerticalOffset = 0.1f
		
		val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
		val navController = navHostFragment.navController
		
		navController
			.addOnDestinationChangedListener { controller, destination, bundle ->
				if (destination.id != R.id.FirstFragment) {
					setBottomBarVisibility(false)
					binding.fab.hide()
				}
				else {
					setBottomBarVisibility(true)
					binding.fab.show()
				}
			}
		
		
		
		binding.fab.setOnClickListener { view ->
			launcherEditor.launch(Intent(this, SubscriptionEditorActivity::class.java))
		}
		binding.bottomAppBar.setOnMenuItemClickListener {
			when (it.itemId) {
				R.id.action_show_upcoming -> {
//					lifecycleScope.launch {
//						val key = booleanPreferencesKey("ShowTimeLeft")
//						val value = storeGeneral.read(key, false).first()
//						storeGeneral.write(key, !value)
//					}
					
					DisplayOptionsSheet(this).show()
					
					true
				}
				R.id.action_settings -> {
					startActivity(Intent(this, SettingsActivity::class.java))
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