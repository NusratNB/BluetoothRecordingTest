package com.example.bluetoothrecordingtest

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileOutputStream

class CustomAudioRecorder {
    private val ctx: Context

    constructor(ctx: Context) {
        this.ctx = ctx
        init()
    }

    private val audioSource = MediaRecorder.AudioSource.DEFAULT
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var isRecording = false



    fun init(){

        ActivityCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO)
        audioRecord = AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize())
    }


    fun start(outputFile: File) {

        init()

        ActivityCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO)
        audioRecord = AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize())
//        audioRecord?.preferredDevice = preferredDevice
        audioRecord?.startRecording()
        Log.d("WatchTest ", "AudioRecorder file path $outputFile")

        isRecording = true
        Thread(Runnable {
            writeAudioDataToFile(outputFile)
        }).start()
    }

    fun stop() {
        audioRecord?.stop()
//        audioRecord?.release()
        audioRecord = null
        isRecording = false
    }

    private fun writeAudioDataToFile(outputFile: File) {
        val data = ByteArray(bufferSize())
        val outputStream = FileOutputStream(outputFile)
        while (isRecording) {
            val read = audioRecord?.read(data, 0, bufferSize()) ?: 0
            if (read > 0) {
                outputStream.write(data, 0, read)
            }
        }
        outputStream.close()
    }

    private fun bufferSize(): Int {
//        Log.d("scoTest", "bufferSize " + (AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)).toString())
        return AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    }
}