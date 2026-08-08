package com.audiolab.app.utils

import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FFmpegHelper {

    private const val TAG = "FFmpegHelper"

    data class FFmpegResult(
        val success: Boolean,
        val message: String = ""
    )

    suspend fun execute(command: String): FFmpegResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Executing: ffmpeg $command")
        val session = FFmpegKit.execute(command)
        val returnCode = session.returnCode

        if (ReturnCode.isSuccess(returnCode)) {
            Log.d(TAG, "Command succeeded")
            FFmpegResult(true, "Muvaffaqiyatli!")
        } else {
            val error = session.allLogsAsString ?: "Noma'lum xatolik"
            Log.e(TAG, "Command failed: $error")
            FFmpegResult(false, error)
        }
    }

    suspend fun getDuration(filePath: String): Double = withContext(Dispatchers.IO) {
        try {
            val session = FFprobeKit.execute(
                "-v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 \"$filePath\""
            )
            val output = session.output?.trim()
            output?.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting duration: ${e.message}")
            0.0
        }
    }

    // Audio kesish - boshlanish va tugash vaqti bo'yicha
    fun buildTrimCommand(inputPath: String, outputPath: String, startSec: Double, endSec: Double): String {
        return "-y -i \"$inputPath\" -ss $startSec -to $endSec -c:a copy \"$outputPath\""
    }

    // Ketma-ket birlashtirish (concat)
    fun buildConcatCommand(listFilePath: String, outputPath: String): String {
        return "-y -f concat -safe 0 -i \"$listFilePath\" -c copy \"$outputPath\""
    }

    // Ustma-ust qo'yish (mix/overlay)
    fun buildMixCommand(inputPaths: List<String>, outputPath: String): String {
        val inputs = inputPaths.joinToString(" ") { "-i \"$it\"" }
        val filterComplex = "amix=inputs=${inputPaths.size}:duration=longest:dropout_transition=0"
        return "-y $inputs -filter_complex \"$filterComplex\" \"$outputPath\""
    }

    // Echo effekti
    // aecho=in_gain:out_gain:delays:decays
    // delays va decays ms da, masalan 1000ms delay, 0.5 decay
    fun buildEchoCommand(inputPath: String, outputPath: String, intensity: Float): String {
        val delay = (200 + intensity * 800).toInt() // 200ms - 1000ms
        val decay = 0.3f + intensity * 0.4f // 0.3 - 0.7
        return "-y -i \"$inputPath\" -af \"aecho=0.8:0.9:$delay:$decay\" \"$outputPath\""
    }

    // Reverb effekti (afreeverb filtri yordamida)
    // FFmpeg da reverb uchun aecho ni ko'p marta qo'llash yoki afreeverb ishlatish mumkin
    fun buildReverbCommand(inputPath: String, outputPath: String, intensity: Float): String {
        val roomSize = 50 + (intensity * 50).toInt() // 50-100
        val damping = 50 + (intensity * 30).toInt() // 50-80
        val wetLevel = -3 - (intensity * 7).toInt() // -3 to -10
        // aecho bilan reverb simulyatsiyasi
        val delay1 = (40 + intensity * 60).toInt()
        val delay2 = (80 + intensity * 120).toInt()
        val decay1 = 0.4f + intensity * 0.2f
        val decay2 = 0.2f + intensity * 0.2f
        return "-y -i \"$inputPath\" -af \"aecho=0.8:0.88:$delay1|$delay2:${decay1}|${decay2}\" \"$outputPath\""
    }

    // Delay effekti
    fun buildDelayCommand(inputPath: String, outputPath: String, intensity: Float): String {
        val delayMs = (300 + intensity * 1200).toInt() // 300ms - 1500ms
        val decay = 0.3f + intensity * 0.3f // 0.3 - 0.6
        return "-y -i \"$inputPath\" -af \"aecho=0.8:0.7:$delayMs:$decay\" \"$outputPath\""
    }

    // Pitch (ton) o'zgartirish - tezlikni o'zgartirmasdan
    // rubberband filtri yordamida yoki asetrate + aresample
    fun buildPitchCommand(inputPath: String, outputPath: String, pitchFactor: Float): String {
        // asetrate bilan pitch o'zgartirish, keyin aresample bilan tezlikni qaytarish
        val sampleRate = 44100
        val newRate = (sampleRate * pitchFactor).toInt()
        return "-y -i \"$inputPath\" -af \"asetrate=$newRate,aresample=$sampleRate,atempo=${1.0f / pitchFactor}\" \"$outputPath\""
    }

    // Tezlik o'zgartirish (atempo)
    fun buildSpeedCommand(inputPath: String, outputPath: String, speedFactor: Float): String {
        // atempo 0.5 dan 100.0 gacha qabul qiladi, lekin 0.5-2.0 orasida yaxshi ishlaydi
        // Agar tezlik 2x dan katta bo'lsa, zanjir qilish kerak
        val tempo = speedFactor.coerceIn(0.25f, 4.0f)
        return if (tempo <= 2.0f && tempo >= 0.5f) {
            "-y -i \"$inputPath\" -af \"atempo=$tempo\" \"$outputPath\""
        } else if (tempo > 2.0f) {
            val t1 = 2.0f
            val t2 = tempo / 2.0f
            "-y -i \"$inputPath\" -af \"atempo=$t1,atempo=$t2\" \"$outputPath\""
        } else {
            val t1 = 0.5f
            val t2 = tempo / 0.5f
            "-y -i \"$inputPath\" -af \"atempo=$t1,atempo=$t2\" \"$outputPath\""
        }
    }

    // Ovoz balandligini o'zgartirish
    fun buildVolumeCommand(inputPath: String, outputPath: String, volumeFactor: Float): String {
        return "-y -i \"$inputPath\" -af \"volume=$volumeFactor\" \"$outputPath\""
    }

    // Format konvertatsiya
    fun buildConvertCommand(inputPath: String, outputPath: String): String {
        return "-y -i \"$inputPath\" \"$outputPath\""
    }

    // Videodan audio ajratish
    fun buildExtractAudioCommand(inputPath: String, outputPath: String): String {
        return "-y -i \"$inputPath\" -vn -acodec copy \"$outputPath\""
    }

    // Videodan audio ajratish (format bilan)
    fun buildExtractAudioWithFormatCommand(inputPath: String, outputPath: String, format: String): String {
        return when (format) {
            "mp3" -> "-y -i \"$inputPath\" -vn -acodec libmp3lame -b:a 192k \"$outputPath\""
            "wav" -> "-y -i \"$inputPath\" -vn -acodec pcm_s16le \"$outputPath\""
            "aac" -> "-y -i \"$inputPath\" -vn -acodec aac -b:a 192k \"$outputPath\""
            else -> "-y -i \"$inputPath\" -vn \"$outputPath\""
        }
    }

    // Audio + Rasm = Video
    fun buildAudioToVideoCommand(audioPath: String, imagePath: String, outputPath: String): String {
        return "-y -loop 1 -i \"$imagePath\" -i \"$audioPath\" -c:v libx264 -tune stillimage -c:a aac -b:a 192k -pix_fmt yuv420p -shortest \"$outputPath\""
    }
}
