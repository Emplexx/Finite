package moe.emi.finite

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.emi.finite.databinding.ActivityBackupBinding
import moe.emi.finite.dump.snackbar
import moe.emi.finite.service.repo.SubscriptionsRepo
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


class BackupActivity : AppCompatActivity() {
	
	private lateinit var binding: ActivityBackupBinding
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		binding = ActivityBackupBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		initLayout()
	}
	
	private fun initLayout() {
		binding.rowImport.textLabel.text = "Import"
		binding.rowExport.textLabel.text = "Export"
		
		binding.rowExport.root.setOnClickListener {
			saveFile()
		}
		binding.rowImport.root.setOnClickListener {
			openFile()
		}
	}
	
	val launcherExport = registerForActivityResult(ActivityResultContracts.CreateDocument("application/finite"))
	onResult@ {
		Log.d("TAG", "launcher $it")
		it ?: return@onResult
//		lifecycleScope.launch {
//			writeToFile(it)
//		}
		lifecycleScope.launch {
			writeDbToFile(it).collect {
				if (it == 1) binding.root.snackbar("Loading")
				if (it == 0) binding.root.snackbar("Export backup successful")
			}
		}
	}
	private fun saveFile() {
		launcherExport.launch("backup.finite")
	}
	
	private suspend fun writeToFile(uri: Uri) {
		
		withContext(Dispatchers.IO) {
			// == prepareAndGetDbInputStream
			FiniteApp.db.subscriptionDao()
				.checkpoint(SimpleSQLiteQuery("pragma wal_checkpoint(full)"))
			val input = FileInputStream(getDatabasePath("finite"))
			
			val output = contentResolver.openOutputStream(uri)!!
			
			// ==writeFrom
			val bufferSize = 8 * 1024
			val buffer = ByteArray(bufferSize)
			var bytesRead: Int
			while (input.read(buffer, 0, bufferSize).also { bytesRead = it } > 0) {
				output.write(buffer, 0, bytesRead)
			}
			
			output.flush()
			input.close()
			output.close()
		}
	}
	
	val launcher2 = registerForActivityResult(ActivityResultContracts.GetContent())
	onResult@ {
		it ?: return@onResult
		lifecycleScope.launch {
			readDbFromFile(it).collect {
				if (it == 1) binding.root.snackbar("Loading")
				if (it == 2) binding.root.snackbar("Error")
				if (it == 0) binding.root.snackbar("Import backup successful")
			}
		}
	}
	
	private fun openFile() {
		launcher2.launch("application/octet-stream")
	}
	
	private fun readFromFile(uri: Uri) {
		lifecycleScope.launch(Dispatchers.IO) {
			
			// TODO validate file from uri
			
			FiniteApp.db.close()
			
			backupDb()
			
			val input = contentResolver.openInputStream(uri) as FileInputStream
			val output = FileOutputStream(getDatabasePath("finite"))
		
			val fromChannel = input.channel
			val toChannel   = output.channel
			fromChannel.transferTo(0, fromChannel.size(), toChannel)
			fromChannel.close()
			toChannel.close()
			
			FiniteApp.instance.initDb()
			
			SubscriptionsRepo.getAllSubscriptions().first()
				.also { it.forEach {
					Log.d("TAG", "$it")
				} }
				
		}
	}
	
	private suspend fun backupDb() {
		withContext(Dispatchers.IO) {
			// == prepareAndGetDbInputStream
			val input = FileInputStream(getDatabasePath("finite"))
			
			val backupDir = File(filesDir, "backup")
			if (!backupDir.exists()) backupDir.mkdirs()
			val backupFilePath = backupDir.path + File.separator + "temp_backup"
			val backupFile = File(backupFilePath)
			if (backupFile.exists()) backupFile.delete()
			
			val output = FileOutputStream(backupFile)
			
			if (backupFile.createNewFile()) {
				
				// ==writeFrom
				val bufferSize = 8 * 1024
				val buffer = ByteArray(bufferSize)
				var bytesRead = bufferSize
				while (input.read(buffer, 0, bufferSize).also { bytesRead = it } > 0) {
					output.write(buffer, 0, bytesRead)
				}
				
				output.flush()
				input.close()
				output.close()
			}
		}
	}
}