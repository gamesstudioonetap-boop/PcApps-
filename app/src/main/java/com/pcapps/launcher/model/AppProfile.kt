package com.pcapps.launcher.model

/** CPU architecture detected from the binary's header. */
enum class BinaryArch { X86, X64, ARM32, ARM64, UNKNOWN }

/** Which compatibility layer this binary needs. */
enum class RuntimeKind { WINE_X86, WINE_X64, NATIVE_LINUX_ARM, UNSUPPORTED }

/** Graphics backend selected for a given app/environment. */
enum class GraphicsBackend { VULKAN, GLES, SOFTWARE }

enum class MouseMode { TOUCH_DIRECT, VIRTUAL_CURSOR }

data class GraphicsPreset(
    val backend: GraphicsBackend = GraphicsBackend.VULKAN,
    val resolutionWidth: Int = 1280,
    val resolutionHeight: Int = 720,
    val fpsLimit: Int = 60,
    val vsync: Boolean = true,
    val renderScale: Float = 1.0f,
    val textureQuality: String = "Balanced" // Performance | Balanced | Quality
)

/**
 * Persisted, editable configuration for one launchable application.
 * This is the unit shown on the "Application Detail" screen and stored
 * under an Environment.
 */
data class AppProfile(
    val id: String,                    // stable UUID
    val displayName: String,
    val sourceUri: String,             // content:// URI from SAF, original picked file
    val fileName: String,
    val arch: BinaryArch,
    val runtimeKind: RuntimeKind,
    val environmentId: String,         // which container/prefix this app lives in
    val graphics: GraphicsPreset = GraphicsPreset(),
    val mouseMode: MouseMode = MouseMode.VIRTUAL_CURSOR,
    val ramAllocationMb: Int? = null,  // null = Auto
    val fullscreen: Boolean = true,
    val orientationLocked: String? = null, // "portrait" | "landscape" | null = auto
    val lastLaunchedAt: Long? = null,
    val estimatedStorageMb: Long = 0L
)

/** Result of the pre-launch compatibility check, shown to the user before Launch. */
data class CompatibilityReport(
    val arch: BinaryArch,
    val runtimeKind: RuntimeKind,
    val supported: Boolean,
    val warnings: List<String> = emptyList(),
    val blockingIssues: List<String> = emptyList()
)
