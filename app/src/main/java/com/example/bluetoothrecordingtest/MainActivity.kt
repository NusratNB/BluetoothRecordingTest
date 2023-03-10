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
                    Log.d("scoTest", "bluetoothHeadset $bluetoothHeadset")
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
//
//        val bluetoothAdapter = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothAdapter?
//        bluetoothAdapter?.getProfileProxy(this, profileListener, BluetoothProfile.HEADSET)

        val deviceName = "G17 Audio talk" // Replace with the name of the device you want to connect to
        val devices = bluetoothAdapter.bondedDevices
        for (device in devices) {
//                device.uuids
            Log.d("scoTest ${device.name}", "MAC: ${device.address} ")
            if (device.name == deviceName) {
                selectedDevice = device
                Log.d("scoTest: ", selectedDevice.name )
//                if (device.uuids.isNotEmpty()){
//                    for (uu in selectedDevice.uuids){
//                        Log.d("devices $selectedDevice", uu.toString())
//                    }
//
//                }
//                else{
//                    Log.d("devices", "UUID Error")
//                }
                break
            }
        }



//        mediaRecorder = MediaRecorder()
//        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
//        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
//        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

        btnPlay.setOnClickListener {
            val isSupports = isVoiceRecognitionSupported(selectedDevice)

            Log.d("scoTest", "Is device supports: $isSupports")
//            if (outputFile.toString().isNotEmpty()){
//
//                mediaPlayer = MediaPlayer()
//                val afd = assets.openFd(outputFile.absolutePath)
//                mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
//
//                val audioAttributes = AudioAttributes.Builder()
//                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
//                    .setUsage(AudioAttributes.USAGE_MEDIA)
//                    .build()
//                mediaPlayer.setAudioAttributes(audioAttributes)
////                mediaPlayer.setDataSource(outputFile.absolutePath)
//                mediaPlayer.prepare()
//                mediaPlayer.start()
//            } else{
//                Toast.makeText(this, "No audio found", Toast.LENGTH_SHORT).show()
//            }
        }

        Log.d("scoTest", intent?.action.toString())
        if (intent?.categories != null){
            Log.d("scoTest", intent?.categories.toString())
        }

        onNewIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        checkIntent(intent)
    }

    private fun checkIntent(intent: Intent?) {
        if (intent != null) {
            if (intent.action == "android.intent.action.VOICE_COMMAND" || intent.action == "android.intent.action.ASSIST") {
                enableVoiceRecord()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun isVoiceRecognitionSupported(device: BluetoothDevice): Boolean {


        ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
        return bluetoothHeadset!!.isVoiceRecognitionSupported(device)
    }

//    @RequiresApi(Build.VERSION_CODES.S)
//    fun isBluetoothDeviceSupportsVoiceRecognition(device: BluetoothDevice): Boolean {
//
//        ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
//        if (!bluetoothHeadset?.startVoiceRecognition(device)!!) {
//            return false
//        }
//
//        val supported = bluetoothHeadset!!.isVoiceRecognitionSupported(device)
//
//        if (!bluetoothHeadset!!.stopVoiceRecognition(device)) {
//            Log.w("Bluetooth Headset", "Failed to stop voice recognition")
//        }
//
//        return supported
//    }

    private val mBluetoothScoReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.S)
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
            Log.d("scoTest", "ANDROID Audio SCO state: $state")
            Log.d("scoTest", "state: " + state + " isBluetoothScoOn: " +
                    audioManager.isBluetoothScoOn + " isSpeakerphoneOn: " + audioManager.isSpeakerphoneOn
            )

            if (AudioManager.SCO_AUDIO_STATE_CONNECTED == state) {



                isRecording = true
                time.setToNow()

                val audioName = time.format("%Y%m%d%H%M%S") + ".pcm"
                outputFile = File(pathToRecords, audioName)
                audioRecorder.start(outputFile)
//                mediaRecorder.setOutputFile(outputFile.absolutePath)
//                if (device != null) {
//                    if (device.audioProfiles.size>0){
//                        for (i in device.audioProfiles){
//                            Log.d("scoTest", "device audioprofiles: $i")
//                        }
//                    }
//
//                    if(::audioRecorder.isInitialized.not()) {
//                        audioRecorder = CustomAudioRecorder(this@MainActivity)
//                    }
//                    audioRecorder.init()
//
//
//                }else{
//                    Log.d("scoTest", "preferred Device is Null")
//                }


                btnRecord.text = "Recording"
                txtStatus.text = "Recorder status: Recording..."


            } else if (AudioManager.SCO_AUDIO_STATE_DISCONNECTED == state) {
                if (isRecording) {
                    disableVoiceRecord()

                    isRecording = false
                }
            }

        }
    }



    private fun enableVoiceRecord() {
        val intentFilter = IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED )

        registerReceiver(mBluetoothScoReceiver, intentFilter)

        // Start Bluetooth SCO.

        Log.d("scoTest", "isBluetoothScoAvailableOffCall "+ (audioManager.isBluetoothScoAvailableOffCall).toString())

        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isBluetoothScoOn = true
        audioManager.startBluetoothSco()
        // Stop Speaker.

        audioManager.isSpeakerphoneOn = false

    }

    private fun disableVoiceRecord() {
//        if (isRecording) {
//            buttonStopRecording.setEnabled(false)
//            buttonPlayLastRecordAudio.setEnabled(true)
//            buttonStartRecording.setEnabled(true)
//            buttonStopPlayingRecording.setEnabled(false)
//            // Stop Media recorder
//            speechRecognizer.stopListening()
//        }
        try {
            unregisterReceiver(mBluetoothScoReceiver)
        } catch (e: Exception) {
        }
        audioRecorder.stop()
//        mediaRecorder.stop()
//        mediaRecorder.release()
        btnRecord.text = "Start"
        txtStatus.text = "Recorder status: Not Recording"
        isRecording = false

        Log.d("scoTest", "Disconnected, disable sco")
        // Stop Bluetooth SCO.
        audioManager.stopBluetoothSco()
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isBluetoothScoOn = false
        Log.d("scoTest", "isBluetoothScoAvailableOffCall "+ (audioManager.isBluetoothScoAvailableOffCall).toString())
        // Start Speaker.
        audioManager.isSpeakerphoneOn = true
    }
}