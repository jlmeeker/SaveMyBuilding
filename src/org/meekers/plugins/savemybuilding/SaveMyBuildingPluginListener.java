package org.meekers.plugins.savemybuilding;

import org.bukkit.event.Listener;

/**
 *
 * @author jaredm
 */
class SaveMyBuildingPluginListener implements Listener {
    
    SaveMyBuilding plugin;    

    public SaveMyBuildingPluginListener(SaveMyBuilding plugin) {
        this.plugin = plugin;
        //this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
}
