/*
 * Copyright (C) 2017-2018 Paranoid Android
 * Copyright (C) 2022 FlamingoOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.policy

import android.content.Context
import android.util.Log

import com.android.systemui.res.R
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.doze.util.getBurnInOffset
import com.android.systemui.statusbar.phone.PhoneStatusBarView
import com.android.systemui.statusbar.policy.ConfigurationController
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener

import javax.inject.Inject

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val UPDATE_INTERVAL = 1000 * 10L

private val TAG = BurnInProtectionController::class.simpleName

@SysUISingleton
class BurnInProtectionController @Inject constructor(
    private val context: Context,
    configurationController: ConfigurationController,
) : ConfigurationListener {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val shiftEnabled = context.resources.getBoolean(com.android.internal.R.bool.config_enableBurnInProtection)

    private var phoneStatusBarView: PhoneStatusBarView? = null

    private var shiftJob: Job? = null

    private var maxStatusBarOffsetX = 0
    private var maxStatusBarOffsetY = 0

    private var statusBarOffset = Offset.Zero

    init {
        logD {
            "shiftEnabled = $shiftEnabled"
        }
        configurationController.addCallback(this)
        loadResources()
    }

    private fun loadResources()  {
        with(context.resources) {
            maxStatusBarOffsetX = minOf(
                getDimensionPixelSize(R.dimen.status_bar_padding_start),
                getDimensionPixelSize(R.dimen.status_bar_padding_end)
            ) / 2
            maxStatusBarOffsetY = getDimensionPixelSize(R.dimen.status_bar_offset_max_y) / 2
        }
        logD {
            "maxStatusBarOffsetX = $maxStatusBarOffsetX, maxStatusBarOffsetY = $maxStatusBarOffsetY"
        }
    }

    fun setPhoneStatusBarView(phoneStatusBarView: PhoneStatusBarView?) {
        this.phoneStatusBarView = phoneStatusBarView
    }

    fun startShiftTimer() {
        if (!shiftEnabled || (shiftJob?.isActive == true)) return
        shiftJob = coroutineScope.launch {
            while (isActive) {
                val sbOffset = Offset(
                    centeredBurnInOffset(maxStatusBarOffsetX, xAxis = true),
                    centeredBurnInOffset(maxStatusBarOffsetY, xAxis = false)
                )
                logD {
                    "new offsets: sbOffset = $sbOffset"
                }
                updateViews(sbOffset)
                delay(UPDATE_INTERVAL)
            }
        }
        logD {
            "Started shift job"
        }
    }

    private fun updateViews(sbOffset: Offset) {
        if (sbOffset != statusBarOffset) {
            logD {
                "Translating statusbar"
            }
            phoneStatusBarView?.offsetStatusBar(sbOffset)
            statusBarOffset = sbOffset
        }
    }

    fun stopShiftTimer() {
        if (!shiftEnabled || (shiftJob?.isActive != true)) return
        logD {
            "Cancelling shift job"
        }
        shiftJob?.cancel()
        shiftJob = null
        updateViews(Offset.Zero)
    }

    override fun onDensityOrFontScaleChanged() {
        logD {
            "onDensityOrFontScaleChanged"
        }
        loadResources()
    }
}

private fun centeredBurnInOffset(maxOffset: Int, xAxis: Boolean): Int {
    return maxOffset - getBurnInOffset(
        amplitude = maxOffset * 2,
        xAxis = xAxis
    )
}

private inline fun logD(crossinline msg: () -> String) {
    if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, msg())
    }
}

data class Offset(
    val x: Int,
    val y: Int
) {
    companion object {
        val Zero = Offset(0, 0)
    }
}
