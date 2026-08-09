package com.pcapps.launcher.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.documentfile.provider.DocumentFile

/**
 * Thin wrapper around Android's Storage Access Framework.
 *
 * Deliberately does NOT request MANAGE_EXTERNAL_STORAGE or any broad
 * filesystem permission. Every folder the environment can see is one the
 * user picked through the system file/folder picker, and access is
 * persisted only for that specific tree via a persistable URI permission.
 */
class StorageAccessManager(private val activity: ComponentActivity) {

    /** Registers the folder-picker launcher. Call once, e.g. in onCreate. */
    fun registerFolderPicker(onPicked: (Uri) -> Unit): ActivityResultLauncher<Uri?> =
        activity.registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                activity.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                onPicked(uri)
            }
        }

    /** Registers the single-file picker launcher for selecting an .exe/.msi/etc. */
    fun registerFilePicker(onPicked: (Uri) -> Unit): ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                activity.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                onPicked(uri)
            }
        }

    fun listPersistedGrants(): List<Uri> =
        activity.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission }
            .map { it.uri }

    fun revokeGrant(uri: Uri) {
        activity.contentResolver.releasePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }

    fun displayNameFor(uri: Uri): String =
        DocumentFile.fromTreeUri(activity, uri)?.name
            ?: DocumentFile.fromSingleUri(activity, uri)?.name
            ?: uri.lastPathSegment
            ?: uri.toString()
}
