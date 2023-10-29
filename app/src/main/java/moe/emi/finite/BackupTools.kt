package moe.emi.finite

import android.content.Context
import android.net.Uri
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import moe.emi.finite.service.repo.SubscriptionsRepo
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

private suspend fun Context.prepareAndGetDbInputStream(): FileInputStream {
	return withContext(Dispatchers.IO) {
		FiniteApp.db.subscriptionDao()
			.checkpoint(SimpleSQLiteQuery("pragma wal_checkpoint(full)"))
		
		FileInputStream(getDatabasePath("finite"))
	}
}

private val Context.tempBackupFile: File
	get() {
		val dir = File(filesDir, "backup").also { if (!it.exists()) it.mkdirs() }
		val file = File(dir.path + File.separator + "temp_backup")
		return file
	}

private fun OutputStream.writeFrom(inputStream: InputStream) {
	val bufferSize = 8 * 1024
	val buffer = ByteArray(bufferSize)
	var bytesRead: Int
	while (inputStream.read(buffer, 0, bufferSize).also { bytesRead = it } > 0) {
		this.write(buffer, 0, bytesRead)
	}
	this.flush()
	inputStream.close()
	this.close()
}

private fun FileInputStream.copyTo(outputStream: FileOutputStream) {
	val fromChannel = this.channel
	val toChannel   = outputStream.channel
	fromChannel.transferTo(0, fromChannel.size(), toChannel)
	fromChannel.close()
	toChannel.close()
}

//

private suspend fun Context.backupDb() {
	withContext(Dispatchers.IO) {
		val input = prepareAndGetDbInputStream()
		val output = FileOutputStream(tempBackupFile)
		if (tempBackupFile.createNewFile()) {
			output.writeFrom(input)
		}
	}
}

//

fun Context.writeDbToFile(uri: Uri) = flow<Int> {
	emit(1)
	withContext(Dispatchers.IO) {
		val input = prepareAndGetDbInputStream()
		val output = contentResolver.openOutputStream(uri)!! // TODO return error Int
		
		output.writeFrom(input)
		
	}
	emit(0)
}

fun Context.readDbFromFile(uri: Uri) = flow<Int> {
	emit(1)
	FiniteApp.db.close()
	
	backupDb()
	
	withContext(Dispatchers.IO) {
		val input = contentResolver.openInputStream(uri) as FileInputStream
		val output = FileOutputStream(getDatabasePath("finite"))
		input.copyTo(output)
		
		input.close()
		output.close()
	}
	
	FiniteApp.instance.destroyDb()
	FiniteApp.instance.initDb()
	
	SubscriptionsRepo.getAllSubscriptions().first()
		.also {
			if (it.isEmpty()) {
				val input = FileInputStream(tempBackupFile)
				val output = FileOutputStream(getDatabasePath("finite"))
				input.copyTo(output)
				
				
				emit(2)
			}
			else {
				tempBackupFile.also { if (it.exists()) it.delete() }
				emit(1)
			}
		}
}