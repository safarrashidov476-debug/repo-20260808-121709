package com.audiolab.app.ui

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.audiolab.app.R
import com.audiolab.app.utils.FFmpegHelper
import com.audiolab.app.utils.FileUtils
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.io.File

class EffectsActivity : AppCompatActivity() {

    private val PICK_AUDIO = 1003
    private var inputFile: File? = null
    private var previewFile: File? = null

    // Original audio player
    private var origPlayer: MediaPlayer? = null
    private var origPlaying = false

    // Effect preview player
    private var effectPlayer: MediaPlayer? = null

    private val handler = Handler(Looper.getMainLooper())

    private lateinit var tvFileName: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvCurrentTime: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEchoValue: TextView
    private lateinit var tvReverbValue: TextView
    private lateinit var tvDelayValue: TextView
    private lateinit var tvPitchValue: TextView
    private lateinit var tvSpeedValue: TextView
    private lateinit var tvVolumeValue: TextView
    private lateinit var radioGroupFormat: RadioGroup
    private lateinit var seekBarAudio: SeekBar

    private lateinit var seekEcho: SeekBar
    private lateinit var seekReverb: SeekBar
    private lateinit var seekDelay: SeekBar
    private lateinit var seekPitch: SeekBar
    private lateinit var seekSpeed: SeekBar
    private lateinit var seekVolume: SeekBar

