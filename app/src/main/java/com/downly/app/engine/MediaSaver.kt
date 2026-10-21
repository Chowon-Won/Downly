package com.downly.app.engine

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File

/** Copia el archivo terminado a la galería (Movies/Downly) o a la música (Music/Downly). */
object MediaSaver {
    const val FOLDER = "Downly"

    data class Saved(val uri: Uri, val displayPath: String)

    fun save(context: Context, file: File, kind: MediaKind): Saved {
        val ext = file.extension.lowercase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: if (kind == MediaKind.AUDIO) "audio/mpeg" else "video/mp4"
        val baseDir = if (kind == MediaKind.AUDIO) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = if (kind == MediaKind.AUDIO)
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$baseDir/$FOLDER")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(collection, values) ?: error("No se pudo crear el archivo en el almacenamiento")
            resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                ?: error("No se pudo escribir el archivo")
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            file.delete()
            Saved(uri, "$baseDir/$FOLDER/${file.name}")
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(baseDir), FOLDER).apply { mkdirs() }
            val dest = uniqueFile(dir, file.name)
            file.copyTo(dest, overwrite = false)
            file.delete()
            MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), arrayOf(mime), null)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", dest)
            Saved(uri, "$baseDir/$FOLDER/${dest.name}")
        }
    }

    private fun uniqueFile(dir: File, name: String): File {
        var f = File(dir, name)
        var i = 1
        while (f.exists()) {
            f = File(dir, "${name.substringBeforeLast('.')} ($i).${name.substringAfterLast('.')}")
            i++
        }
        return f
    }
}
