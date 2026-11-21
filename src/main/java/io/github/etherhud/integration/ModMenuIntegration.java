package io.github.etherhud.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.etherhud.EtherHud;
import io.github.etherhud.ui.ModuleToggleScreen;

public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new ModuleToggleScreen(parent, EtherHud.instance());
    }
}