    private lateinit var layoutOriginalPlayer: LinearLayout
    private lateinit var layoutEffectPlayer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_effects)
        supportActionBar?.title = "Audio Effektlar"

        initViews()
        setupListeners()
    }

    private fun initViews() {
        tvFileName = findViewById(R.id.tvFileName)
        tvStatus = findViewById(R.id.tvStatus)
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        progressBar = findViewById(R.id.progressBar)
        tvEchoValue = findViewById(R.id.tvEchoValue)
        tvReverbValue = findViewById(R.id.tvReverbValue)
        tvDelayValue = findViewById(R.id.tvDelayValue)
        tvPitchValue = findViewById(R.id.tvPitchValue)
        tvSpeedValue = findViewById(R.id.tvSpeedValue)
        tvVolumeValue = findViewById(R.id.tvVolumeValue)
        radioGroupFormat = findViewById(R.id.radioGroupFormat)
        seekBarAudio = findViewById(R.id.seekBarAudio)

        seekEcho = findViewById(R.id.seekEcho)
        seekReverb = findViewById(R.id.seekReverb)
        seekDelay = findViewById(R.id.seekDelay)
        seekPitch = findViewById(R.id.seekPitch)
        seekSpeed = findViewById(R.id.seekSpeed)
        seekVolume = findViewById(R.id.seekVolume)

        layoutOriginalPlayer = findViewById(R.id.layoutOriginalPlayer)
        layoutEffectPlayer = findViewById(R.id.layoutEffectPlayer)
    }

    private fun setupListeners() {
        // Audio tanlash
        findViewById<MaterialButton>(R.id.btnSelectAudio).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "audio/*"
            startActivityForResult(intent, PICK_AUDIO)
        }

        // Original audio Play/Pause/Stop
        findViewById<MaterialButton>(R.id.btnOrigPlay).setOnClickListener {
            origPlayer?.let {
                if (!it.isPlaying) {
                    it.start()
                    origPlaying = true
                    updateOrigSeekBar()
                }
            }
        }
        findViewById<MaterialButton>(R.id.btnOrigPause).setOnClickListener {
            origPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    origPlaying = false
                }
            }
        }
        findViewById<MaterialButton>(R.id.btnOrigStop).setOnClickListener {
            stopOrigPlayer()
        }

        // SeekBar for original audio
        seekBarAudio.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    origPlayer?.seekTo(progress)
                    tvCurrentTime.text = "${FileUtils.formatTime(progress)} / ${FileUtils.formatTime(seekBarAudio.max)}"
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Seekbar listeners - raqamni ko'rsatish
        seekEcho.setOnSeekBarChangeListener(simpleListener(tvEchoValue))
        seekReverb.setOnSeekBarChangeListener(simpleListener(tvReverbValue))
        seekDelay.setOnSeekBarChangeListener(simpleListener(tvDelayValue))

        seekPitch.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvPitchValue.text = if (progress == 50) "$progress (normal)" else "$progress"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvSpeedValue.text = if (progress == 50) "$progress (normal)" else "$progress"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvVolumeValue.text = if (progress == 50) "$progress (normal)" else "$progress"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Effektni qo'llab eshitish
        findViewById<MaterialButton>(R.id.btnApply).setOnClickListener { applyAndPlay() }

        // Effect player Play/Pause/Stop
        findViewById<MaterialButton>(R.id.btnPlay).setOnClickListener {
            effectPlayer?.let {
                if (!it.isPlaying) {
                    it.start()
                    tvStatus.text = "▶️ Ijro etilmoqda..."
                }
            }
        }
        findViewById<MaterialButton>(R.id.btnPause).setOnClickListener {
            effectPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    tvStatus.text = "⏸️ Pauza"
                }
            }
        }
        findViewById<MaterialButton>(R.id.btnStop).setOnClickListener {
            stopEffectPlayer()
            tvStatus.text = "⏹️ To'xtatildi"
        }

        // Qayta effekt berish
        findViewById<MaterialButton>(R.id.btnReset).setOnClickListener {
            stopEffectPlayer()
            layoutEffectPlayer.visibility = View.GONE
            previewFile?.delete()
            previewFile = null
            tvStatus.text = "Sliderlarni sozlab, qayta \"Effektni qo'llab eshitish\" bosing"
        }

        // Saqlab olish
        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener { saveWithEffect() }
    }

    private fun simpleListener(tv: TextView): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tv.text = "$progress"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_AUDIO && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                stopOrigPlayer()
                stopEffectPlayer()
                layoutEffectPlayer.visibility = View.GONE

                inputFile = FileUtils.copyUriToCache(this, uri, "effects_input")
                tvFileName.text = FileUtils.getFileName(this, uri)

                // Original audio player sozlash
                inputFile?.let { file ->
                    origPlayer = MediaPlayer().apply {
                        setDataSource(file.absolutePath)
                        prepare()
                    }
                    val duration = origPlayer!!.duration
                    seekBarAudio.max = duration
                    tvCurrentTime.text = "00:00 / ${FileUtils.formatTime(duration)}"
                    layoutOriginalPlayer.visibility = View.VISIBLE
                    tvStatus.text = "Audio tanlandi. Eshitib ko'ring yoki effekt sozlang."
                }
            }
        }
    }

    private fun updateOrigSeekBar() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                origPlayer?.let {
                    if (origPlaying && it.isPlaying) {
                        seekBarAudio.progress = it.currentPosition
                        tvCurrentTime.text = "${FileUtils.formatTime(it.currentPosition)} / ${FileUtils.formatTime(seekBarAudio.max)}"
                        handler.postDelayed(this, 200)
                    }
                }
            }
        }, 200)
    }

    private fun stopOrigPlayer() {
        origPlaying = false
        origPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        origPlayer = null
        seekBarAudio.progress = 0
    }

    private fun stopEffectPlayer() {
        effectPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        effectPlayer = null
    }

    private fun getSelectedFormat(): String {
        return when (radioGroupFormat.checkedRadioButtonId) {
            R.id.radioMp3 -> "mp3"
            R.id.radioWav -> "wav"
            R.id.radioAac -> "aac"
            R.id.radioOgg -> "ogg"
            R.id.radioFlac -> "flac"
            else -> "mp3"
        }
    }

    private fun buildFilterChain(): String {
        val filters = mutableListOf<String>()

        val echoVal = seekEcho.progress
        if (echoVal > 0) {
            val delay = 100 + (echoVal * 14)
            val decay = 0.2f + (echoVal / 100.0f) * 0.6f
            filters.add("aecho=0.8:0.9:$delay:${String.format("%.2f", decay)}")
        }

        val reverbVal = seekReverb.progress
        if (reverbVal > 0) {
            val d1 = 20 + (reverbVal * 0.8).toInt()
            val d2 = 40 + (reverbVal * 1.6).toInt()
            val decay1 = 0.3f + (reverbVal / 100.0f) * 0.3f
            val decay2 = 0.15f + (reverbVal / 100.0f) * 0.25f
            filters.add("aecho=0.8:0.88:$d1|$d2:${String.format("%.2f", decay1)}|${String.format("%.2f", decay2)}")
        }

        val delayVal = seekDelay.progress
        if (delayVal > 0) {
            val delay = 200 + (delayVal * 18)
            val decay = 0.25f + (delayVal / 100.0f) * 0.45f
            filters.add("aecho=0.8:0.7:$delay:${String.format("%.2f", decay)}")
        }

        val volVal = seekVolume.progress
        if (volVal != 50) {
            val volume = if (volVal <= 50) {
                volVal / 50.0f
            } else {
                1.0f + ((volVal - 50) / 50.0f) * 2.0f
            }
            filters.add("volume=${String.format("%.2f", volume)}")
        }

        val speedVal = seekSpeed.progress
        if (speedVal != 50) {
            val speed = if (speedVal <= 50) {
                0.25f + (speedVal / 50.0f) * 0.75f
            } else {
                1.0f + ((speedVal - 50) / 50.0f) * 3.0f
            }
            if (speed >= 0.5f && speed <= 2.0f) {
                filters.add("atempo=${String.format("%.2f", speed)}")
            } else if (speed < 0.5f) {
                filters.add("atempo=0.50,atempo=${String.format("%.2f", speed / 0.5f)}")
            } else {
                filters.add("atempo=2.00,atempo=${String.format("%.2f", speed / 2.0f)}")
            }
        }

        val pitchVal = seekPitch.progress
        if (pitchVal != 50) {
            val pitch = if (pitchVal <= 50) {
                0.5f + (pitchVal / 50.0f) * 0.5f
            } else {
                1.0f + ((pitchVal - 50) / 50.0f) * 1.0f
            }
            val sampleRate = 44100
            val newRate = (sampleRate * pitch).toInt()
            val tempoCorrection = 1.0f / pitch
            if (tempoCorrection >= 0.5f && tempoCorrection <= 2.0f) {
                filters.add("asetrate=$newRate,aresample=$sampleRate,atempo=${String.format("%.2f", tempoCorrection)}")
            } else if (tempoCorrection < 0.5f) {
                filters.add("asetrate=$newRate,aresample=$sampleRate,atempo=0.50,atempo=${String.format("%.2f", tempoCorrection / 0.5f)}")
            } else {
                filters.add("asetrate=$newRate,aresample=$sampleRate,atempo=2.00,atempo=${String.format("%.2f", tempoCorrection / 2.0f)}")
            }
        }

        return if (filters.isEmpty()) "" else filters.joinToString(",")
    }

    private fun applyAndPlay() {
        if (inputFile == null) {
            Toast.makeText(this, "Avval musiqa tanlang!", Toast.LENGTH_SHORT).show()
            return
        }

        val filterChain = buildFilterChain()
        if (filterChain.isEmpty()) {
            Toast.makeText(this, "Hech qanday effekt sozlanmagan! Sliderlarni o'zgartiring.", Toast.LENGTH_SHORT).show()
            return
        }

        // Original playerni to'xtatish
        origPlaying = false
        origPlayer?.let { if (it.isPlaying) it.pause() }

        stopEffectPlayer()
        layoutEffectPlayer.visibility = View.GONE

        val format = getSelectedFormat()
        previewFile = FileUtils.getTempFile(this, format)

        lifecycleScope.launch {
            tvStatus.text = "⏳ Effekt qo'llanmoqda..."
            progressBar.visibility = View.VISIBLE

            val command = "-y -i \"${inputFile!!.absolutePath}\" -af \"$filterChain\" \"${previewFile!!.absolutePath}\""
            val result = FFmpegHelper.execute(command)

            progressBar.visibility = View.GONE

            if (result.success) {
                tvStatus.text = "▶️ Effektli audio ijro etilmoqda..."
                layoutEffectPlayer.visibility = View.VISIBLE
                effectPlayer = MediaPlayer().apply {
                    setDataSource(previewFile!!.absolutePath)
                    prepare()
                    start()
                    setOnCompletionListener {
                        tvStatus.text = "✅ Ijro tugadi. Yoqsa \"Saqlab olish\", yoqmasa \"Qayta effekt berish\" bosing."
                    }
                }
            } else {
                tvStatus.text = "❌ Xatolik: ${result.message.take(100)}"
            }
        }
    }

    private fun saveWithEffect() {
        if (inputFile == null) {
            Toast.makeText(this, "Avval musiqa tanlang!", Toast.LENGTH_SHORT).show()
            return
        }

        val filterChain = buildFilterChain()
        if (filterChain.isEmpty()) {
            Toast.makeText(this, "Hech qanday effekt sozlanmagan!", Toast.LENGTH_SHORT).show()
            return
        }

        stopEffectPlayer()

        val format = getSelectedFormat()
        val outputFile = FileUtils.getOutputFile(this, "effected", format)

        lifecycleScope.launch {
            tvStatus.text = "💾 Saqlanmoqda..."
            progressBar.visibility = View.VISIBLE

            val command = "-y -i \"${inputFile!!.absolutePath}\" -af \"$filterChain\" \"${outputFile.absolutePath}\""
            val result = FFmpegHelper.execute(command)

            progressBar.visibility = View.GONE

            if (result.success) {
                tvStatus.text = "✅ Saqlandi: ${outputFile.name}"
                Toast.makeText(this@EffectsActivity, "Muvaffaqiyatli saqlandi!\n${outputFile.absolutePath}", Toast.LENGTH_LONG).show()
            } else {
                tvStatus.text = "❌ Xatolik yuz berdi"
                Toast.makeText(this@EffectsActivity, "Xatolik: ${result.message.take(100)}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopOrigPlayer()
        stopEffectPlayer()
        previewFile?.delete()
        FileUtils.cleanTempFiles(this)
    }
}
