package com.usipipo.vpn.widget

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class WidgetPresentation(
    val status: WidgetStatus,
    @StringRes val statusText: Int,
    @DrawableRes val background: Int,
    @DrawableRes val actionIcon: Int,
    @DrawableRes val buttonBackground: Int,
    val showProgress: Boolean,
    val profileName: String,
    val countryLabel: String,
)
