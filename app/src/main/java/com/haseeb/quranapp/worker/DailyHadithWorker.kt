package com.haseeb.quranapp.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.haseeb.quranapp.MainActivity
import com.haseeb.quranapp.R
import org.json.JSONArray
import java.io.InputStream
import kotlin.random.Random

class DailyHadithWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val hadith = getRandomHadith(applicationContext)
            
            if (hadith != null) {
                showNotification(applicationContext, hadith.first, hadith.second)
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun getRandomHadith(context: Context): Pair<String, String>? {
        return try {
            val inputStream: InputStream = context.assets.open("hadiths.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            
            val jsonString = String(buffer, Charsets.UTF_8)
            val jsonArray = JSONArray(jsonString)
            
            if (jsonArray.length() > 0) {
                val randomIndex = Random.nextInt(jsonArray.length())
                val jsonObject = jsonArray.getJSONObject(randomIndex)
                val source = jsonObject.getString("source")
                val text = jsonObject.getString("text")
                Pair(source, text)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun showNotification(context: Context, source: String, text: String) {
        val channelId = "daily_hadith_channel"
        val notificationId = 1001

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the NotificationChannel, but only on API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Daily Hadith"
            val descriptionText = "Notifications for the Daily Hadith"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to open app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build Notification
        val builder = NotificationCompat.Builder(context, channelId)
            // Note: Ideally use a distinct icon for push notifications
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Hadith of the Day ($source)")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }
}
