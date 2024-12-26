package com.example.screenrecorder

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@SuppressLint("ServiceCast", "Recycle")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController
){
    val context = LocalContext.current

    ViewModelStore.initialize(context.applicationContext)
    val viewModel = remember {
        ViewModelStore.getInstance()
    }
    val isRecording by viewModel.isRecording
    val serviceIntent = Intent(context, ScreenRecordingService::class.java)

    val screenCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.startRecording(
                    resultCode = result.resultCode,
                    data = result.data!!,
                    onRecordingStarted = {
                        Toast.makeText(context, "Recording Started", Toast.LENGTH_SHORT).show()
                    },
                    onError = {errorMessage ->
                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    )

    // Audio permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, launch screen capture intent
            screenCaptureLauncher.launch(
                viewModel.mediaProjectionManager.createScreenCaptureIntent()
            )
        } else {
            // Permission denied, show a message
            Toast.makeText(context, "Audio permission is required to record audio.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Screen Recorder", color = Color.White)
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color.Black
                )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(it)
                .fillMaxSize()
        ) {
            Divider(
                modifier = Modifier
                    .padding(8.dp) // Padding around the divider
                    .fillMaxWidth(), // Makes the divider span the full width
                thickness = 2.dp, // Thickness of the line
                color = Color.Gray // Color of the divider
            )

            Button(onClick = {
                navController.navigate("video_list")
            }) {
                Text(text = "Your videos")
            }

            Box(modifier = Modifier
                .background(Color.Black)
                .fillMaxSize(),
                contentAlignment = Alignment.Center) {
                if(isRecording){
                    Box(
                        modifier = Modifier
                            .size(100.dp) // Size of the box
                            .clip(CircleShape) // Clips the box into a circle
                            .background(Color.White)
                            .clickable {
                                viewModel.stopRecording()
                            }, // Background color
                        contentAlignment = Alignment.Center // Aligns content to the center
                    )
                    {
                        Box(
                            modifier = Modifier
                                .size(50.dp) // Size of the box
                                .clip(CircleShape) // Clips the box into a circle
                                .background(Color.Red),
                            contentAlignment = Alignment.Center
                        ){
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Pause Icon",
                                modifier = Modifier.size(48.dp),
                                tint = Color.White
                            )
                        }
                    }
                }else{
                    Box(
                        modifier = Modifier
                            .size(100.dp) // Size of the box
                            .clip(CircleShape) // Clips the box into a circle
                            .background(Color.White) // Background color
                            .clickable {
                                // Start the foreground service first
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    context.startForegroundService(serviceIntent)
                                }
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    // If audio permission is already granted, launch screen capture intent
                                    screenCaptureLauncher.launch(viewModel.mediaProjectionManager.createScreenCaptureIntent())
                                } else {
                                    // Request audio permission
                                    audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            },
                        contentAlignment = Alignment.Center // Aligns content to the center
                    )
                    {
                        Box(
                            modifier = Modifier
                                .size(50.dp) // Size of the box
                                .clip(CircleShape) // Clips the box into a circle
                                .background(Color.DarkGray)
                        )
                    }
                }
            }
        }
    }
}
