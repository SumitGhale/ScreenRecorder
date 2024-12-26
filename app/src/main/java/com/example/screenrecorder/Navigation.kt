package com.example.screenrecorder

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun Navigation(navController: NavHostController){
    NavHost(navController = navController,
        startDestination = "home"){
        composable("home"){
            HomeScreen(navController)
        }
        composable("video_list") {
            VideosListScreen()
        }
    }
}