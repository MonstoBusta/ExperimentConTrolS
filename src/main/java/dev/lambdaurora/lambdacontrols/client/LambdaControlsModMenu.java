/*
 * Copyright © 2021 LambdAurora <aurora42lambda@gmail.com>
 *
 * This file is part of ExperimentConTrolS.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.lambdacontrols.client;

import dev.lambdaurora.lambdacontrols.client.gui.LambdaControlsSettingsScreen;
import io.github.prospector.modmenu.api.ConfigScreenFactory;
import io.github.prospector.modmenu.api.ModMenuApi;

/**
 * Represents the API implementation of ModMenu for LambdaControls.
 *
 * @author LambdAurora
 * @version 1.3.0
 * @since 1.1.0
 */
public class LambdaControlsModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new LambdaControlsSettingsScreen(parent, false);
    }
}
