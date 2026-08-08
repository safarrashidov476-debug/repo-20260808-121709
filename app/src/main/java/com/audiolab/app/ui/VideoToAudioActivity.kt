package com.audiolab.app.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.RadioGroup
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

class VideoToAudioActivity : AppCompatActivity() {

    private val PICK_VIDEO = 1005
    private var inputFile: File? = null

    private lateinit var tvFileName: TextView
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var radioGroupFormat: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_to_audio)
        supportActionBar?.title = "Videodan Audio Ajratish"

        tvFileName = findViewById(R.id.tvFileName)
        tvStatus = findViewById(R.id.tvStatus)
        progressBar = findViewById(R.id.progressBar)
        radioGroupFormat = findViewById(R.id.radioGroupFormat)

        findViewById<MaterialButton>(R.id.btnSelectVideo).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "video/*"
            startActivityForResult(intent, PICK_VIDEO)
        }

        findViewById<MaterialButton>(R.id.btnExtract).setOnClickListener {
            extractAudio()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_VIDEO && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                inputFile = FileUtils.copyUriToCache(this, uri, "video_input")
                tvFileName.text = FileUtils.getFileName(this, uri)
            }
        }
    }

    private fun extractAudio() {
        if (inputFile == null) {
            Toast.makeText(this, "Avval video tanlang!", Toast.LENGTH_SHORT).show()
            return
        }

        val format = when (radioGroupFormat.checkedRadioButtonId) {
            R.id.radioMp3 -> "mp3"
            R.id.radioWav -> "wav"
            R.id.radioAac -> "aac"
            else -> "mp3"
        }

        val outputFile = FileUtils.getOutputFile(this, "extracted_audio", format)

        lifecycleScope.launch {
            tvStatus.text = "Audio ajratilmoqda..."
            progressBar.visibility = View.VISIBLE

            val command = FFmpegHelper.buildExtractAudioWithFormatCommand(
                inputFile!!.absolutePath, outputFile.absolutePath, format
            )
            val result = FFmpegHelper.execute(command)

            progressBar.visibility = View.GONE

            if (result.success) {
                tvStatus.text = "✅ Saqlandi: ${outputFile.name}"
                Toast.makeText(this@VideoToAudioActivity, "Audio muvaffaqiyatli ajratildi!", Toast.LENGTH_LONG).show()
            } else {
                tvStatus.text = "❌ Xatolik yuz berdi"
                Toast.makeText(this@VideoToAudioActivity, "Xatolik: ${result.message.take(100)}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        FileUtils.cleanTempFiles(this)
    }
}
