package com.usipipo.vpn.widget

import com.usipipo.vpn.R
import com.usipipo.vpn.domain.SavedConfig
import com.usipipo.vpn.domain.TunnelState
import com.usipipo.vpn.quickconnect.QuickConnectFailure
import com.usipipo.vpn.quickconnect.QuickConnectState
import com.usipipo.vpn.ui.components.flagEmoji

object WidgetStateMapper {

    fun map(state: QuickConnectState): WidgetPresentation {
        val profileName = state.selectedConfig?.name.orEmpty()
        val countryLabel = countryLabel(state.selectedConfig)
        val status = status(state)
        return presentation(
            status = status,
            profileName = profileName,
            countryLabel = countryLabel,
        )
    }

    private fun status(state: QuickConnectState): WidgetStatus {
        val selectedConfigId = state.selectedConfig?.id
        val localFailureMatches = state.failure is QuickConnectFailure.ConfigUnreadable &&
            state.failure.configId == selectedConfigId
        if (localFailureMatches) return WidgetStatus.Failed

        return when (state.tunnel) {
            is TunnelState.Connecting -> WidgetStatus.Connecting
            is TunnelState.Connected -> WidgetStatus.Connected
            is TunnelState.NoResponse -> WidgetStatus.NoResponse
            is TunnelState.Failed -> WidgetStatus.Failed
            TunnelState.Disconnected -> if (selectedConfigId == null) {
                WidgetStatus.NoProfile
            } else {
                WidgetStatus.Disconnected
            }
        }
    }

    private fun presentation(
        status: WidgetStatus,
        profileName: String,
        countryLabel: String,
    ): WidgetPresentation = when (status) {
        WidgetStatus.Disconnected -> WidgetPresentation(
            status = status,
            statusText = R.string.widget_status_disconnected,
            background = R.drawable.widget_bg_disconnected,
            actionIcon = R.drawable.ic_widget_power,
            buttonBackground = R.drawable.widget_button_disconnected,
            showProgress = false,
            profileName = profileName,
            countryLabel = countryLabel,
        )
        WidgetStatus.Connecting -> WidgetPresentation(
            status = status,
            statusText = R.string.widget_status_connecting,
            background = R.drawable.widget_bg_disconnected,
            actionIcon = R.drawable.ic_widget_power,
            buttonBackground = R.drawable.widget_button_disconnected,
            showProgress = true,
            profileName = profileName,
            countryLabel = countryLabel,
        )
        WidgetStatus.Connected -> WidgetPresentation(
            status = status,
            statusText = R.string.widget_status_active,
            background = R.drawable.widget_bg_connected,
            actionIcon = R.drawable.ic_widget_shield_check,
            buttonBackground = R.drawable.widget_button_connected,
            showProgress = false,
            profileName = profileName,
            countryLabel = countryLabel,
        )
        WidgetStatus.NoResponse -> WidgetPresentation(
            status = status,
            statusText = R.string.widget_status_no_response,
            background = R.drawable.widget_bg_failed,
            actionIcon = R.drawable.ic_widget_power_failed,
            buttonBackground = R.drawable.widget_button_failed,
            showProgress = false,
            profileName = profileName,
            countryLabel = countryLabel,
        )
        WidgetStatus.Failed -> WidgetPresentation(
            status = status,
            statusText = R.string.widget_status_failed,
            background = R.drawable.widget_bg_failed,
            actionIcon = R.drawable.ic_widget_power_failed,
            buttonBackground = R.drawable.widget_button_failed,
            showProgress = false,
            profileName = profileName,
            countryLabel = countryLabel,
        )
        WidgetStatus.NoProfile -> WidgetPresentation(
            status = status,
            statusText = R.string.widget_import_profile,
            background = R.drawable.widget_bg_disconnected,
            actionIcon = R.drawable.ic_widget_add,
            buttonBackground = R.drawable.widget_button_disconnected,
            showProgress = false,
            profileName = profileName,
            countryLabel = countryLabel,
        )
    }

    private fun countryLabel(config: SavedConfig?): String {
        val countryCode = config?.countryCode ?: return ""
        val countryName = config?.countryName?.takeIf { it.isNotBlank() } ?: return ""
        val flag = flagEmoji(countryCode)
        return if (flag.isEmpty()) "" else "$flag $countryName"
    }
}
