package com.pcapps.launcher.manager

import android.content.ContentResolver
import android.net.Uri
import com.pcapps.launcher.model.BinaryArch
import com.pcapps.launcher.model.RuntimeKind
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Detects executable architecture by reading real header bytes
 * (PE COFF header for .exe/.dll, ELF header for Linux binaries).
 * This is not a filename/extension guess — it inspects the file.
 */
object AppDetector {

    private const val PE_SIGNATURE_OFFSET = 0x3C
    private const val PE_MACHINE_I386 = 0x014c
    private const val PE_MACHINE_AMD64 = 0x8664
    private const val PE_MACHINE_ARM64 = 0xAA64

    private val ELF_MAGIC = byteArrayOf(0x7F, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte())

    fun detect(resolver: ContentResolver, uri: Uri, fileName: String): DetectionResult {
        return resolver.openInputStream(uri)?.use { stream ->
            val header = readHeaderBytes(stream, 512)
            when {
                isPe(header) -> detectPe(header, fileName)
                isElf(header) -> detectElf(header, fileName)
                else -> DetectionResult(
                    BinaryArch.UNKNOWN,
                    RuntimeKind.UNSUPPORTED,
                    "File is not a recognized PE (.exe/.msi payload) or ELF binary."
                )
            }
        } ?: DetectionResult(BinaryArch.UNKNOWN, RuntimeKind.UNSUPPORTED, "Could not open file for inspection.")
    }

    private fun readHeaderBytes(stream: InputStream, max: Int): ByteArray {
        val buf = ByteArray(max)
        var read = 0
        while (read < max) {
            val n = stream.read(buf, read, max - read)
            if (n <= 0) break
            read += n
        }
        return buf.copyOf(read)
    }

    private fun isElf(header: ByteArray): Boolean =
        header.size >= 4 && header.copyOfRange(0, 4).contentEquals(ELF_MAGIC)

    private fun isPe(header: ByteArray): Boolean {
        if (header.size < 2 || header[0] != 'M'.code.toByte() || header[1] != 'Z'.code.toByte()) return false
        if (header.size < PE_SIGNATURE_OFFSET + 4) return false
        val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        val peOffset = bb.getInt(PE_SIGNATURE_OFFSET)
        if (peOffset <= 0 || peOffset + 6 > header.size) return false
        return header[peOffset] == 'P'.code.toByte() &&
            header[peOffset + 1] == 'E'.code.toByte() &&
            header[peOffset + 2] == 0.toByte() &&
            header[peOffset + 3] == 0.toByte()
    }

    private fun detectPe(header: ByteArray, fileName: String): DetectionResult {
        val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        val peOffset = bb.getInt(PE_SIGNATURE_OFFSET)
        val machine = bb.getShort(peOffset + 4).toInt() and 0xFFFF
        return when (machine) {
            PE_MACHINE_I386 -> DetectionResult(BinaryArch.X86, RuntimeKind.WINE_X86,
                "32-bit Windows PE. Will run under Wine + Box86 translation.")
            PE_MACHINE_AMD64 -> DetectionResult(BinaryArch.X64, RuntimeKind.WINE_X64,
                "64-bit Windows PE. Will run under Wine + Box64 translation.")
            PE_MACHINE_ARM64 -> DetectionResult(BinaryArch.ARM64, RuntimeKind.WINE_X64,
                "ARM64 Windows PE. Runs under Wine with native ARM64 execution (no CPU translation needed).")
            else -> DetectionResult(BinaryArch.UNKNOWN, RuntimeKind.UNSUPPORTED,
                "Unrecognized PE machine type (0x${machine.toString(16)}).")
        }
    }

    private fun detectElf(header: ByteArray, fileName: String): DetectionResult {
        if (header.size < 20) return DetectionResult(BinaryArch.UNKNOWN, RuntimeKind.UNSUPPORTED, "Truncated ELF header.")
        val eiClass = header[4].toInt()      // 1 = 32-bit, 2 = 64-bit
        val eiData = header[5].toInt()       // 1 = little endian
        val order = if (eiData == 1) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN
        val bb = ByteBuffer.wrap(header).order(order)
        val eMachine = bb.getShort(18).toInt() and 0xFFFF
        // EM_386=3, EM_X86_64=62, EM_ARM=40, EM_AARCH64=183
        return when (eMachine) {
            62 -> DetectionResult(BinaryArch.X64, RuntimeKind.WINE_X64,
                "Linux x86_64 ELF binary. Runs under Box64 translation (no Wine needed).")
            3 -> DetectionResult(BinaryArch.X86, RuntimeKind.WINE_X86,
                "Linux x86 ELF binary. Runs under Box86 translation.")
            183 -> DetectionResult(BinaryArch.ARM64, RuntimeKind.NATIVE_LINUX_ARM,
                "Native ARM64 Linux binary. Runs directly, no translation layer needed.")
            40 -> DetectionResult(BinaryArch.ARM32, RuntimeKind.NATIVE_LINUX_ARM,
                "Native ARM32 Linux binary. Runs directly via the ARM32 compatibility ABI.")
            else -> DetectionResult(BinaryArch.UNKNOWN, RuntimeKind.UNSUPPORTED,
                "Unrecognized ELF machine type ($eMachine).")
        }
    }
}

data class DetectionResult(
    val arch: BinaryArch,
    val runtimeKind: RuntimeKind,
    val message: String
)
