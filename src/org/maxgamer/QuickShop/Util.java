package org.maxgamer.QuickShop;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionType;

public class Util{
	private static HashSet<Material> tools = new HashSet<Material>();
	
	static{
		tools.add(Material.BOW);
		tools.add(Material.SHEARS);
		tools.add(Material.FISHING_ROD);
		tools.add(Material.FLINT_AND_STEEL);

		tools.add(Material.CHAINMAIL_BOOTS);
		tools.add(Material.CHAINMAIL_CHESTPLATE);
		tools.add(Material.CHAINMAIL_HELMET);
		tools.add(Material.CHAINMAIL_LEGGINGS);
		
		tools.add(Material.WOOD_AXE);
		tools.add(Material.WOOD_HOE);
		tools.add(Material.WOOD_PICKAXE);
		tools.add(Material.WOOD_SPADE);
		tools.add(Material.WOOD_SWORD);
		
		tools.add(Material.LEATHER_BOOTS);
		tools.add(Material.LEATHER_CHESTPLATE);
		tools.add(Material.LEATHER_HELMET);
		tools.add(Material.LEATHER_LEGGINGS);
		
		tools.add(Material.DIAMOND_AXE); 
		tools.add(Material.DIAMOND_HOE);
		tools.add(Material.DIAMOND_PICKAXE);
		tools.add(Material.DIAMOND_SPADE);
		tools.add(Material.DIAMOND_SWORD);

		tools.add(Material.DIAMOND_BOOTS);
		tools.add(Material.DIAMOND_CHESTPLATE);
		tools.add(Material.DIAMOND_HELMET);
		tools.add(Material.DIAMOND_LEGGINGS);
		tools.add(Material.STONE_AXE); 
		tools.add(Material.STONE_HOE);
		tools.add(Material.STONE_PICKAXE);
		tools.add(Material.STONE_SPADE);
		tools.add(Material.STONE_SWORD);

		tools.add(Material.GOLD_AXE); 
		tools.add(Material.GOLD_HOE);
		tools.add(Material.GOLD_PICKAXE);
		tools.add(Material.GOLD_SPADE);
		tools.add(Material.GOLD_SWORD);

		tools.add(Material.GOLD_BOOTS);
		tools.add(Material.GOLD_CHESTPLATE);
		tools.add(Material.GOLD_HELMET);
		tools.add(Material.GOLD_LEGGINGS);
		tools.add(Material.IRON_AXE); 
		tools.add(Material.IRON_HOE);
		tools.add(Material.IRON_PICKAXE);
		tools.add(Material.IRON_SPADE);
		tools.add(Material.IRON_SWORD);

		tools.add(Material.IRON_BOOTS);
		tools.add(Material.IRON_CHESTPLATE);
		tools.add(Material.IRON_HELMET);
		tools.add(Material.IRON_LEGGINGS);
	}
	
	public static void parseColours(YamlConfiguration config){
		Set<String> keys = config.getKeys(true);
		
		for(String key : keys){
			String filtered = config.getString(key);
			if(filtered.startsWith("MemorySection")){
				continue;
			}
			char[] chars = filtered.toCharArray();
			
			for(int i = 0; i < chars.length - 1; i++){
				if(chars[i] == '&' 
					&& ((chars[i+1] >= '0' && chars[i+1] <= '9')
						|| (chars[i+1] >= 'a' && chars[i+1] <= 'f'))){
					chars[i] = ChatColor.COLOR_CHAR;
				}
			}
			
			filtered = new String(chars);
			config.set(key, filtered);
		}
	}
	
	 /**
	  * Gets the percentage (Without trailing %) damage on a tool.
	  * @param item The ItemStack of tools to check
	  * @return The percentage 'health' the tool has. (Opposite of total damage)
	  */
	public static String getToolPercentage(ItemStack item){
		double dura = item.getDurability();
		double max = item.getType().getMaxDurability();
		
		DecimalFormat formatter = new DecimalFormat("0");
		return formatter.format((1 - dura/max)* 100.0);
	}

	/**
	 * Converts a string into an item from the database.
	 * @param itemString The database string.  Is the result of makeString(ItemStack item).
	 * @return A new itemstack, with the properties given in the string
	 */
	public static ItemStack makeItem(String itemString){
		String[] itemInfo = itemString.split(":");
		
		ItemStack item = new ItemStack(Material.getMaterial(itemInfo[0]));
		MaterialData data = new MaterialData(Integer.parseInt(itemInfo[1]));
		item.setData(data);
		item.setDurability( Short.parseShort(itemInfo[2]));
		item.setAmount(Integer.parseInt(itemInfo[3]));
		
		for(int i = 4; i < itemInfo.length; i = i + 2){
			int level = Integer.parseInt(itemInfo[i+1]);
			
			Enchantment ench = Enchantment.getByName(itemInfo[i]);
			if(ench == null) continue; //Invalid
			if (ench.canEnchantItem(item)){
				if(level <= 0) continue;
				level = Math.min(ench.getMaxLevel(), level);
				
				item.addEnchantment(ench, level);
			}
			
		}
		return item;
	}
	
