package moe.emi.finite.ui.settings.backup

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import moe.emi.finite.R

class BackupViewModel(
	val context: Application
) : AndroidViewModel(context) {

	// Not making these private because i CBA. just don't modify them from the activity please :)
	val isLoadingState = MutableStateFlow(false)
	val messageBus = MutableStateFlow<MessageEvent?>(null)
	
	// This one can be modified though
	val appBackupState = MutableStateFlow<AppBackup?>(null)
	
	fun createBackup(fileUri: Uri) = viewModelScope.launch(Dispatchers.IO) {
		isLoadingState.emit(true)
		
		val result = runCatching {
			
			val input = createAppBackup().serialize().byteInputStream()
			val output = context.contentResolver.openOutputStream(fileUri) ?: error("Content resolver crashed.")
			
			input.use { i ->
				output.use { o ->
					i.copyTo(o)
				}
			}
		}
		result
			.onFailure { e ->
				e.printStackTrace()
				messageBus.update { MessageEvent(context.createBackupError(e)) }
			}
			.onSuccess {
				messageBus.update { MessageEvent(context.getString(R.string.snackbar_backup_success)) }
			}
		
		isLoadingState.emit(false)
	}
	
	@OptIn(ExperimentalSerializationApi::class)
	fun readBackup(fileUri: Uri) = viewModelScope.launch(Dispatchers.IO) {
		isLoadingState.emit(true)
		
		val result = runCatching {
			val input = context.contentResolver.openInputStream(fileUri) ?: error("Content resolver crashed.")
			val string = input.use { it.bufferedReader().use { reader -> reader.readText() } }
			jsonBackup.decodeFromString(AppBackup.serializer(), string)
		}
		
		result
			.onFailure { e ->
				e.printStackTrace()
				
				val message = when (e) {
					is MissingFieldException -> context.getString(R.string.snackbar_invalid_file)
					else -> context.restoreBackupError(e)
				}
				
				messageBus.update { MessageEvent(message) }
			}
			.onSuccess { appBackupState.emit(it) }
		
		isLoadingState.emit(false)
	}
	
	// TODO implement replace existing
	fun restoreBackup(backup: AppBackup, replaceExisting: Boolean) = viewModelScope.launch(Dispatchers.IO) {
		isLoadingState.emit(true)
		
		restoreAppBackup(backup)
			.onFailure { e ->
				e.printStackTrace()
				messageBus.update { MessageEvent(context.restoreBackupError(e)) }
			}
			.onSuccess {
				messageBus.update { MessageEvent(context.getString(R.string.snackbar_restore_success)) }
			}
		
		isLoadingState.emit(false)
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
	
}