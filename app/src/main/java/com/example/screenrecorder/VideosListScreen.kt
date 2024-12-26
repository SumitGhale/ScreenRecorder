package com.example.screenrecorder

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Video.Media
import android.util.Log
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.collection.scatterSetOf
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosListScreen(){
    val context = LocalContext.current

    val recordedVideos = getAppRecordedVideos(context)

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            items(recordedVideos.size) { video ->
                VideoItem(video = recordedVideos[video], context)
                Log.d("VideoItem", "Video URI: ${recordedVideos[video].uri}")
            }
        }
    }
}

@Composable
fun VideoItem(video: Video, context: Context) {
    Row(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val thumbnail = remember(video.uri) {
            getVideoThumbnail(context, Uri.parse(video.uri))
        }

        if (thumbnail != null) {
            Image(
                bitmap = thumbnail.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(R.drawable.ic_display),
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.width(16.dp))
        Text(text = video.name, color = Color.White)
    }
}

@SuppressLint("Recycle")
fun getAppRecordedVideos(context: Context): List<Video>{
    val contentResolver = context.contentResolver
    val recordedVideos = mutableListOf<Video>()

    val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
    )

    val selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
    val selectionArgs = arrayOf("Movies/ScreenRecorder%")

    val cursor = contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        "${MediaStore.Video.Media.DATE_ADDED} DESC"
    )

    cursor?.use{
        val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        while (it.moveToNext()){
            val id = it.getLong(idColumn)
            val videoUri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
            recordedVideos.add(Video(it.getString(1), videoUri.toString()))
        }
    }
    return recordedVideos
}

fun getVideoThumbnail(context: Context, videoUri: Uri): Bitmap? {
    return try {
        context.contentResolver.loadThumbnail(
            videoUri,
            Size(100, 100),
            null
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}