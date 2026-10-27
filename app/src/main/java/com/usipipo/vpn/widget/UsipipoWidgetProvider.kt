package com.usipipo.vpn.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.usipipo.vpn.AppContainer
import com.usipipo.vpn.UsipipoApp
import kotlinx.coroutines.launch

class UsipipoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ): Unit {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        container(context).widgetUpdater.refreshNow()
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ): Unit {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        container(context).widgetUpdater.refreshNow()
    }

    override fun onEnabled(context: Context): Unit {
        super.onEnabled(context)
        container(context).widgetUpdater.refreshNow()
    }

    override fun onReceive(context: Context, intent: Intent): Unit {
        when (intent.action) {
            WidgetIntents.ACTION_TOGGLE -> toggle(context)

            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED ->
                container(context).widgetUpdater.refreshNow()

            else -> super.onReceive(context, intent)
        }
    }

    private fun toggle(context: Context) {
        val pendingResult = goAsync()
        val container = container(context)
        container.applicationScope.launch {
            try {
                QuickActionRouter(
                    context.applicationContext,
                    container.quickConnectController,
                ).toggle()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun container(context: Context): AppContainer =
        (context.applicationContext as UsipipoApp).container
}
