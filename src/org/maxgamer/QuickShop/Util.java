package org.maxgamer.QuickShop;

import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.material.Sign;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;

public class Util{
	private static HashSet<Material> tools = new HashSet<Material>();
	private static HashSet<Material> blacklist = new HashSet<Material>();
	private static QuickShop plugin;
	
	private final static String charset = "ISO-8859-1";
	
	static{
		plugin = QuickShop.instance;
	
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
		
		List<String> configBlacklist = plugin.getConfig().getStringList("blacklist");
		
		for(String s : configBlacklist){
			Material mat = Material.getMaterial(s.toUpperCase());
			if(mat == null){
				mat = Material.getMaterial(Integer.parseInt(s));
				if(mat == null){
					plugin.getLogger().info(s + " is not a valid material.  Check your spelling or ID");
					continue;
				}
			}
			blacklist.add(mat);
		}
	}
	
	public static void parseColours(YamlConfiguration config){
		Set<String> keys = config.getKeys(true);
		
		for(String key : keys){
			String filtered = config.getString(key);
			if(filtered.startsWith("MemorySection")){
				continue;
			}
			filtered = ChatColor.translateAlternateColorCodes('&', filtered);
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
	 * Returns the chest attached to the given chest. The given block must be a chest.
	 * @param b The chest to check.
	 * @return the block which is also a chest and connected to b.
	 */
	public static Block getSecondHalf(Block b){
		Block[] blocks = new Block[4];
		blocks[0] = b.getRelative(1, 0, 0);
		blocks[1] = b.getRelative(-1, 0, 0);
		blocks[2] = b.getRelative(0, 0, 1);
		blocks[3] = b.getRelative(0, 0, -1);
		
		for(Block c : blocks){
			if(c.getType() == Material.CHEST){
				return c;
			}
		}

		return null;
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
	 * Converts the given ItemStack into a compound NBT tag, then the tag to an archived byte[], then the byte[] to a String using ISO-LATIN-1. Then returns the string.
	 * @param i The itemstack to serialize
	 * @return The String representing the item
	 * @throws UnsupportedEncodingException If the system does not have ISO-LATIN-1 installed as an encoding
	 * @throws ClassNotFoundException If the server is not v1_4_6.
	 */
	public static String getNBTString(ItemStack i) throws UnsupportedEncodingException, ClassNotFoundException{
		byte[] bytes = getNBTBytes(i);
		return new String(bytes, charset);
	}
	
	public static byte[] getNBTBytes(ItemStack i) throws ClassNotFoundException{
		Class.forName("net.minecraft.server.v1_4_6.ItemStack");
		net.minecraft.server.v1_4_6.ItemStack is = org.bukkit.craftbukkit.v1_4_6.inventory.CraftItemStack.asNMSCopy(i);
		//Save the NMS itemstack to a new NBT tag
		net.minecraft.server.v1_4_6.NBTTagCompound itemCompound = new net.minecraft.server.v1_4_6.NBTTagCompound();
		itemCompound = is.save(itemCompound);
		
		//Convert the NBT tag to a byte[]
		return net.minecraft.server.v1_4_6.NBTCompressedStreamTools.a(itemCompound);
	}
	
	/**
	 * Reverses the effects of Util.getNBTString(ItemStack).
	 * @param nbt The output of Util.getNBTString(ItemStack)
	 * @return The input of Util.getNBTString(ItemStack)
	 */
	public static ItemStack getItemStack(String nbt) throws UnsupportedEncodingException, ClassNotFoundException{
		return getItemStack((nbt.getBytes(charset)));
	}
	
	public static ItemStack getItemStack(byte[] bytes) throws UnsupportedEncodingException, ClassNotFoundException{
		net.minecraft.server.v1_4_6.NBTTagCompound c = net.minecraft.server.v1_4_6.NBTCompressedStreamTools.a(bytes);
		net.minecraft.server.v1_4_6.ItemStack is = net.minecraft.server.v1_4_6.ItemStack.a(c);
		return org.bukkit.craftbukkit.v1_4_6.inventory.CraftItemStack.asBukkitCopy(is);
	}
	
	public static String getName(ItemStack i){
		String vanillaName = getDataName(i.getType(), i.getDurability());
		if(plugin.useSpout){
			org.getspout.spoutapi.inventory.SpoutItemStack spoutItemStack = new org.getspout.spoutapi.inventory.SpoutItemStack(i);
			String spoutName = null;
			try{
				spoutName = spoutItemStack.getMaterial().getName();
				if(spoutName.length() > vanillaName.length() || vanillaName.contains(":")){
					return prettifyText(spoutName);
				}
			} catch(Exception e){ }
		}
		return prettifyText(vanillaName);
	}
	
	public static String prettifyText(String ugly){
		if(!ugly.contains("_") || (!ugly.equals(ugly.toUpperCase())))
			return ugly;
		String fin = "";
		ugly = ugly.toLowerCase();
		if(ugly.contains("_")){
			String[] splt = ugly.split("_");
			int i = 0;
			for(String s : splt){
				i += 1;
				fin += Character.toUpperCase(s.charAt(0)) + s.substring(1);
				if(i<splt.length)
					fin += " ";
			}
		} else {
			fin += Character.toUpperCase(ugly.charAt(0)) + ugly.substring(1);
		}
		return fin;
	}
	
	public static String toRomain(Integer value) {
		return toRoman(value.intValue());
	}

	private static final String[] ROMAN = {"X", "IX", "V", "IV", "I"};
	private static final int[] DECIMAL = {10, 9, 5, 4, 1};
	
	public static String toRoman(int n){
		if(n <= 0 || n >= 40) throw new NumberFormatException(n + " is out of range.  ");
		String roman = "";
		
		for(int i = 0; i < ROMAN.length; i++){
			while(n >= DECIMAL[i]){
				n -= DECIMAL[i];
				roman += ROMAN[i];
			}
		}
		
		return roman;
	}
	
	/**
	 * Converts a given material and data value into a format similar to Material.<?>.toString().
	 * Upper case, with underscores.  Includes material name in result.
	 * @param mat The base material.
	 * @param damage The durability/damage of the item.
	 * @return A string with the name of the item.
	 */
	private static String getDataName(Material mat, short damage){
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
			//Special case,.. Why?
			if(damage == 0) return "WATER_BOTTLE";
			Potion pot = Potion.fromDamage(damage);
			
			String prefix = "";
			String suffix = "";
			if(pot.getLevel() > 0) suffix += "_" + pot.getLevel();
			if(pot.hasExtendedDuration()) prefix += "EXTENDED_";
			if(pot.isSplash()) prefix += "SPLASH_";
			
			
			if(pot.getEffects().isEmpty()){
				switch((int) pot.getNameId()){
				case 0: return prefix + "MUNDANE_POTION" + suffix;
				case 7: return prefix + "CLEAR_POTION" + suffix;
				case 11: return prefix + "DIFFUSE_POTION" + suffix;
				case 13: return prefix + "ARTLESS_POTION" + suffix;
				case 15: return prefix + "THIN_POTION" + suffix;
				case 16: return prefix + "AWKWARD_POTION" + suffix;
				case 32: return prefix + "THICK_POTION" + suffix;
				}
			}
			else{
				String effects = "";
				for(PotionEffect effect : pot.getEffects()){
					effects += effect.toString().split(":")[0];
				}
				return prefix + effects + suffix;
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
	
	/**
	 * Compares two items to each other. Returns true if they match.
	 * @param stack1 The first item stack
	 * @param stack2 The second item stack
	 * @return true if the itemstacks match. (Material, durability, enchants)
	 */
	public static boolean matches(ItemStack stack1, ItemStack stack2){
		if(stack1 == stack2) return true; //Referring to the same thing, or both are null.
		if(stack1 == null || stack2 == null) return false; //One of them is null (Can't be both, see above)
		
		if(stack1.getType() != stack2.getType()) return false; //Not the same material
		if(stack1.getDurability() != stack2.getDurability()) return false; //Not the same durability
		if(!stack1.getEnchantments().equals(stack2.getEnchantments())) return false; //They have the same enchants
		
		try{
			Class.forName("org.bukkit.inventory.meta.EnchantmentStorageMeta");
			boolean book1 = stack1.getItemMeta() instanceof EnchantmentStorageMeta;
			boolean book2 = stack2.getItemMeta() instanceof EnchantmentStorageMeta;
			if(book1 != book2) return false;//One has enchantment meta, the other does not.
			if(book1 == true){ //They are the same here (both true or both false).  So if one is true, the other is true.
				Map<Enchantment, Integer> ench1 = ((EnchantmentStorageMeta) stack1.getItemMeta()).getStoredEnchants();
				Map<Enchantment, Integer> ench2 = ((EnchantmentStorageMeta) stack2.getItemMeta()).getStoredEnchants();
				if(!ench1.equals(ench2)) return false; //Enchants aren't the same.
			}
		}
		catch(ClassNotFoundException e){
			//Nothing. They dont have a build high enough to support this.
		}
		
		return true;
	}
	
	/**
	 * Formats the given number according to how vault would like it.
	 * E.g. $50 or 5 dollars.
	 * @return The formatted string.
	*/
	public static String format(double n){
		try{
			return plugin.getEcon().format(n);
		}
		catch(NumberFormatException e){
			return "$"+n;
		}
	}
	
	/**
	 * @param m The material to check if it is blacklisted
	 * @return true if the material is black listed. False if not.
	 */
	public static boolean isBlacklisted(Material m){
		return blacklist.contains(m);
	}
	
	/**
	 * Fetches the block which the given sign is attached to
	 * @param sign The sign which is attached
	 * @return The block the sign is attached to
	 */
	public static Block getAttached(Block b){
		Sign sign = (Sign) b.getState().getData();
		BlockFace attached = sign.getAttachedFace();
		
		if(attached == null) return null;
		return b.getRelative(attached);
	}
	
	/**
	 * Counts the number of items in the given inventory where Util.matches(inventory item, item) is true.
	 * @param inv The inventory to search
	 * @param item The ItemStack to search for
	 * @return The number of items that match in this inventory.
	 */
	public static int countItems(Inventory inv, ItemStack item){
		int items = 0;
		for(ItemStack iStack : inv.getContents()){
			if(iStack == null) continue;
			if(Util.matches(item, iStack)){
				items += iStack.getAmount();
			}
		}
		return items;
	}
	
	/**
	 * Returns the number of items that can be given to the inventory safely.
	 * @param inv The inventory to count
	 * @param item The item prototype.  Material, durabiltiy and enchants must match for 'stackability' to occur.
	 * @return The number of items that can be given to the inventory safely.
	 */
	public static int countSpace(Inventory inv, ItemStack item){
		int space = 0;
		for(ItemStack iStack : inv.getContents()){
			if(iStack == null || iStack.getType() == Material.AIR){
				space += item.getMaxStackSize();
			}
			else if(matches(item, iStack)){
				space += item.getMaxStackSize() - iStack.getAmount();
			}
		}
		return space;
	}
}