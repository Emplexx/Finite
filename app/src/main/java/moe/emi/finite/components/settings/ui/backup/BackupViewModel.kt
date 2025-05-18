package moe.emi.finite.components.settings.ui.backup

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import moe.emi.finite.R
import moe.emi.finite.components.upgrade.cache.UpgradeState
import moe.emi.finite.core.alarms.ReminderScheduler
import moe.emi.finite.di.VMFactory
import moe.emi.finite.di.app
import moe.emi.finite.di.container
import moe.emi.finite.di.singleViewModel
import moe.emi.finite.dump.with
import moe.emi.finite.core.db.FiniteDB

class BackupViewModel(
	private val context: Application,
	private val db: FiniteDB,
	private val reminderScheduler: ReminderScheduler,
	val upgradeState: Flow<UpgradeState>
) : ViewModel() {

	private val _isLoadingState = MutableStateFlow(false)
	val isLoadingState = _isLoadingState.asStateFlow()
	
	private val _messageBus = MutableStateFlow<MessageEvent?>(null)
	val messageBus = _messageBus.asStateFlow()
	
	private val _appBackupState = MutableStateFlow<AppBackup?>(null)
	val appBackupState = _appBackupState.asStateFlow()
	
	
	fun createBackup(fileUri: Uri) = viewModelScope.launch {
		_isLoadingState.emit(true)
		
		with(db, context) {
			val backup = createAppBackup()
			writeBackupFile(fileUri, backup)
		}
			.onFailure { e ->
				e.printStackTrace()
				_messageBus.update { MessageEvent(context.createBackupError(e)) }
			}
			.onSuccess {
				_messageBus.update { MessageEvent(context.getString(R.string.snackbar_backup_success)) }
			}
		
		_isLoadingState.emit(false)
	}
	
	@OptIn(ExperimentalSerializationApi::class)
	fun readBackup(fileUri: Uri) = viewModelScope.launch {
		_isLoadingState.emit(true)
		
		with(context) { readBackupFile(fileUri) }
			.onFailure { e ->
				e.printStackTrace()
				
				_messageBus.update {
					val message = when (e) {
						is MissingFieldException -> context.getString(R.string.snackbar_invalid_file)
						else -> context.restoreBackupError(e)
					}
					MessageEvent(message)
				}
			}
			.onSuccess { _appBackupState.emit(it) }
		
		_isLoadingState.emit(false)
	}
	
	fun restoreBackup(backup: AppBackup, replaceExisting: Boolean) = viewModelScope.launch {
		_isLoadingState.emit(true)
		
		with(db, reminderScheduler) { restoreAppBackup(backup, replaceExisting) }
			.onFailure { e ->
				e.printStackTrace()
				_messageBus.update { MessageEvent(context.restoreBackupError(e)) }
			}
			.onSuccess {
				_messageBus.update { MessageEvent(context.getString(R.string.snackbar_restore_success)) }
			}
		
		_isLoadingState.emit(false)
	}
	
	fun clearInMemoryBackup() {
		_appBackupState.update { null }
	}
	
	private fun Context.createBackupError(e: Throwable): String {
		return getString(
			R.string.snackbar_backup_failure,
			"${e.javaClass.name} - ${e.message ?: e}"
		)
	}
	
	private fun Context.restoreBackupError(e: Throwable): String {
		return getString(
			R.string.snackbar_restore_failure,
			"${e.javaClass.name} - ${e.message ?: e}"
		)
	}
	
	companion object : VMFactory by singleViewModel({
		BackupViewModel(app, container.db, container.reminderScheduler, container.upgradeState)
	})
	
}