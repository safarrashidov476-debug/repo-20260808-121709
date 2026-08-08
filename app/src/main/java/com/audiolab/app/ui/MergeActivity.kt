package com.audiolab.app.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
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

class MergeActivity : AppCompatActivity() {

    private val PICK_AUDIO = 1002
    private val audioFiles = mutableListOf<File>()
    private val audioNames = mutableListOf<String>()

    private lateinit var layoutFileList: LinearLayout
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var radioGroupMerge: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_merge)
        supportActionBar?.title = "Audioni Birlashtirish"

        layoutFileList = findViewById(R.id.layoutFileList)
        tvStatus = findViewById(R.id.tvStatus)
        progressBar = findViewById(R.id.progressBar)
        radioGroupMerge = findViewById(R.id.radioGroupMerge)

        findViewById<MaterialButton>(R.id.btnAddAudio).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "audio/*"
            startActivityForResult(intent, PICK_AUDIO)
        }

        findViewById<MaterialButton>(R.id.btnMerge).setOnClickListener {
            mergeAudio()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_AUDIO && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                addAudioFile(uri)
            }
        }
    }

    private fun addAudioFile(uri: Uri) {
        val file = FileUtils.copyUriToCache(this, uri, "merge_${audioFiles.size}")
        file?.let {
            audioFiles.add(it)
            val name = FileUtils.getFileName(this, uri)
            audioNames.add(name)
            updateFileList()
        }
    }

    private fun updateFileList() {
        layoutFileList.removeAllViews()
        audioNames.forEachIndexed { index, name ->
            val tv = TextView(this).apply {
                text = "${index + 1}. $name"
                textSize = 14f
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(0, 8, 0, 8)
            }
            layoutFileList.addView(tv)
        }
    }

    private fun mergeAudio() {
        if (audioFiles.size < 2) {
            Toast.makeText(this, "Kamida 2 ta audio fayl tanlang!", Toast.LENGTH_SHORT).show()
            return
        }

        val isSequential = radioGroupMerge.checkedRadioButtonId == R.id.radioSequential
        val outputFile = FileUtils.getOutputFile(this, "merged", "mp3")

        lifecycleScope.launch {
            tvStatus.text = "Birlashtirilmoqda..."
            progressBar.visibility = View.VISIBLE

            val result = if (isSequential) {
                // Ketma-ket birlashtirish - concat list fayl yaratish
                val listFile = File(cacheDir, "concat_list.txt")
                val content = audioFiles.joinToString("\n") { "file '${it.absolutePath}'" }
                listFile.writeText(content)
                val command = FFmpegHelper.buildConcatCommand(listFile.absolutePath, outputFile.absolutePath)
                FFmpegHelper.execute(command)
            } else {
                // Ustma-ust qo'yish (mix)
                val command = FFmpegHelper.buildMixCommand(
                    audioFiles.map { it.absolutePath },
                    outputFile.absolutePath
                )
                FFmpegHelper.execute(command)
            }

            progressBar.visibility = View.GONE

            if (result.success) {
                tvStatus.text = "✅ Saqlandi: ${outputFile.name}"
                Toast.makeText(this@MergeActivity, "Muvaffaqiyatli birlashtirildi!", Toast.LENGTH_LONG).show()
            } else {
                tvStatus.text = "❌ Xatolik yuz berdi"
                Toast.makeText(this@MergeActivity, "Xatolik: ${result.message.take(100)}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        FileUtils.cleanTempFiles(this)
    }
}
