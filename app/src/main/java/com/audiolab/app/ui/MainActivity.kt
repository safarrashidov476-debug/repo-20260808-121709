package com.audiolab.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.audiolab.app.R
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()

        checkPermissions()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        findViewById<MaterialCardView>(R.id.cardTrim).setOnClickListener {
            startActivity(Intent(this, TrimActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardMerge).setOnClickListener {
            startActivity(Intent(this, MergeActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardEffects).setOnClickListener {
            startActivity(Intent(this, EffectsActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardConvert).setOnClickListener {
            startActivity(Intent(this, ConvertActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardVideoToAudio).setOnClickListener {
            startActivity(Intent(this, VideoToAudioActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardAudioToVideo).setOnClickListener {
            startActivity(Intent(this, AudioToVideoActivity::class.java))
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            // Android 12 va pastroq
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Ruxsatnomalar kerak!", Toast.LENGTH_LONG).show()
            }
        }
    }
}
