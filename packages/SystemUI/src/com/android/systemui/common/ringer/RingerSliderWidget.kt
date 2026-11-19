/*
 * Copyright (C) 2025 AxionOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.systemui.common.ringer

import android.media.AudioManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.gestures.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.android.systemui.res.R
import kotlin.math.roundToInt

@Composable
fun RingerSliderWidget(
    interactor: RingerModeInteractor,
    theme: RingerSliderTheme,
    dimens: RingerSliderDimens,
    modifier: Modifier = Modifier,
    isDozing: Boolean = false,
    border: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {
    val availableModes = remember(interactor) { interactor.getAvailableRingerModes() }
    val numModes = availableModes.size
    val maxOffset = (numModes - 1).coerceAtLeast(1).toFloat()
    val targetPosition by interactor.targetPositionFlow.collectAsState(
        initial = interactor.getTargetPosition(interactor.getCurrentMode())
    )
    val isDndEnabled by interactor.dndMode.collectAsState(initial = interactor.isDndEnabled())
    val activeIndex = targetPosition.roundToInt().coerceIn(0, numModes - 1)
    val tileLabel = stringResource(R.string.quick_settings_sound_label)
    val stateLabel = stringResource(
        when {
            isDndEnabled -> R.string.quick_settings_dnd_label
            availableModes[activeIndex].mode == AudioManager.RINGER_MODE_VIBRATE ->
                R.string.volume_ringer_status_vibrate
            availableModes[activeIndex].mode == AudioManager.RINGER_MODE_SILENT ->
                R.string.volume_ringer_status_silent
            else -> R.string.volume_ringer_status_normal
        }
    )

    var dragOffset by remember { mutableStateOf(targetPosition) }
    var isDragging by remember { mutableStateOf(false) }

    val animatedPosition by animateFloatAsState(
        targetValue = if (isDragging) dragOffset else targetPosition,
        animationSpec = tween(
            durationMillis = 250,
            easing = LinearOutSlowInEasing
        ),
        label = "ringer_position"
    )

    LaunchedEffect(targetPosition) {
        if (!isDragging) dragOffset = targetPosition
    }

    Box(
        modifier = modifier
            .height(dimens.thumbSize)
            .semantics(mergeDescendants = true) {
                contentDescription = tileLabel
                stateDescription = stateLabel
                role = Role.Button
                onClick {
                    if (isDndEnabled) return@onClick false
                    val nextIndex = (activeIndex + 1) % numModes
                    dragOffset = nextIndex.toFloat()
                    interactor.setRingerMode(availableModes[nextIndex].mode)
                    true
                }
                if (onLongClick != null) {
                    onLongClick {
                        onLongClick.invoke()
                        true
                    }
                }
            }
            .background(
                when {
                    isDozing -> Color.Transparent
                    isDndEnabled -> theme.dndBg
                    else -> theme.neutralBg
                },
                RoundedCornerShape(24.dp)
            )
            .clip(RoundedCornerShape(24.dp))
            .then(
                if (isDozing)
                    Modifier.border(theme.dozeStroke, Color.White, RoundedCornerShape(24.dp))
                else border
            )
            .pointerInput(interactor, onLongClick) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        if (isDndEnabled || size.width <= 0) return@detectTapGestures
                        val sectionWidth = size.width / numModes.toFloat()
                        val snappedIndex = (tapOffset.x / sectionWidth).toInt()
                            .coerceIn(0, numModes - 1)
                        dragOffset = snappedIndex.toFloat()
                        interactor.setRingerMode(availableModes[snappedIndex].mode)
                    },
                    onLongPress = {
                        onLongClick?.invoke()
                    }
                )
            }
            .pointerInput(interactor) {
                detectHorizontalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        if (!isDndEnabled) {
                            interactor.setRingerMode(interactor.snapMode(dragOffset))
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                    }
                ) { change, dragAmount ->
                    change.consume()
                    if (isDndEnabled) return@detectHorizontalDragGestures
                    val trackWidth = size.width - dimens.thumbSize.toPx()
                    if (trackWidth <= 0f) return@detectHorizontalDragGestures
                    val pixelPerUnit = trackWidth / maxOffset
                    dragOffset = (dragOffset + (dragAmount / pixelPerUnit))
                        .coerceIn(0f, maxOffset)
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val currentIndex = animatedPosition.roundToInt().coerceIn(0, numModes - 1)
            availableModes.forEachIndexed { index, _ ->
                val dotAlpha by animateFloatAsState(
                    targetValue = if (currentIndex == index) 0f else 0.4f,
                    animationSpec = tween(durationMillis = 200),
                    label = "dot_alpha"
                )
                Box(
                    modifier = Modifier
                        .size(dimens.dotSize)
                        .graphicsLayer { alpha = dotAlpha }
                        .background(
                            if (isDozing) Color.White else if (isDndEnabled) theme.dndIcon else theme.neutralIcon,
                            RoundedCornerShape(50)
                        )
                )
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val totalWidth = maxWidth
            val step = if (numModes > 1) (totalWidth - dimens.thumbSize) / (numModes - 1) else 0.dp
            val thumbOffset = if (isDndEnabled) {
                (totalWidth - dimens.thumbSize) / 2f
            } else {
                step * animatedPosition
            }

            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(dimens.thumbSize)
                    .padding(dimens.thumbPadding)
                    .background(
                        when {
                            isDozing -> Color.Transparent
                            isDndEnabled -> theme.dndBg
                            else -> theme.activeBg
                        },
                        CircleShape
                    )
                    .then(
                        when {
                            isDozing ->
                            Modifier.border(theme.dozeStroke, Color.White, CircleShape)
                            isDndEnabled ->
                                Modifier.border(2.dp, theme.dndBg, CircleShape)
                            else ->
                                Modifier.border(2.dp, theme.activeBg, CircleShape)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDndEnabled) {
                        Icons.Filled.DoNotDisturb
                    } else {
                        availableModes[activeIndex].icon
                    },
                    contentDescription = null,
                    tint = when {
                        isDozing -> Color.White
                        isDndEnabled -> theme.dndIcon
                        else -> theme.activeIcon
                    },
                    modifier = Modifier.size(dimens.iconSize)
                )
            }
        }
    }
}