	/**
	 * Converts an itemstack into a string for database storage.  See makeItem(String itemString) for 
	 * reversing this.
	 * @param item The item to model it off of.
	 * @return A new string with the properties of the item.
	 */
	public static String makeString(ItemStack item){
		String itemString = item.getType().toString() + ":" + item.getData().getData() + ":" + item.getDurability() + ":" + item.getAmount();
		
		for(Entry<Enchantment, Integer> ench : item.getEnchantments().entrySet()){
			itemString += ":" + ench.getKey().getName() + ":" + ench.getValue();
		}
		return itemString;
	}
	
	/**
	 * Converts a given material and data value into a format similar to Material.<?>.toString().
	 * Upper case, with underscores.  Includes material name in result.
	 * @param mat The base material.
	 * @param damage The durability/damage of the item.
	 * @return A string with the name of the item.
	 */
	public static String getDataName(Material mat, short damage){
		int id = mat.getId();
		switch(id){
		case 35: 
			switch((int) damage){
			case 0: return "WHITE_WOOL";
			case 1: return "ORANGE_WOOL";
			case 2: return "MAGENTA_WOOL";
			case 3: return "LIGHT_BLUE_WOOL";
			case 4: return "YELLOW_WOOL";
			case 5: return "LIME_WOOL";
			case 6: return "PINK_WOOL";
			case 7: return "GRAY_WOOL";
			case 8: return "LIGHT_GRAY_WOOL";
			case 9: return "CYAN_WOOL";
			case 10: return "PURPLE_WOOL";
			case 11: return "BLUE_WOOL";
			case 12: return "BROWN_WOOL";
			case 13: return "GREEN_WOOL";
			case 14: return "RED_WOOL";
			case 15: return "BLACK_WOOL";
			}
			return mat.toString();
		case 351:
			switch((int) damage){
			case 0: return "INK_SAC";
			case 1: return "ROSE_RED";
			case 2: return "CACTUS_GREEN";
			case 3: return "COCOA_BEANS";
			case 4: return "LAPIS_LAZULI";
			case 5: return "PURPLE_DYE";
			case 6: return "CYAN_DYE";
			case 7: return "LIGHT_GRAY_DYE";
			case 8: return "GRAY_DYE";
			case 9: return "PINK_DYE";
			case 10: return "LIME_DYE";
			case 11: return "DANDELION_YELLOW";
			case 12: return "LIGHT_BLUE_DYE";
			case 13: return "MAGENTA_DYE";
			case 14: return "ORANGE_DYE";
			case 15: return "BONE_MEAL";
			}
			return mat.toString();
		case 98:
			switch((int) damage){
			case 0: return "STONE_BRICKS";
			case 1: return "MOSSY_STONE_BRICKS";
			case 2: return "CRACKED_STONE_BRICKS";
			case 3: return "CHISELED_STONE_BRICKS";
			}
			return mat.toString();
		case 373:
			//Special case
			if(damage == 64) return "MUNDANE_POTION";
			Potion pot;
			try{
				pot = Potion.fromDamage(damage);
			}
			catch(IllegalArgumentException ex){
				pot = new Potion(PotionType.WATER);
			}
			
			String prefix = "";
			String suffix = "";
			if(pot.getLevel() > 0) suffix += "_" + pot.getLevel();
			if(pot.hasExtendedDuration()) prefix += "EXTENDED_";
			if(pot.isSplash()) prefix += "SPLASH_";
			
			switch((int) pot.getNameId()){
			case 0: return prefix + "WATER_BOTTLE" + suffix;
			case 1: return prefix + "POTION_OF_REGENERATION" + suffix;
			case 2: return prefix + "POTION_OF_SWIFTNESS" + suffix;
			case 3: return prefix + "POTION_OF_FIRE_RESISTANCE" + suffix;
			case 4: return prefix + "POTION_OF_POISON" + suffix;
			case 5: return prefix + "POTION_OF_HEALING" + suffix;
			case 6: return prefix + "CLEAR_POTION" + suffix;
			case 7: return prefix + "CLEAR_POTION" + suffix;
			case 8: return prefix + "POTION_OF_WEAKNESS" + suffix;
			case 9: return prefix + "POTION_OF_STRENGTH" + suffix;
			case 10: return prefix + "POTION_OF_SLOWNESS" + suffix;
			case 11: return prefix + "DIFFUSE_POTION" + suffix;
			case 12: return prefix + "POTION_OF_HARMING" + suffix;
			case 13: return prefix + "ARTLESS_POTION" + suffix;
			case 14: return prefix + "THIN_POTION" + suffix;
			case 15: return prefix + "THIN_POTION" + suffix;
			case 16: return prefix + "AWKWARD_POTION" + suffix;
			case 32: return prefix + "THICK_POTION" + suffix;
			}
			return mat.toString();
		case 6:
			switch((int) damage){
			case 0: return "OAK_SAPLING";
			case 1: return "PINE_SAPLING";
			case 2: return "BIRCH_SAPLING";
			case 3: return "JUNGLE_TREE_SAPLING";
			}
			return mat.toString();
		
		case 5:
			switch((int) damage){
			case 0: return "OAK_PLANKS";
			case 1: return "PINE_PLANKS";
			case 2: return "BIRCH_PLANKS";
			case 3: return "JUNGLE_PLANKS";
			}
			return mat.toString();
		case 17:
			switch(damage){
			case 0: return "OAK_LOG";
			case 1: return "PINE_LOG";
			case 2: return "BIRCH_LOG";
			case 3: return "JUNGLE_LOG";
			}
			return mat.toString();
		case 18:
			damage = (short) (damage%4);
			switch(damage){
			case 0: return "OAK_LEAVES";
			case 1: return "PINE_LEAVES";
			case 2: return "BIRCH_LEAVES";
			case 3: return "JUNGLE_LEAVES";
			}
		case 263:
			switch(damage){
			case 0: return "COAL";
			case 1: return "CHARCOAL";
			}
			return mat.toString();
		case 24:
			switch((int) damage){
			case 0: return "SANDSTONE";
			case 1: return "CHISELED_SANDSTONE";
			case 2: return "SMOOTH_SANDSTONE";
			}
			return mat.toString();
		case 31:
			switch((int) damage){
			case 0: return "DEAD_SHRUB";
			case 1: return "TALL_GRASS";
			case 2: return "FERN";
			}
			return mat.toString();
		case 44:
			switch((int) damage){
			case 0: return "STONE_SLAB";
			case 1: return "SANDSTONE_SLAB";
			case 2: return "WOODEN_SLAB";
			case 3: return "COBBLESTONE_SLAB";
			case 4: return "BRICK_SLAB";
			case 5: return "STONE_BRICK_SLAB";
			}
			return mat.toString();
		case 383:
			switch((int) damage){
			case 50: return "CREEPER_EGG";
			case 51: return "SKELETON_EGG";
			case 52: return "SPIDER_EGG";
			case 53: return "GIANT_EGG";
			case 54: return "ZOMBIE_EGG";
			case 55: return "SLIME_EGG";
			case 56: return "GHAST_EGG";
			case 57: return "ZOMBIE_PIGMAN_EGG";
			case 58: return "ENDERMAN_EGG";
			case 59: return "CAVE_SPIDER_EGG";
			case 60: return "SILVERFISH_EGG";
			case 61: return "BLAZE_EGG";
			case 62: return "MAGMA_CUBE_EGG";
			case 63: return "ENDER_DRAGON_EGG";
			case 90: return "PIG_EGG";
			case 91: return "SHEEP_EGG";
			case 92: return "COW_EGG";
			case 93: return "CHICKEN_EGG";
			case 94: return "SQUID_EGG";
			case 95: return "WOLF_EGG";
			case 96: return "MOOSHROOM_EGG";
			case 97: return "SNOW_GOLEM_EGG";
			case 98: return "OCELOT_EGG";
			case 99: return "IRON_GOLEM_EGG";
			case 120: return "VILLAGER_EGG";
			case 200: return "ENDER_CRYSTAL_EGG";
			case 14: return "PRIMED_TNT_EGG";
			case 66: return "WITCH_EGG";
			case 65: return "BAT_EGG";
			}
			return mat.toString();
		case 76:
			return "REDSTONE_TORCH";
		case 115:
			return "NETHER_WART";
		case 30:
			return "COBWEB";
		case 102:
			return "GLASS_PANE";
		case 101:
			return "IRON_BARS";
		case 58:
			return "CRAFTING_TABLE";
		case 123:
			return "REDSTONE_LAMP";
		}
		if(damage == 0 || isTool(mat)) return mat.toString();
		return mat.toString()+ ":" + damage;
	}
	/**
	 * @param mat The material to check
	 * @return Returns true if the item is a tool (Has durability) or false if it doesn't.
	 */
	public static boolean isTool(Material mat){
		return tools.contains(mat);
	}
}