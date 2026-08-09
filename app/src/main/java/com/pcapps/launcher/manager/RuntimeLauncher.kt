package com.pcapps.launcher.manager

import com.pcapps.launcher.model.AppProfile
import com.pcapps.launcher.model.EnvironmentConfig

/**
 * NOTE ON SCOPE:
 * This interface defines the contract between the Android app layer and the
 * native compatibility runtime (Wine + Box86/Box64 + graphics translation).
 * It intentionally has no working implementation in this stage.
 *
 * Actually executing Windows/Linux binaries requires a prebuilt native
 * runtime — an ARM64 build of Wine, Box86/Box64's JIT, and a Vulkan/GLES
 * translation layer (Turnip/DXVK or similar) — compiled with NDK toolchains
 * outside a single source file. There is no shortcut that makes a real PE/ELF
 * binary execute without that native runtime present.
 *
 * The pragmatic path to a working RuntimeLauncher implementation is to
 * integrate an existing open-source runtime (e.g. the Winlator project's
 * native libraries) behind this interface, rather than reimplementing Wine
 * and a CPU translator from scratch. Everything above this interface
 * (UI, file picker, environment manager, profiles, diagnostics) is real,
 * runnable code and does not depend on which runtime backs it.
 */
interface RuntimeLauncher {

    /** True once the native runtime for [environment.runtimeKind] is present on device. */
    fun isRuntimeInstalled(environment: EnvironmentConfig): Boolean

    /**
     * Downloads/unpacks the runtime components needed for this environment kind
     * (e.g. Wine build + Box64 binary + graphics driver shim).
     * Reports progress via [onProgress] (0..100).
     */
    suspend fun installRuntime(environment: EnvironmentConfig, onProgress: (Int) -> Unit)

    /**
     * Launches [profile] inside [environment]. Returns a handle the UI can use
     * to track process state, or throws a [RuntimeLaunchException] with a
     * diagnosable reason (see AppDetector's CompatibilityReport for the
     * pre-flight version of these same checks).
     */
    suspend fun launch(profile: AppProfile, environment: EnvironmentConfig): RunningAppHandle

    fun terminate(handle: RunningAppHandle)
}

data class RunningAppHandle(
    val processId: Int,
    val profileId: String,
    val startedAt: Long
)

class RuntimeLaunchException(message: String, val diagnosticLog: String? = null) : Exception(message)

/**
 * Stub used until a real native runtime is integrated. Every call fails
 * loudly with a clear reason rather than silently pretending to work.
 */
class UnimplementedRuntimeLauncher : RuntimeLauncher {
    override fun isRuntimeInstalled(environment: EnvironmentConfig) = false

    override suspend fun installRuntime(environment: EnvironmentConfig, onProgress: (Int) -> Unit) {
        throw RuntimeLaunchException(
            "No native runtime is integrated yet for ${environment.runtimeKind}. " +
                "See RuntimeLauncher.kt for integration notes."
        )
    }

    override suspend fun launch(profile: AppProfile, environment: EnvironmentConfig): RunningAppHandle {
        throw RuntimeLaunchException(
            "Cannot launch '${profile.displayName}': native runtime not yet integrated."
        )
    }

    override fun terminate(handle: RunningAppHandle) {
        // no-op: nothing is actually running
    }
}
