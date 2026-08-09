package com.pcapps.launcher.model

/**
 * An isolated container/prefix that one or more AppProfiles run inside.
 * Maps roughly to a Wine prefix (for Windows apps) or a chroot-style
 * Linux userspace (for native Linux ARM binaries).
 *
 * Storage layout on device (created under app-private external storage,
 * NOT arbitrary Android storage):
 *   /Environments/<id>/prefix/       -- Wine C: drive or Linux rootfs overlay
 *   /Environments/<id>/config.json   -- this object, serialized
 *   /Environments/<id>/shader_cache/
 *   /Environments/<id>/mounts.json   -- Android folder -> in-env path mapping
 */
data class EnvironmentConfig(
    val id: String,
    val name: String,                     // e.g. "Blender", "Games"
    val runtimeKind: RuntimeKind,
    val createdAt: Long,
    val storagePath: String,              // absolute path to /Environments/<id>/
    val mounts: List<StorageMount> = emptyList(),
    val installedDependencies: List<String> = emptyList(), // e.g. "vcredist2019", "dotnet48"
    val notes: String = ""
)

/**
 * A single Android storage location the user explicitly granted access to,
 * exposed inside the environment at a fixed mount point.
 * androidUri is a SAF content:// tree the user picked; nothing broader is ever exposed.
 */
data class StorageMount(
    val androidUri: String,       // content://... tree uri from SAF
    val mountPoint: String,       // e.g. "/documents", "/app"
    val readOnly: Boolean = false
)
