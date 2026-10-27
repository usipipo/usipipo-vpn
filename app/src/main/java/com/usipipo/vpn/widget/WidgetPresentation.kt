package com.usipipo.vpn.widget

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class WidgetPresentation(
    val status: WidgetStatus,
    @get:StringRes val statusText: Int,
    @get:DrawableRes val background: Int,
    @get:DrawableRes val actionIcon: Int,
    @get:DrawableRes val buttonBackground: Int,
    val showProgress: Boolean,
    val profileName: String,
    val countryLabel: String,
)
