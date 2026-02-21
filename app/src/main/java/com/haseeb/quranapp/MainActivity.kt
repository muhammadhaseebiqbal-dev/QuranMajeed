package com.haseeb.quranapp

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.haseeb.quranapp.worker.DailyHadithWorker
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.haseeb.quranapp.ui.home.MainScreen
import com.haseeb.quranapp.ui.navigation.Screen
import com.haseeb.quranapp.ui.reader.ReaderScreen
import com.haseeb.quranapp.ui.settings.SettingsScreen
import com.haseeb.quranapp.ui.theme.QuranAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        
        // Schedule daily local notification
        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyHadithWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(2, TimeUnit.HOURS) // Show the first one slightly offset if possible
            .build()
            
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyHadithRoutine",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )
        
        // Start AudioService to ensure it's ready
        val intent = Intent(this, com.haseeb.quranapp.service.AudioService::class.java)
        startForegroundService(intent) // Or startService depending on API level

        setContent {
            QuranAppTheme {
                val navController = rememberNavController()
                
                NavHost(navController = navController, startDestination = Screen.Home.route) {
                    composable(Screen.Home.route) {
                        MainScreen(
                            onSurahClick = { surahId ->
                                navController.navigate(Screen.Reader.createRoute(surahId))
                            },
                            onJuzClick = { juzId ->
                                navController.navigate(Screen.JuzReader.createRoute(juzId))
                            },
                            onResumeClick = { surahId, ayahNum ->
                                navController.navigate(Screen.Reader.createRoute(surahId, ayahNum))
                            },
                            onAskClick = {
                                // Removed
                            },
                            onSettingsClick = {
                                navController.navigate(Screen.Settings.route)
                            }
                        )
                    }
                    
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    
                    composable(
                        route = Screen.Reader.route,
                        arguments = listOf(
                            navArgument("surahId") { type = NavType.IntType },
                            navArgument("scrollToAyah") { type = NavType.IntType; defaultValue = -1 }
                        )
                    ) { backStackEntry ->
                        val scrollToAyah = backStackEntry.arguments?.getInt("scrollToAyah") ?: -1
                        ReaderScreen(
                            onBackClick = { navController.popBackStack() },
                            scrollToAyah = if (scrollToAyah != -1) scrollToAyah else null
                        )
                    }

                    composable(
                        route = Screen.JuzReader.route,
                        arguments = listOf(
                            navArgument("juzId") { type = NavType.IntType },
                            navArgument("scrollToAyah") { type = NavType.IntType; defaultValue = -1 }
                        )
                    ) { backStackEntry ->
                        val scrollToAyah = backStackEntry.arguments?.getInt("scrollToAyah") ?: -1
                        ReaderScreen(
                            onBackClick = { navController.popBackStack() },
                            scrollToAyah = if (scrollToAyah != -1) scrollToAyah else null
                        )
                    }
                }
            }
        }
    }
}
