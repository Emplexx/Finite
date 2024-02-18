package moe.emi.finite.ui.settings.backup

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isGone
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import moe.emi.finite.R
import moe.emi.finite.databinding.ActivityBackupBinding
import moe.emi.finite.dump.collectOn
import moe.emi.finite.dump.snackbar
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class BackupActivity : AppCompatActivity() {
	
	private val viewModel by viewModels<BackupViewModel>()
	private lateinit var binding: ActivityBackupBinding
	
	private val launcherExport = registerForActivityResult(CreateDocument("application/json"))
	onResult@ { viewModel.createBackup(it ?: return@onResult) }
	
	private val launcherImport = registerForActivityResult(GetContent())
	onResult@ { viewModel.readBackup(it ?: return@onResult) }
	
	override fun onCreate(savedInstanceState: Bundle?) {
		WindowCompat.setDecorFitsSystemWindows(window, false)
		super.onCreate(savedInstanceState)
		
		binding = ActivityBackupBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		binding.scrollView.applyInsetter {
			type(navigationBars = true) {
				padding(bottom = true)
			}
		}
		
		initLayout()
		collectFlow()
	}
	
	private fun initLayout() {
		
		// Hide cloud backup stuff because it's not implemented yet
		listOf(
			binding.headerCloud,
			binding.rowCloudCreate,
			binding.rowCloudRestore,
			binding.headerManual
		)
			.onEach { it.root.isGone = true }
		
		binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
		
		binding.headerCloud.text.text = getString(R.string.setting_backup_cloud_title)
		binding.rowCloudCreate.apply {
			icon.setImageResource(R.drawable.ic_cloud_upload_fill_24)
			textLabel.text = getString(R.string.setting_create_backup_cloud)
		}
		binding.rowCloudRestore.apply {
			icon.setImageResource(R.drawable.ic_cloud_download_fill_24)
			textLabel.text = getString(R.string.setting_restore_backup_cloud)
		}
		
		binding.headerManual.text.text = getString(R.string.setting_backup_local_title)
		binding.rowExport.apply {
			icon.setImageResource(R.drawable.ic_download_24)
			textLabel.text = getString(R.string.setting_create_backup_local)
		}
		binding.rowImport.apply {
			icon.setImageResource(R.drawable.ic_backup_restore_24)
			textLabel.text = getString(R.string.setting_restore_backup_local)
		}
		
		
		binding.rowExport.root.setOnClickListener {
			val date = java.time.LocalDate.now()
			val name = "finite-backup-${date.year}-${date.month.value}-${date.dayOfMonth}.json"
			launcherExport.launch(name)
		}
		binding.rowImport.root.setOnClickListener {
			launcherImport.launch("application/json")
		}
	}
	
	private fun collectFlow() {
		
		viewModel.isLoadingState.collectOn(this) {
			if (it) binding.progressLinear.show()
			else binding.progressLinear.hide()
		}
		
		viewModel.appBackupState
			.filterNotNull()
			.collectOn(this) {
				showRestoreConfirmationDialog(
					it,
					onConfirmed = { replaceExisting ->
						viewModel.restoreBackup(it, replaceExisting)
					}
				)
			}
		
		
		viewModel.messageBus
			.filterNotNull()
			.filter { !it.consumed }
			.collectOn(this) {
				it.consume()
				binding.root.snackbar(it.message)
			}
		
		return
	}
	
	private fun showRestoreConfirmationDialog(backup: AppBackup, onConfirmed: (Boolean) -> Unit) {
		MaterialAlertDialogBuilder(this)
			.setTitle(R.string.dialog_restore_title)
			.setMessage(buildString {
				
				val timestamp = Instant.ofEpochMilli(backup.createdAt)
				val timestampZoned = ZonedDateTime.ofInstant(timestamp, ZoneId.systemDefault())
				val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
				val date = timestampZoned.format(formatter)
				
				append(getString(R.string.dialog_restore_created_on, date))
			})
			.setPositiveButton(R.string.action_restore) { _, _ ->
				onConfirmed(true)
			}
			.setNegativeButton(R.string.action_cancel, null)
			.setOnDismissListener {
				viewModel.appBackupState.update { null }
			}
			.show()
	}
}