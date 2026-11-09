package io.github.swiftstagrime.termuxrunner.data.repository

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptFileRepository
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Repo for saving script to bridge folder if they are close to possibly exceeding Intent
 */
class ScriptFileRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ScriptFileRepository {
        override fun saveToBridge(
            fileName: String,
            code: String,
        ): String {
            val folderName = context.getString(R.string.bridge_folder_name)

            // Issue #25: on Q+ this app's /Android/data folder is invisible to
            // Termux, so the bridge file goes to public Downloads via MediaStore.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cleanupLegacyBridgeDir(folderName)
                return saveToPublicDownloads(fileName, code, folderName)
            }

            val downloadDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val bridgeDir = File(downloadDir, folderName)

            if (!bridgeDir.exists()) {
                if (!bridgeDir.mkdirs()) {
                    throw IOException(context.getString(R.string.error_create_bridge_dir))
                }
            }

            val bridgeFile = File(bridgeDir, fileName)
            bridgeFile.writeText(code)

            return bridgeFile.absolutePath
        }

        @androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.Q)
        private fun saveToPublicDownloads(
            fileName: String,
            code: String,
            folderName: String,
        ): String {
            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/$folderName"

            val staleBeforeSeconds =
                TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) -
                    TimeUnit.DAYS.toSeconds(1)
            runCatching {
                resolver.delete(
                    collection,
                    "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND " +
                        "${MediaStore.MediaColumns.DATE_ADDED}<?",
                    arrayOf("$relativePath/", staleBeforeSeconds.toString()),
                )
            }

            val deterministicPath =
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "$folderName/$fileName",
                ).absolutePath
            resolver.delete(
                collection,
                "${MediaStore.MediaColumns.DATA}=?",
                arrayOf(deterministicPath),
            )

            val values =
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    // MIME_TYPE unset so MediaStore keeps .py/.js/.rb extensions
                    put(MediaStore.MediaColumns.OWNER_PACKAGE_NAME, context.packageName)
                }

            val uri =
                runCatching { resolver.insert(collection, values) }
                    .getOrElse {
                        // API 29 can reject a caller-provided owner
                        values.remove(MediaStore.MediaColumns.OWNER_PACKAGE_NAME)
                        resolver.insert(collection, values)
                    }
                    ?: throw IOException(context.getString(R.string.error_create_bridge_dir))

            resolver.openOutputStream(uri)?.use { it.write(code.toByteArray()) }
                ?: throw IOException(context.getString(R.string.error_create_bridge_dir))

            val dataCursor =
                resolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)
            dataCursor?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)?.let { return it }
                }
            }

            return deterministicPath
        }

        private fun cleanupLegacyBridgeDir(folderName: String) {
            val staleBefore = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
            runCatching {
                File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), folderName)
                    .listFiles()
                    ?.filter { it.lastModified() < staleBefore }
                    ?.forEach { it.delete() }
            }
        }
    }
