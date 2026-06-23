package com.example.features.home

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Context.createImageFile(): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_" + timeStamp + "_"
    val storageDir = File(filesDir, "inventory_images")
    if (!storageDir.exists()) {
        storageDir.mkdirs()
    }
    return File.createTempFile(
        imageFileName, /* prefix */
        ".jpg", /* suffix */
        storageDir /* directory */
    )
}

fun Context.getUriForFile(file: File): Uri {
    return FileProvider.getUriForFile(
        this,
        "${packageName}.fileprovider",
        file
    )
}

fun Context.copyUriToInternalStorage(uri: Uri): Uri? {
    return try {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "IMG_$timeStamp.jpg"
        val storageDir = File(filesDir, "inventory_images")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        val file = File(storageDir, fileName)
        val outputStream = FileOutputStream(file)
        
        inputStream?.copyTo(outputStream)
        
        inputStream?.close()
        outputStream.close()
        
        Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
