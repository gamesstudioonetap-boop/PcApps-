package com.pcapps.launcher.manager

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.pcapps.launcher.model.EnvironmentConfig
import com.pcapps.launcher.model.RuntimeKind
import com.pcapps.launcher.model.StorageMount
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Owns the on-disk lifecycle of environments (Wine prefixes / Linux rootfs
 * overlays). Storage lives under app-private external files, never in
 * shared/public storage, and never grants the environment access to any
 * Android folder the user didn't explicitly mount via StorageAccessManager.
 */
class EnvironmentManager(private val context: Context) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private val environmentsRoot: File
        get() = File(context.getExternalFilesDir(null), "Environments").apply { mkdirs() }

    fun listEnvironments(): List<EnvironmentConfig> =
        environmentsRoot.listFiles { f -> f.isDirectory }
            ?.mapNotNull { dir -> readConfig(dir) }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()

    fun createEnvironment(name: String, runtimeKind: RuntimeKind): EnvironmentConfig {
        val id = UUID.randomUUID().toString()
        val dir = File(environmentsRoot, id).apply { mkdirs() }
        File(dir, "prefix").mkdirs()
        File(dir, "shader_cache").mkdirs()

        val config = EnvironmentConfig(
            id = id,
            name = name,
            runtimeKind = runtimeKind,
            createdAt = System.currentTimeMillis(),
            storagePath = dir.absolutePath
        )
        writeConfig(dir, config)
        return config
    }

    fun duplicateEnvironment(sourceId: String, newName: String): EnvironmentConfig? {
        val sourceDir = File(environmentsRoot, sourceId)
        val sourceConfig = readConfig(sourceDir) ?: return null

        val newId = UUID.randomUUID().toString()
        val newDir = File(environmentsRoot, newId)
        sourceDir.copyRecursively(newDir, overwrite = true)

        val newConfig = sourceConfig.copy(
            id = newId,
            name = newName,
            createdAt = System.currentTimeMillis(),
            storagePath = newDir.absolutePath
        )
        writeConfig(newDir, newConfig)
        return newConfig
    }

    fun deleteEnvironment(id: String): Boolean {
        val dir = File(environmentsRoot, id)
        return dir.exists() && dir.deleteRecursively()
    }

    fun addMount(id: String, mount: StorageMount): EnvironmentConfig? {
        val dir = File(environmentsRoot, id)
        val config = readConfig(dir) ?: return null
        val updated = config.copy(mounts = config.mounts + mount)
        writeConfig(dir, updated)
        return updated
    }

    /** Exports an environment as a single .zip the user can back up or share via SAF. */
    fun exportEnvironment(id: String, destination: File) {
        val dir = File(environmentsRoot, id)
        require(dir.exists()) { "Environment $id does not exist" }
        ZipOutputStream(destination.outputStream()).use { zos ->
            dir.walkTopDown().filter { it.isFile }.forEach { file ->
                val entryName = dir.toURI().relativize(file.toURI()).path
                zos.putNextEntry(ZipEntry(entryName))
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    /** Imports a previously exported environment archive, assigning it a fresh id. */
    fun importEnvironment(source: File): EnvironmentConfig {
        val newId = UUID.randomUUID().toString()
        val newDir = File(environmentsRoot, newId).apply { mkdirs() }

        ZipInputStream(source.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(newDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { zis.copyTo(it) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val imported = readConfig(newDir)
            ?: throw IllegalStateException("Imported archive missing config.json")
        val relocated = imported.copy(id = newId, storagePath = newDir.absolutePath)
        writeConfig(newDir, relocated)
        return relocated
    }

    private fun readConfig(dir: File): EnvironmentConfig? {
        val configFile = File(dir, "config.json")
        if (!configFile.exists()) return null
        return runCatching { gson.fromJson(configFile.readText(), EnvironmentConfig::class.java) }.getOrNull()
    }

    private fun writeConfig(dir: File, config: EnvironmentConfig) {
        File(dir, "config.json").writeText(gson.toJson(config))
    }
}
