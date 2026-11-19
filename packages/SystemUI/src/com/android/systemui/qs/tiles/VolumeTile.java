/*
 * Copyright (C) 2015 The CyanogenMod Project
 * Copyright (C) 2017 The LineageOS Project
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
package com.android.systemui.qs.tiles;

import static com.android.internal.logging.MetricsLogger.VIEW_UNKNOWN;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;

import androidx.annotation.Nullable;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.animation.Expandable;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.QsEventLogger;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.res.R;
import com.android.systemui.volume.domain.model.VolumePanelRoute;
import com.android.systemui.volume.ui.navigation.VolumeNavigator;

import javax.inject.Inject;

/** Quick settings tile: opens the volume panel */
public class VolumeTile extends QSTileImpl<BooleanState> {

    public static final String TILE_SPEC = "volume_panel";

    private final Handler mMainHandler;
    private final VolumeNavigator mVolumeNavigator;

    @Inject
    public VolumeTile(
            QSHost host,
            QsEventLogger uiEventLogger,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger,
            VolumeNavigator volumeNavigator
    ) {
        super(host, uiEventLogger, backgroundLooper, mainHandler, falsingManager, metricsLogger,
                statusBarStateController, activityStarter, qsLogger);
        mMainHandler = mainHandler;
        mVolumeNavigator = volumeNavigator;
    }

    @Override
    protected void handleClick(@Nullable Expandable expandable) {
        // panel needs main thread
        mMainHandler.post(() -> mVolumeNavigator.openVolumePanel(
                VolumePanelRoute.COMPOSE_VOLUME_PANEL));
    }

    @Override
    @Nullable
    public Intent getLongClickIntent() {
        return null;
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.label = mContext.getString(com.android.internal.R.string.volume_unknown);
        state.icon = maybeLoadResourceIcon(R.drawable.ic_volume_system);
        state.state = Tile.STATE_ACTIVE;
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(com.android.internal.R.string.volume_unknown);
    }

    @Override
    public int getMetricsCategory() {
        return VIEW_UNKNOWN;
    }

    @Override
    public BooleanState newTileState() {
        BooleanState state = new BooleanState();
        state.handlesLongClick = false;
        return state;
    }
}
