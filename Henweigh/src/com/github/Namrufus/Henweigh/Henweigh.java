package com.github.Namrufus.Henweigh;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Henweigh extends JavaPlugin {
	private EncumberingItemManager encumberingItemManager;
	
	public void onEnable() {			
		// perform check for config file, if it doesn't exist, then create it using the default config file
		if (!this.getConfig().isSet("henweigh")) {
			this.saveDefaultConfig();
			this.getLogger().warning("Config did not exist or was invalid, default config saved.");
		}
		this.reloadConfig();
		
		Config.load(this, this.getConfig().getConfigurationSection("henweigh"));
		
		encumberingItemManager = new EncumberingItemManager(this);
		this.getServer().getPluginManager().registerEvents(encumberingItemManager, this);
	}
	
	public void onDisable() {
		// reset the speed to normal for all players
		for (Player player : this.getServer().getOnlinePlayers()) {
			player.setWalkSpeed(0.2f);
		}
		
		encumberingItemManager.stop();
		encumberingItemManager = null;
	}
}
