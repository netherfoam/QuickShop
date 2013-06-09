package org.maxgamer.QuickShop.Util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class NMS{
	/** The ID for shop item names */
	private static int nextId = 0;
	
	/**
	 * Sets the given items stack size to 0,
	 * as well as gives it a custom NBT tag
	 * called "quickshop" in the root, to
	 * prevent it from merging with other
	 * items.  This is all through NMS code.
	 * @param item The given item
	 * @throws ClassNotFoundException 
	 */
	public static void safeGuard(Item item) throws ClassNotFoundException{
		rename(item.getItemStack());
		protect(item);
		item.setPickupDelay(Integer.MAX_VALUE);
	}
	
	/** 
	 * Renames the given itemstack to ChatColor.RED + "QuickShop " + Util.getName(iStack).
	 * This prevents items stacking (Unless, ofcourse, the other item has a jerky name too - Rare)
	 * @param iStack the itemstack to rename.
	 */
	private static void rename(ItemStack iStack){
		//This stops it merging with other items. * Unless they're named funnily... In which case, shit.
		ItemMeta meta = iStack.getItemMeta();
		meta.setDisplayName(ChatColor.RED + "QuickShop " + Util.getName(iStack) + " " + nextId++);
		iStack.setItemMeta(meta);
	}
	
	private static void protect(Item item){
		try{
			Field itemField = item.getClass().getDeclaredField("item");
			itemField.setAccessible(true);
			
			Object nmsEntityItem = itemField.get(item);
			
			Method getItemStack;
			try{
				getItemStack = nmsEntityItem.getClass().getMethod("getItemStack");
			}
			catch(NoSuchMethodException e){
				getItemStack = nmsEntityItem.getClass().getMethod("d"); //Obfuscated for 'getItemStack'
			}
			
			Object itemStack = getItemStack.invoke(nmsEntityItem);
			
			Field countField;
			
			try{
				countField = itemStack.getClass().getDeclaredField("count");
			}
			catch(NoSuchFieldException e){
				countField = itemStack.getClass().getDeclaredField("a"); // 'count' is called 'a' in obfuscated code (...ForgeModLoader)
			}
			countField.setAccessible(true);
			countField.set(itemStack, 0);
		}
		catch(NoSuchFieldException e){
			e.printStackTrace();
			System.out.println("[QuickShop] Could not protect item from pickup properly! Dupes are now possible.");
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}