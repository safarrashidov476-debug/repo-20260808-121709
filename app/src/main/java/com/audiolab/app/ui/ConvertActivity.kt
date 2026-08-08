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

class ConvertActivity : AppCompatActivity() {

    private val PICK_AUDIO = 1004
    private var inputFile: File? = null

    private lateinit var tvFileName: TextView
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var radioGroupFormat: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_convert)
        supportActionBar?.title = "Format O'zgartirish"

        tvFileName = findViewById(R.id.tvFileName)
        tvStatus = findViewById(R.id.tvStatus)
        progressBar = findViewById(R.id.progressBar)
        radioGroupFormat = findViewById(R.id.radioGroupFormat)

        findViewById<MaterialButton>(R.id.btnSelectAudio).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "audio/*"
            startActivityForResult(intent, PICK_AUDIO)
        }

        findViewById<MaterialButton>(R.id.btnConvert).setOnClickListener {
            convertAudio()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_AUDIO && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                inputFile = FileUtils.copyUriToCache(this, uri, "convert_input")
                tvFileName.text = FileUtils.getFileName(this, uri)
            }
        }
    }

    private fun convertAudio() {
        if (inputFile == null) {
            Toast.makeText(this, "Avval audio tanlang!", Toast.LENGTH_SHORT).show()
            return
        }

        val format = when (radioGroupFormat.checkedRadioButtonId) {
            R.id.radioMp3 -> "mp3"
            R.id.radioWav -> "wav"
            R.id.radioAac -> "aac"
            R.id.radioOgg -> "ogg"
            R.id.radioFlac -> "flac"
            else -> "mp3"
        }

        val outputFile = FileUtils.getOutputFile(this, "converted", format)

        lifecycleScope.launch {
            tvStatus.text = "Konvertatsiya qilinmoqda..."
            progressBar.visibility = View.VISIBLE

            val command = FFmpegHelper.buildConvertCommand(inputFile!!.absolutePath, outputFile.absolutePath)
            val result = FFmpegHelper.execute(command)

            progressBar.visibility = View.GONE

            if (result.success) {
                tvStatus.text = "✅ Saqlandi: ${outputFile.name}"
                Toast.makeText(this@ConvertActivity, "Muvaffaqiyatli konvertatsiya qilindi!", Toast.LENGTH_LONG).show()
            } else {
                tvStatus.text = "❌ Xatolik yuz berdi"
                Toast.makeText(this@ConvertActivity, "Xatolik: ${result.message.take(100)}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        FileUtils.cleanTempFiles(this)
    }
}
