package com.xlrr.roambendom.ui.loginScreen

import androidx.lifecycle.ViewModel
import coil3.PlatformContext

expect class LoginViewModel : ViewModel {
    constructor()
    fun launchBrowser(platformContext: PlatformContext)
}