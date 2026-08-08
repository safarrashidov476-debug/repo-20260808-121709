package com.audiolab.app.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
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

class AudioToVideoActivity : AppCompatActivity() {

    private val PICK_AUDIO = 1006
    private val PICK_IMAGE = 1007
    private var audioFile: File? = null
    private var imageFile: File? = null

    private lateinit var tvAudioFile: TextView
    private lateinit var tvImageFile: TextView
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_to_video)
        supportActionBar?.title = "Audiodan Video Yasash"

        tvAudioFile = findViewById(R.id.tvAudioFile)
        tvImageFile = findViewById(R.id.tvImageFile)
        tvStatus = findViewById(R.id.tvStatus)
        progressBar = findViewById(R.id.progressBar)

        findViewById<MaterialButton>(R.id.btnSelectAudio).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "audio/*"
            startActivityForResult(intent, PICK_AUDIO)
        }

        findViewById<MaterialButton>(R.id.btnSelectImage).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE)
        }

        findViewById<MaterialButton>(R.id.btnCreate).setOnClickListener {
            createVideo()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                when (requestCode) {
                    PICK_AUDIO -> {
                        audioFile = FileUtils.copyUriToCache(this, uri, "a2v_audio")
                        tvAudioFile.text = FileUtils.getFileName(this, uri)
                    }
                    PICK_IMAGE -> {
                        imageFile = FileUtils.copyUriToCache(this, uri, "a2v_image")
                        tvImageFile.text = FileUtils.getFileName(this, uri)
                    }
                }
            }
        }
    }

    private fun createVideo() {
        if (audioFile == null) {
            Toast.makeText(this, "Avval audio tanlang!", Toast.LENGTH_SHORT).show()
            return
        }
        if (imageFile == null) {
            Toast.makeText(this, "Avval rasm tanlang!", Toast.LENGTH_SHORT).show()
            return
        }

        val outputFile = FileUtils.getOutputFile(this, "audio_video", "mp4")

        lifecycleScope.launch {
            tvStatus.text = "Video yaratilmoqda..."
            progressBar.visibility = View.VISIBLE

            val command = FFmpegHelper.buildAudioToVideoCommand(
                audioFile!!.absolutePath,
                imageFile!!.absolutePath,
                outputFile.absolutePath
            )
            val result = FFmpegHelper.execute(command)

            progressBar.visibility = View.GONE

            if (result.success) {
                tvStatus.text = "✅ Saqlandi: ${outputFile.name}"
                Toast.makeText(this@AudioToVideoActivity, "Video muvaffaqiyatli yaratildi!", Toast.LENGTH_LONG).show()
            } else {
                tvStatus.text = "❌ Xatolik yuz berdi"
                Toast.makeText(this@AudioToVideoActivity, "Xatolik: ${result.message.take(100)}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        FileUtils.cleanTempFiles(this)
    }
}
