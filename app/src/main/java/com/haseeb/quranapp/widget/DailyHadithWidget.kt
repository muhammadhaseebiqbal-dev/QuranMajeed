package com.haseeb.quranapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.haseeb.quranapp.MainActivity
import com.haseeb.quranapp.R
import org.json.JSONArray
import java.io.InputStream
import kotlin.random.Random

class DailyHadithWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.daily_hadith_widget)
        
        val hadith = getRandomHadith(context)
        if (hadith != null) {
            views.setTextViewText(R.id.widget_text, "\"${hadith.second}\"")
            views.setTextViewText(R.id.widget_text_ur, "\"${hadith.third}\"")
            views.setTextViewText(R.id.widget_source, "- ${hadith.first}")
        } else {
            views.setTextViewText(R.id.widget_text, "Could not load hadith.")
            views.setTextViewText(R.id.widget_text_ur, "")
            views.setTextViewText(R.id.widget_source, "")
        }
        
        // Setup click intent to open main activity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getRandomHadith(context: Context): Triple<String, String, String>? {
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
                val textUr = jsonObject.optString("text_ur", "")
                Triple(source, text, textUr)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
