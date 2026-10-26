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
        render(context, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ): Unit {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        render(context, intArrayOf(appWidgetId))
    }

    override fun onEnabled(context: Context): Unit {
        super.onEnabled(context)

        val container = container(context)
        UsipipoWidgetRenderer(context).render(container.quickConnectController.state.value)
    }

    override fun onReceive(context: Context, intent: Intent): Unit {
        if (intent.action != WidgetIntents.ACTION_TOGGLE) {
            super.onReceive(context, intent)
            return
        }

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

    private fun render(context: Context, appWidgetIds: IntArray): Unit {
        val container = container(context)
        UsipipoWidgetRenderer(context).render(
            container.quickConnectController.state.value,
            appWidgetIds,
        )
    }

    private fun container(context: Context): AppContainer =
        (context.applicationContext as UsipipoApp).container
}
