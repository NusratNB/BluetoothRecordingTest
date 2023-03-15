package com.example.bluetoothrecordingtest

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.*
import android.os.Build
import android.os.Bundle
import android.text.format.Time
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.File


class MainActivity : AppCompatActivity() {

    private lateinit var txtStatus: TextView
    private lateinit var btnRecord: Button
    private lateinit var btnPlay: Button
    private lateinit var pathToRecords: File
    private lateinit var outputFile: File
    private lateinit var audioRecorder: CustomAudioRecorder
    private lateinit var audioManager: AudioManager
    private var isRecording = false
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var mediaRecorder: MediaRecorder
    private val time = Time()
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var selectedDevice: BluetoothDevice

    private var bluetoothHeadset: BluetoothHeadset? = null




    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("MissingInflatedId", "ServiceCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { // get permission
            ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAPTURE_AUDIO_OUTPUT,
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_COARSE_LOCATION),200);
        }

        val profileListener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.HEADSET) {
                    bluetoothHeadset = proxy as BluetoothHeadset
//                    Log.d("WatchTest", "bluetoothHeadset $bluetoothHeadset")
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.HEADSET) {
                    bluetoothHeadset = null
                }
            }
        }

        pathToRecords = File(externalCacheDir?.absoluteFile, "AudioRecord" )
        if (!pathToRecords.exists()){
            pathToRecords.mkdir()
        }



        txtStatus = findViewById(R.id.txtStatus)
        btnRecord = findViewById(R.id.btnRecord)
        btnPlay = findViewById(R.id.btnPlay)
        audioRecorder = CustomAudioRecorder(this)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        audioManager = applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager

        bluetoothAdapter.getProfileProxy(this, profileListener, BluetoothProfile.HEADSET)

        val deviceName = "DT AudioBE41" // Replace with the name of the device you want to connect to
        val devices = bluetoothAdapter.bondedDevices
        for (device in devices) {
//                device.uuids
            Log.d("WatchTest", "Available devices: ${device.name} ")
            if (device.name == deviceName) {
                selectedDevice = device
                Log.d("WatchTest", "Connected device: "+selectedDevice.name )
                break
            }
        }

        btnPlay.setOnClickListener {
            Toast.makeText(this, "Not implemented", Toast.LENGTH_SHORT).show()
        }

        Log.d("WatchTest", intent?.action.toString())


        onNewIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        checkIntent(intent)
    }

    private fun checkIntent(intent: Intent?) {
        if (intent != null) {
            if (intent.action == "android.intent.action.VOICE_COMMAND") {
                enableVoiceRecord()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun isVoiceRecognitionSupported(device: BluetoothDevice): Boolean {


        ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
        return bluetoothHeadset!!.isVoiceRecognitionSupported(device)
    }

    private val mBluetoothScoReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.S)
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
            Log.d("WatchTest", "ANDROID Audio SCO state: $state")
            Log.d("WatchTest", "state=" + state + " audioManager.isBluetoothScoOn=" +
                    audioManager.isBluetoothScoOn + " audioManager.isSpeakerphoneOn=" + audioManager.isSpeakerphoneOn
            )

            if (AudioManager.SCO_AUDIO_STATE_CONNECTED == state) {

                isRecording = true
                time.setToNow()

                Log.d("WatchTest", "Record start time: "+time.format("%H:%M:%S"))

                val audioName = time.format("%Y%m%d%H%M%S") + ".pcm"
                outputFile = File(pathToRecords, audioName)
                audioRecorder.start(outputFile)

                btnRecord.text = "Recording"
                txtStatus.text = "Recorder status: Recording..."


            } else if (AudioManager.SCO_AUDIO_STATE_DISCONNECTED == state) {
                if (isRecording) {
                    disableVoiceRecord()
                    isRecording = false
                    time.setToNow()
                    Log.d("WatchTest", "Record end time: "+time.format("%H:%M:%S"))
                }
            }

        }
    }



    private fun enableVoiceRecord() {
        val intentFilter = IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED )

        registerReceiver(mBluetoothScoReceiver, intentFilter)

        // Start Bluetooth SCO.
        Log.d("WatchTest", "Connected, enable SCO connection")

        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isBluetoothScoOn = true
        audioManager.startBluetoothSco()
        // Stop Speaker.

        audioManager.isSpeakerphoneOn = false

    }

    private fun disableVoiceRecord() {
        try {
            unregisterReceiver(mBluetoothScoReceiver)
        } catch (e: Exception) {
        }
        audioRecorder.stop()
        btnRecord.text = "Start"
        txtStatus.text = "Recorder status: Not Recording"
        isRecording = false

        Log.d("WatchTest", "Disconnected, disable SCO connection")
        // Stop Bluetooth SCO.
        audioManager.stopBluetoothSco()
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isBluetoothScoOn = false
//        Log.d("WatchTest", "isBluetoothScoAvailableOffCall "+ (audioManager.isBluetoothScoAvailableOffCall).toString())
        // Start Speaker.
        audioManager.isSpeakerphoneOn = true

    }
}