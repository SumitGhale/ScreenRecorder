package com.example.screenrecorder
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

class HomeScreenViewModel(private val context: Context): ViewModel() {
    private val _isRecording = mutableStateOf(false)
    val isRecording: State<Boolean> get() = _isRecording

    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null

    val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

    //function to start recording
    @SuppressLint("Recycle")
    fun startRecording(
        resultCode: Int,
        data: Intent,
        onRecordingStarted: () -> Unit,
        onError: (String) -> Unit
    ){
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "screen_recording_${System.currentTimeMillis()}.mp4")
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/ScreenRecorder")
            }

            val contentResolver = context.contentResolver
            val videoUri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

            mediaProjection = mediaProjectionManager.getMediaProjection(
                resultCode,
                data!!
            )
            mediaRecorder = MediaRecorder().apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                videoUri?.let {
                    contentResolver.openFileDescriptor(it, "w")?.fileDescriptor?.let { fd->
                        setOutputFile(fd)
                    }
                }
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoSize(1280, 720)
                setVideoEncodingBitRate(5_000_000)
                setVideoFrameRate(30)
                prepare()
            }

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenRecording",
                1280,
                720,
                context.resources.displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder!!.surface,
                null,
                null
            )
            mediaRecorder?.start()
            _isRecording.value = true
            onRecordingStarted()
        }catch (e: Exception){
            onError(e.localizedMessage?: "Failed to start recording")
        }
    }

    //  Function to stop recording
    fun stopRecording(){
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    Log.d("ViewModel", "Error stopping mediaRecorder: ${e.message}")
                }
                reset()
                release()
            }

            virtualDisplay?.release()
            mediaProjection?.stop()

            // Reset state
            mediaRecorder = null
            virtualDisplay = null
            mediaProjection = null

            _isRecording.value = false

            // Stop service
            val serviceIntent = Intent(context, ScreenRecordingService::class.java)
            context.stopService(serviceIntent)
        }catch (e: Exception){
            Log.d("ViewModel", "Error stopping recording: ${e.message}")
        }
    }
}