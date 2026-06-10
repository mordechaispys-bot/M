package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class VaultRepository(private val vaultDao: VaultDao) {

    val allItems: Flow<List<VaultItem>> = vaultDao.getAllItems()

    fun getItemsByType(type: String): Flow<List<VaultItem>> {
        return vaultDao.getItemsByType(type)
    }

    /**
     * Copy the file from the external Uri to internal secure storage,
     * and save metadata to Room.
     * After successful copy, we try to delete the source file so it disappears from the gallery.
     */
    suspend fun importMedia(context: Context, uri: Uri, isVideo: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            
            // Get original file info
            var originalName = "media_file"
            var size = 0L
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) originalName = cursor.getString(nameIndex)
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }

            // Create target folders in private storage
            val mediaDir = File(context.filesDir, "vault_media")
            if (!mediaDir.exists()) {
                mediaDir.mkdirs()
            }

            // Generate a secure unique local file name
            val fileExtension = if (isVideo) "mp4" else "jpg"
            val uniqueFileName = "vault_${UUID.randomUUID()}.$fileExtension"
            val destinationFile = File(mediaDir, uniqueFileName)

            // Open and copy streams
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // Get actual file size if openInputStream reported 0
            val finalSize = if (size > 0) size else destinationFile.length()

            // Save metadata to Room database
            val vaultItem = VaultItem(
                fileName = uniqueFileName,
                fileType = if (isVideo) "VIDEO" else "PHOTO",
                originalName = originalName,
                size = finalSize
            )
            vaultDao.insertItem(vaultItem)

            // Attempt to delete original file (API 30+ has restriction where apps can only delete their own files or prompt user.
            // But we can try to call contentResolver.delete)
            try {
                contentResolver.delete(uri, null, null)
            } catch (e: Exception) {
                Log.e("VaultRepository", "Could not delete original file via ContentResolver (this is expected on Android 10+): ${e.message}")
            }

            true
        } catch (e: Exception) {
            Log.e("VaultRepository", "Error importing media: ${e.message}", e)
            false
        }
    }

    /**
     * Delete the media item from both Room database and internal file storage.
     */
    suspend fun deleteMedia(context: Context, item: VaultItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val mediaDir = File(context.filesDir, "vault_media")
            val file = File(mediaDir, item.fileName)
            if (file.exists()) {
                file.delete()
            }
            vaultDao.deleteItem(item)
            true
        } catch (e: Exception) {
            Log.e("VaultRepository", "Error deleting media: ${e.message}", e)
            false
        }
    }

    /**
     * Helper to get the absolute File object for a VaultItem
     */
    fun getFileForItem(context: Context, item: VaultItem): File {
        val mediaDir = File(context.filesDir, "vault_media")
        return File(mediaDir, item.fileName)
    }
}
