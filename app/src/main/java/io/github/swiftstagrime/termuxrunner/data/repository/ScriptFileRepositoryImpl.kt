package io.github.swiftstagrime.termuxrunner.data.repository

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptFileRepository
import java.io.File
import java.io.IOException
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
            val downloadDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

            val folderName = context.getString(R.string.bridge_folder_name)
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
    }
