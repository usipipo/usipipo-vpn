package com.usipipo.vpn.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.usipipo.vpn.R
import com.usipipo.vpn.quickconnect.QuickConnectState

class UsipipoWidgetRenderer(context: Context) {

    private val appContext: Context = context.applicationContext
    private val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(appContext)

    fun render(state: QuickConnectState, appWidgetIds: IntArray): Unit {
        if (appWidgetIds.isEmpty()) return

        val presentation = WidgetStateMapper.map(state)
        appWidgetIds.forEach { appWidgetId -> renderWidget(appWidgetId, presentation) }
    }

    fun render(state: QuickConnectState): Unit {
        val component = ComponentName(appContext, UsipipoWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(component)
        if (appWidgetIds.isEmpty()) return

        render(state, appWidgetIds)
    }

    private fun renderWidget(appWidgetId: Int, presentation: WidgetPresentation): Unit {
        val remoteViews = RemoteViews(appContext.packageName, layoutFor(appWidgetId))

        remoteViews.setInt(R.id.widget_root, "setBackgroundResource", presentation.background)
        remoteViews.setTextViewText(
            R.id.widget_status,
            appContext.getString(presentation.statusText),
        )
        remoteViews.setImageViewResource(R.id.widget_action, presentation.actionIcon)
        remoteViews.setInt(
            R.id.widget_action,
            "setBackgroundResource",
            presentation.buttonBackground,
        )
        remoteViews.setViewVisibility(
            R.id.widget_progress,
            if (presentation.showProgress) View.VISIBLE else View.GONE,
        )
        remoteViews.setViewVisibility(
            R.id.widget_action,
            if (presentation.showProgress) View.INVISIBLE else View.VISIBLE,
        )
        setOptionalText(remoteViews, R.id.widget_profile_name, presentation.profileName)
        setOptionalText(remoteViews, R.id.widget_country, presentation.countryLabel)
        applyTextColors(remoteViews, presentation.status)
        remoteViews.setOnClickPendingIntent(
            R.id.widget_action,
            togglePendingIntent(appWidgetId),
        )

        appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
    }

    private fun layoutFor(appWidgetId: Int): Int {
        val minWidth = appWidgetManager
            .getAppWidgetOptions(appWidgetId)
            .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        return if (minWidth >= WIDE_LAYOUT_MIN_WIDTH_DP) {
            R.layout.widget_usipipo_wide
        } else {
            R.layout.widget_usipipo
        }
    }

    private fun setOptionalText(
        remoteViews: RemoteViews,
        viewId: Int,
        text: String,
    ): Unit {
        if (text.isEmpty()) {
            remoteViews.setViewVisibility(viewId, View.GONE)
        } else {
            remoteViews.setTextViewText(viewId, text)
            remoteViews.setViewVisibility(viewId, View.VISIBLE)
        }
    }

    private fun applyTextColors(
        remoteViews: RemoteViews,
        status: WidgetStatus,
    ): Unit {
        val primaryColor = when (status) {
            WidgetStatus.Connected -> R.color.widget_text_on_connected
            WidgetStatus.Failed,
            WidgetStatus.NoResponse,
            -> R.color.widget_text_failed
            WidgetStatus.Disconnected,
            WidgetStatus.Connecting,
            WidgetStatus.NoProfile,
            -> R.color.widget_text_primary
        }
        val secondaryColor = when (status) {
            WidgetStatus.Connected -> R.color.widget_text_on_connected
            WidgetStatus.Failed,
            WidgetStatus.NoResponse,
            -> R.color.widget_text_failed
            WidgetStatus.Disconnected,
            WidgetStatus.Connecting,
            WidgetStatus.NoProfile,
            -> R.color.widget_text_secondary
        }
        remoteViews.setTextColor(
            R.id.widget_profile_name,
            appContext.getColor(primaryColor),
        )
        remoteViews.setTextColor(
            R.id.widget_country,
            appContext.getColor(secondaryColor),
        )
    }

    private fun togglePendingIntent(appWidgetId: Int): PendingIntent {
        val intent = Intent(appContext, UsipipoWidgetProvider::class.java).apply {
            action = WidgetIntents.ACTION_TOGGLE
        }
        return PendingIntent.getBroadcast(
            appContext,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val WIDE_LAYOUT_MIN_WIDTH_DP: Int = 250
    }
}
