package com.github.Namrufus.Henweigh;

import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public class Config {
	// the amount that the encumbrance score of a player increases or decreases to match the
	// target encumbrance per tick
	public static double increaseRate;
	public static double decreaseRate;
	
	// tick rate that the encumbrance value is calculated for each player
	public static long calculationPeriod;
	
	// map from block material to terrain object (determines player speed from encumbrance level)
	public static HashMap<Material, Terrain> terrains;
	
	// map specific worn or held items to encumbrance values
	public static HashMap<Material, Double> toolEncumbrance;
	public static HashMap<Material, Double> armorEncumbrance;
	
	// normalized weights for each type of armor
	// used to construct a weighted average of encumbrance
	public static double helmetWeight, chestplateWeight, leggingsWeight, bootsWeight, itemHandWeight;
	
	public static boolean inventoryEncumbranceEnabled;
	// number of "freely" carried items 
	public static int minInventory;
	// maximum number of carried items
	public static int maxInventory;
	
	private static Plugin plugin;
	
	// ========================================================================
	
	private static HashMap<String, Material[]> materialPacks = new HashMap<String, Material[]>();;
	static {
		final Material[] WOOD_TOOLS = {Material.WOOD_AXE, Material.WOOD_HOE, Material.WOOD_PICKAXE, Material.WOOD_SPADE, Material.WOOD_SWORD};
		final Material[] STONE_TOOLS = {Material.STONE_AXE, Material.STONE_HOE, Material.STONE_PICKAXE, Material.STONE_SPADE, Material.STONE_SWORD};
		final Material[] IRON_TOOLS = {Material.IRON_AXE, Material.IRON_HOE, Material.IRON_PICKAXE, Material.IRON_SPADE, Material.IRON_SWORD};
		final Material[] GOLD_TOOLS = {Material.GOLD_AXE, Material.GOLD_HOE, Material.GOLD_PICKAXE, Material.GOLD_SPADE, Material.GOLD_SWORD};
		final Material[] DIAMOND_TOOLS = {Material.DIAMOND_AXE, Material.DIAMOND_HOE, Material.DIAMOND_PICKAXE, Material.DIAMOND_SPADE, Material.DIAMOND_SWORD};
		
		final Material[] LEATHER_ARMOR = {Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS};
		final Material[] CHAINMAIL_ARMOR = {Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS};
		final Material[] IRON_ARMOR = {Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS};
		final Material[] GOLD_ARMOR = {Material.GOLD_HELMET, Material.GOLD_CHESTPLATE, Material.GOLD_LEGGINGS, Material.GOLD_BOOTS};
		final Material[] DIAMOND_ARMOR = {Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS};
		
		materialPacks.put("WOOD_TOOLS", WOOD_TOOLS);
		materialPacks.put("STONE_TOOLS", STONE_TOOLS);
		materialPacks.put("IRON_TOOLS", IRON_TOOLS);
		materialPacks.put("GOLD_TOOLS", GOLD_TOOLS);
		materialPacks.put("DIAMOND_TOOLS", DIAMOND_TOOLS);
		
		materialPacks.put("LEATHER_ARMOR", LEATHER_ARMOR);
		materialPacks.put("CHAINMAIL_ARMOR", CHAINMAIL_ARMOR);
		materialPacks.put("IRON_ARMOR", IRON_ARMOR);
		materialPacks.put("GOLD_ARMOR", GOLD_ARMOR);
		materialPacks.put("DIAMOND_ARMOR", DIAMOND_ARMOR);
	}
	
	// ========================================================================
	
	public static void load(Plugin _plugin, ConfigurationSection conf) {
		plugin = _plugin;
	
		inventoryEncumbranceEnabled = conf.getBoolean("inventory_encumbrance_enabled");
		minInventory = conf.getInt("min_inventory");
		maxInventory = conf.getInt("max_inventory");
		
		terrains = new HashMap<Material, Terrain>();
		
		loadTerrain(conf.getConfigurationSection("terrain"));
		
		toolEncumbrance = new HashMap<Material, Double>();
		armorEncumbrance = new HashMap<Material, Double>();
		
		loadEncumberingMaterials(armorEncumbrance, conf.getConfigurationSection("armor_encumbrance"));
		loadEncumberingMaterials(toolEncumbrance, conf.getConfigurationSection("tool_encumbrance"));
		
		helmetWeight = conf.getDouble("helmet_weight");
		chestplateWeight = conf.getDouble("chestplate_weight");
		leggingsWeight = conf.getDouble("leggings_weight");
		bootsWeight = conf.getDouble("boots_weight");
		itemHandWeight = conf.getDouble("item_hand_weight");
		
		double sumWeight = helmetWeight + chestplateWeight + leggingsWeight + bootsWeight + itemHandWeight;
		helmetWeight /= sumWeight;
		chestplateWeight /= sumWeight;
		leggingsWeight /= sumWeight;
		bootsWeight /= sumWeight;
		itemHandWeight /= sumWeight;
	}
		
	// load the material encumbrance from the indicated configuration section to the indicated map from material to double
	private static void loadTerrain(ConfigurationSection conf) {
		for (String terrainName : conf.getKeys(false)) {
			ConfigurationSection terrainConf = conf.getConfigurationSection(terrainName);

			double a = terrainConf.getDouble("a");
			double e = terrainConf.getDouble("e");
			
			Terrain terrain = new Terrain(a, e);
			
			for (String materialName : terrainConf.getStringList("materials")) {
				// if the itemName was not a key word, then attempt to interpret it as a material name
				Material material = Material.getMaterial(materialName);
				if (material == null) {
					plugin.getLogger().warning("while configuring terrain material "+materialName+" does not exist.");
					continue;
				}
				
				terrains.put(material, terrain);
			}
		}
	}
	// load the material encumberance from the indicated configuration section to the indicated map from material to double
	private static void loadEncumberingMaterials(HashMap<Material, Double> materialEncumbrance, ConfigurationSection conf) {
		for (String itemName : conf.getKeys(false)) {
			// take the key as a material name, 
			Material[] materials = {};
			if (materialPacks.containsKey(itemName)) {
				// if the item name is a compound material, apply the encumbrance to all materials associated
				materials = materialPacks.get(itemName);
			}
			else {
				// if the itemName was not a key word, then attempt to interpret it as a material name
				Material material = Material.getMaterial(itemName);
				if (material == null) {
					plugin.getLogger().warning("while configuring encumbrance material "+itemName+" does not exist.");
					continue;
				}
				materials = new Material[1];
				materials[0] = material;
			}
			
			// set the indicated encumbrance value for the indicated material
			Double encumbrance = new Double(conf.getDouble(itemName));
			for (Material material : materials) {
				materialEncumbrance.put(material, encumbrance);
			}
		}
	}
}
