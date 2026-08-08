package com.audiolab.app.ui

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
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

class TrimActivity : AppCompatActivity() {

    private val PICK_AUDIO = 1001

    private var mediaPlayer: MediaPlayer? = null
    private var inputFile: File? = null
    private var startTimeMs: Int = 0
    private var endTimeMs: Int = 0
    private var duration: Int = 0

    private val handler = Handler(Looper.getMainLooper())
    private var isPlaying = false

    private lateinit var tvFileName: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvCurrentPos: TextView
    private lateinit var tvStartTime: TextView
    private lateinit var tvEndTime: TextView
    private lateinit var tvStatus: TextView
    private lateinit var seekBarAudio: SeekBar
    private lateinit var progressBar: ProgressBar
    private lateinit var radioGroupFormat: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trim)
        supportActionBar?.title = "Audioni Kesish"

        initViews()
        setupListeners()
    }

    private fun initViews() {
        tvFileName = findViewById(R.id.tvFileName)
        tvDuration = findViewById(R.id.tvDuration)
        tvCurrentPos = findViewById(R.id.tvCurrentPos)
        tvStartTime = findViewById(R.id.tvStartTime)
        tvEndTime = findViewById(R.id.tvEndTime)
        tvStatus = findViewById(R.id.tvStatus)
        seekBarAudio = findViewById(R.id.seekBarAudio)
        progressBar = findViewById(R.id.progressBar)
        radioGroupFormat = findViewById(R.id.radioGroupFormat)
    }

    private fun setupListeners() {
        findViewById<MaterialButton>(R.id.btnSelectAudio).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "audio/*"
            startActivityForResult(intent, PICK_AUDIO)
        }

        findViewById<MaterialButton>(R.id.btnPlay).setOnClickListener { playAudio() }
        findViewById<MaterialButton>(R.id.btnPause).setOnClickListener { pauseAudio() }
        findViewById<MaterialButton>(R.id.btnStop).setOnClickListener { stopAudio() }

        // Eshitib borayotganda "Kesish boshlanishi" belgilash
        findViewById<MaterialButton>(R.id.btnSetStart).setOnClickListener {
            mediaPlayer?.let {
                startTimeMs = it.currentPosition
                tvStartTime.text = "Boshlanishi: ${FileUtils.formatTime(startTimeMs)}"
                tvStatus.text = "✂️ Kesish boshlanishi belgilandi: ${FileUtils.formatTime(startTimeMs)}"
            }
        }

        // Eshitib borayotganda "Kesish tugashi" belgilash
        findViewById<MaterialButton>(R.id.btnSetEnd).setOnClickListener {
            mediaPlayer?.let {
                endTimeMs = it.currentPosition
                tvEndTime.text = "Tugashi: ${FileUtils.formatTime(endTimeMs)}"
                tvStatus.text = "✂️ Kesish tugashi belgilandi: ${FileUtils.formatTime(endTimeMs)}"
            }
        }

        findViewById<MaterialButton>(R.id.btnPreview).setOnClickListener { previewTrim() }
        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener { saveTrim() }

        seekBarAudio.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                    tvCurrentPos.text = FileUtils.formatTime(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_AUDIO && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                loadAudio(uri)
            }
        }
    }

    private fun loadAudio(uri: Uri) {
        stopAudio()
        inputFile = FileUtils.copyUriToCache(this, uri, "trim_input")
        inputFile?.let { file ->
            tvFileName.text = FileUtils.getFileName(this, uri)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
            }

            duration = mediaPlayer!!.duration
            endTimeMs = duration
            startTimeMs = 0
            seekBarAudio.max = duration
            seekBarAudio.progress = 0
            tvDuration.text = "Davomiylik: ${FileUtils.formatTime(duration)}"
            tvEndTime.text = "Tugashi: ${FileUtils.formatTime(duration)}"
            tvStartTime.text = "Boshlanishi: 00:00"
            tvCurrentPos.text = "00:00"
            tvStatus.text = "Audio tanlandi. Play bosib eshiting, keyin kesish joylarini belgilang."
        }
    }

    private fun playAudio() {
        mediaPlayer?.let {
            it.start()
            isPlaying = true
            updateSeekBar()
        }
    }

    private fun pauseAudio() {
        mediaPlayer?.pause()
        isPlaying = false
    }

    private fun stopAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
        isPlaying = false
        seekBarAudio.progress = 0
        tvCurrentPos.text = "00:00"
    }

    private fun updateSeekBar() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                mediaPlayer?.let {
                    if (isPlaying && it.isPlaying) {
                        seekBarAudio.progress = it.currentPosition
                        tvCurrentPos.text = FileUtils.formatTime(it.currentPosition)
                        handler.postDelayed(this, 200)
                    }
                }
            }
        }, 200)
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

    private fun previewTrim() {
        if (inputFile == null) {
            Toast.makeText(this, "Avval audio tanlang!", Toast.LENGTH_SHORT).show()
            return
        }
        if (startTimeMs >= endTimeMs) {
            Toast.makeText(this, "Boshlanish vaqti tugash vaqtidan kichik bo'lishi kerak!", Toast.LENGTH_SHORT).show()
            return
        }

        stopAudio()
        val format = getSelectedFormat()
        val tempFile = FileUtils.getTempFile(this, format)

        lifecycleScope.launch {
            tvStatus.text = "⏳ Preview tayyorlanmoqda..."
            progressBar.visibility = View.VISIBLE

            val startSec = startTimeMs / 1000.0
            val endSec = endTimeMs / 1000.0
            val command = FFmpegHelper.buildTrimCommand(inputFile!!.absolutePath, tempFile.absolutePath, startSec, endSec)
            val result = FFmpegHelper.execute(command)

            progressBar.visibility = View.GONE

            if (result.success) {
                tvStatus.text = "▶️ Kesilgan qism ijro etilmoqda..."
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(tempFile.absolutePath)
                    prepare()
                    start()
                    setOnCompletionListener {
                        tvStatus.text = "✅ Preview tugadi"
                    }
                }
                duration = mediaPlayer!!.duration
                seekBarAudio.max = duration
                isPlaying = true
                updateSeekBar()
            } else {
                tvStatus.text = "❌ Xatolik: ${result.message.take(100)}"
            }
        }
    }

    private fun saveTrim() {
        if (inputFile == null) {
            Toast.makeText(this, "Avval audio tanlang!", Toast.LENGTH_SHORT).show()
            return
        }
        if (startTimeMs >= endTimeMs) {
            Toast.makeText(this, "Boshlanish vaqti tugash vaqtidan kichik bo'lishi kerak!", Toast.LENGTH_SHORT).show()
            return
        }

        val format = getSelectedFormat()
        val outputFile = FileUtils.getOutputFile(this, "trimmed", format)

        lifecycleScope.launch {
            tvStatus.text = "💾 Saqlanmoqda..."
            progressBar.visibility = View.VISIBLE

            val startSec = startTimeMs / 1000.0
            val endSec = endTimeMs / 1000.0
            val command = FFmpegHelper.buildTrimCommand(inputFile!!.absolutePath, outputFile.absolutePath, startSec, endSec)
            val result = FFmpegHelper.execute(command)

            progressBar.visibility = View.GONE

            if (result.success) {
                tvStatus.text = "✅ Saqlandi: ${outputFile.name}"
                Toast.makeText(this@TrimActivity, "Muvaffaqiyatli saqlandi!\n${outputFile.absolutePath}", Toast.LENGTH_LONG).show()
            } else {
                tvStatus.text = "❌ Xatolik yuz berdi"
                Toast.makeText(this@TrimActivity, "Xatolik: ${result.message.take(100)}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudio()
        FileUtils.cleanTempFiles(this)
    }
}
