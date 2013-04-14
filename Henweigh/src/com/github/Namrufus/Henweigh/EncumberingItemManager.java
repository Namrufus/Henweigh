package com.github.Namrufus.Henweigh;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class EncumberingItemManager implements Listener {
	private Plugin plugin;
	private HashMap<String, EncumberedPlayer> playerData;
	
	private Queue<String> pendingPlayers;
	BukkitTask calculateTask;
	
	// the static Config must be populated before creating this object
	public EncumberingItemManager(Plugin plugin) {
		this.plugin = plugin;
		
		playerData = new HashMap<String, EncumberedPlayer>();
		
		pendingPlayers = new LinkedList<String>();
		calculateTask = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
		    @Override  
		    public void run() {
		    	calculateEncumbrance();
		    }
		}, 1, 1);
	}
	
	public void stop() {
	}
	
	// ========================================================================
	
	@EventHandler
	public void onPlayerLogout(PlayerQuitEvent  event) {
		// reset the speed to normal for the player
		event.getPlayer().setWalkSpeed(0.2f);
	}
	
	// ========================================================================
	
	@EventHandler
	public void onPlayerLoginEvent(PlayerLoginEvent event) {
		pendingPlayers.add(event.getPlayer().getName());
	}
	@EventHandler
	public void onPlayerItemHeldEvent(PlayerItemHeldEvent event) {
		pendingPlayers.add(event.getPlayer().getName());
	}
	@EventHandler
	public void onPlayerPickupItemEvent(PlayerPickupItemEvent event) {
		pendingPlayers.add(event.getPlayer().getName());
	}
	@EventHandler
	public void onInventoryClickEvent(InventoryClickEvent event) {
		pendingPlayers.add(event.getWhoClicked().getName());
	}
	
	private void calculateEncumbrance() {
		while (!pendingPlayers.isEmpty()) {
			Player player = plugin.getServer().getPlayerExact(pendingPlayers.remove());
			if (player == null)
				return;
			EncumberedPlayer encumberedPlayer = getEncumberedPlayer(player);
			encumberedPlayer.setTargetEncumbrance(getNetEncumbrance(player));
		}
	}
	private EncumberedPlayer getEncumberedPlayer(Player player) {
		if (playerData.containsKey(player.getName()))
			return  playerData.get(player.getName());
		else {
			EncumberedPlayer encumberedPlayer = new EncumberedPlayer(1.0);
			playerData.put(player.getName(), encumberedPlayer);
			return encumberedPlayer;
		}
	}

	// ========================================================================
	
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent  event) {
    	Player player = event.getPlayer();
    	EncumberedPlayer encumberedPlayer = getEncumberedPlayer(player);
    	
    	// the material that the player is walking on
    	Material terrainMaterial;
    	
    	// first check if the player is standing in snow,
    	// then if the block directly below the player is not AIR
    	// then the below another block below the player (jumping)
    	Block snowBlock = player.getLocation().getBlock();	
    	if ((terrainMaterial = snowBlock.getType()) != Material.SNOW) {
    		// get the block below foot, (displace by 0.5 vertically for half-blocks)
    		Block upperBlock = player.getLocation().add(new Vector(0, 0.5, 0)).getBlock().getRelative(0,-1,0);
	    	if ((terrainMaterial = upperBlock.getType()) == Material.AIR) {
				Block lowerBlock = upperBlock.getRelative(0,-1,0);
				terrainMaterial = lowerBlock.getType();
	    	}
    	}

    	// get the terrain configuration for the given material and determine the player's speed 
    	// based on the terrain and the 
    	Terrain terrain = Config.terrains.get(terrainMaterial);
    	if (terrain == null)
    		return; /*if that terrain does not exist, then nothing is to be done*/
    	double multiplier = terrain.speedModifier(encumberedPlayer.getEncumbrance());
    	player.setWalkSpeed((float) multiplier * 0.2f/*0.2 = normal speed*/);
    	
    	// cause the hunger level of the player to go down while jumping on hard terrain,
    	// in order to counteract the fact that sprint jumping still works
    	// the amount of hunger points taken per jump depends on the speed multiplier
    	// if the player is moving upwards while above a known terrain (no air) (about 4 of these events per jump)
    	if (!player.isSneaking() && player.getFoodLevel() > 0 && event.getFrom().getY() < event.getTo().getY()) {
    		double chance = 1.0 - multiplier;
    		// sprinting takes much more energy (sprint-jumping is the main problem)
    		if (!player.isSprinting())
    			chance /= 16.0;
    		else
    			chance /= 4.0;
    		
			if (Math.random() < chance)
				player.setFoodLevel(player.getFoodLevel() - 1);
    	}
    }
	
    // get the minimum encumbrance value of either the equipment or the inventory and use that
	private double getNetEncumbrance(Player player) {
		double equipmentEncumbrance = getEquipmentEncumbrance(player);
		
		if (Config.inventoryEncumbranceEnabled) {
			return Math.min(equipmentEncumbrance, getInventoryEncumbrance(player));
		} /*else*/
		
		return equipmentEncumbrance;
	}
	
	// get a player's encumbrance score based on their current armor and held items
	// this is a weighted average of the encumbrance scores of the individual items
	private double getEquipmentEncumbrance(Player player) {
		double encumbrance = 0.0;
		
		PlayerInventory inventory = player.getInventory();
		
		Material helmet = Material.AIR;
		Material chestplate = Material.AIR;
		Material leggings = Material.AIR;
		Material boots = Material.AIR;
		Material hand = Material.AIR;
		
		if (inventory.getHelmet() != null)
			helmet = inventory.getHelmet().getType();
		if (inventory.getChestplate() != null)
			chestplate = inventory.getChestplate().getType();
		if (inventory.getLeggings() != null)
			leggings = inventory.getLeggings().getType();
		if (inventory.getBoots() != null)
			boots = inventory.getBoots().getType();
		if (inventory.getItemInHand() != null)
			hand = inventory.getItemInHand().getType();
		
		encumbrance += Config.helmetWeight * getEncumbrance(Config.armorEncumbrance, helmet);
		encumbrance += Config.chestplateWeight * getEncumbrance(Config.armorEncumbrance, chestplate);
		encumbrance += Config.leggingsWeight * getEncumbrance(Config.armorEncumbrance, leggings);
		encumbrance += Config.bootsWeight * getEncumbrance(Config.armorEncumbrance, boots);
		encumbrance += Config.itemHandWeight * getEncumbrance(Config.toolEncumbrance, hand);
		
		return encumbrance;
	}
	// get the encumbrance value of an item, based on the given map from item to encumbrance value
	private double getEncumbrance(HashMap<Material, Double> materialEncumbrance, Material material) {
		if (materialEncumbrance.containsKey(material))
			return materialEncumbrance.get(material);
		
		// if the material is not in the list of encumbering materials
		// the player should not be penalized for holding it
		return 1.0;
	}
	
	// get the player's encumbrance score based on the number of items held
	private double getInventoryEncumbrance(Player player) {
		ItemStack[] items = player.getInventory().getContents();
		
		// get the total number of items in the player's inventory
		int count = 0;
		for (ItemStack item : items) {
			if (item == null)
				continue;
			count += item.getAmount();
		}
		
		if (count < Config.minInventory)
			return 1.0;
		else if (count > Config.maxInventory)
			return 0.0;
		// else max <= count <= min
		
		// linear interpolation from (min, 1) to (max, 0)
		count -= Config.minInventory;
		return 1.0 - ((double)count / (double)(Config.maxInventory-Config.minInventory));
	}
}

