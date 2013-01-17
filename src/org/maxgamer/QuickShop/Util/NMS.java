package org.maxgamer.QuickShop.Util;

import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class NMS{
	private static HashMap<String, NMSDependent> dependents = new HashMap<String, NMSDependent>();
	
	static{
		NMSDependent dep;
		
		/* ***********************
		 * **       1.4       ** *
		 * ***********************/
		dep = new NMSDependent(){
			@Override
			public void safeGuard(Item item) {
				ItemStack iStack = item.getItemStack();
				
				//Fetch the NMS item
				net.minecraft.server.ItemStack nmsI = org.bukkit.craftbukkit.inventory.CraftItemStack.createNMSItemStack(iStack);
				//Force the count to 0, don't notify anything though.
				nmsI.count = 0;
				//Get the itemstack back as a bukkit stack
				iStack = org.bukkit.craftbukkit.inventory.CraftItemStack.asBukkitStack(nmsI);
				
				//Set the display item to the stack.
				item.setItemStack(iStack);
			}
			
			@Override
			public byte[] getNBTBytes(ItemStack iStack) {
				net.minecraft.server.ItemStack is = org.bukkit.craftbukkit.inventory.CraftItemStack.createNMSItemStack(iStack);
				//Save the NMS itemstack to a new NBT tag
				net.minecraft.server.NBTTagCompound itemCompound = new net.minecraft.server.NBTTagCompound();
				itemCompound = is.save(itemCompound);
				
				//Convert the NBT tag to a byte[]
				return net.minecraft.server.NBTCompressedStreamTools.a(itemCompound);
			}
			
			@Override
			public ItemStack getItemStack(byte[] bytes) {
				net.minecraft.server.NBTTagCompound c = net.minecraft.server.NBTCompressedStreamTools.a(bytes);
				net.minecraft.server.ItemStack is = net.minecraft.server.ItemStack.a(c);
				return org.bukkit.craftbukkit.inventory.CraftItemStack.asBukkitStack(is);
			}
		};
		dependents.put("", dep);
		
		/* ***********************
		 * **      1.4.6      ** *
		 * ***********************/
		dep = new NMSDependent(){
			@Override
			public void safeGuard(Item item) {
				ItemStack iStack = item.getItemStack();
				
				//Fetch the NMS item
				net.minecraft.server.v1_4_6.ItemStack nmsI = org.bukkit.craftbukkit.v1_4_6.inventory.CraftItemStack.asNMSCopy(iStack);
				//Force the count to 0, don't notify anything though.
				nmsI.count = 0;
				//Get the itemstack back as a bukkit stack
				iStack = org.bukkit.craftbukkit.v1_4_6.inventory.CraftItemStack.asBukkitCopy(nmsI);
				
				//Set the display item to the stack.
				item.setItemStack(iStack);
			}
			
			@Override
			public byte[] getNBTBytes(ItemStack iStack) {
				net.minecraft.server.v1_4_6.ItemStack is = org.bukkit.craftbukkit.v1_4_6.inventory.CraftItemStack.asNMSCopy(iStack);
				//Save the NMS itemstack to a new NBT tag
				net.minecraft.server.v1_4_6.NBTTagCompound itemCompound = new net.minecraft.server.v1_4_6.NBTTagCompound();
				itemCompound = is.save(itemCompound);
				
				//Convert the NBT tag to a byte[]
				return net.minecraft.server.v1_4_6.NBTCompressedStreamTools.a(itemCompound);
			}
			
			@Override
			public ItemStack getItemStack(byte[] bytes) {
				net.minecraft.server.v1_4_6.NBTTagCompound c = net.minecraft.server.v1_4_6.NBTCompressedStreamTools.a(bytes);
				net.minecraft.server.v1_4_6.ItemStack is = net.minecraft.server.v1_4_6.ItemStack.a(c);
				return org.bukkit.craftbukkit.v1_4_6.inventory.CraftItemStack.asBukkitCopy(is);
			}
		};
		dependents.put("v1_4_6", dep);
		
		/* ***********************
		 * **      1.4.7      ** *
		 * ***********************/
		dep = new NMSDependent(){
			@Override
			public void safeGuard(Item item) {
				ItemStack iStack = item.getItemStack();
				
				//Fetch the NMS item
				net.minecraft.server.v1_4_R1.ItemStack nmsI = org.bukkit.craftbukkit.v1_4_R1.inventory.CraftItemStack.asNMSCopy(iStack);
				//Force the count to 0, don't notify anything though.
				nmsI.count = 0;
				//Get the itemstack back as a bukkit stack
				iStack = org.bukkit.craftbukkit.v1_4_R1.inventory.CraftItemStack.asBukkitCopy(nmsI);
				
				//Set the display item to the stack.
				item.setItemStack(iStack);
			}
			
			@Override
			public byte[] getNBTBytes(ItemStack iStack) {
				net.minecraft.server.v1_4_R1.ItemStack is = org.bukkit.craftbukkit.v1_4_R1.inventory.CraftItemStack.asNMSCopy(iStack);
				//Save the NMS itemstack to a new NBT tag
				net.minecraft.server.v1_4_R1.NBTTagCompound itemCompound = new net.minecraft.server.v1_4_R1.NBTTagCompound();
				itemCompound = is.save(itemCompound);
				
				//Convert the NBT tag to a byte[]
				return net.minecraft.server.v1_4_R1.NBTCompressedStreamTools.a(itemCompound);
			}
			
			@Override
			public ItemStack getItemStack(byte[] bytes) {
				net.minecraft.server.v1_4_R1.NBTTagCompound c = net.minecraft.server.v1_4_R1.NBTCompressedStreamTools.a(bytes);
				net.minecraft.server.v1_4_R1.ItemStack is = net.minecraft.server.v1_4_R1.ItemStack.createStack(c);
				return org.bukkit.craftbukkit.v1_4_R1.inventory.CraftItemStack.asBukkitCopy(is);
			}
		};
		dependents.put("v1_4_R1", dep);
	}
	
	/** The known working NMSDependent. This will be null if we haven't found one yet. */
	private static NMSDependent nms;
	
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
		if(nms == null){ //We haven't found it yet.
			for(NMSDependent dep : dependents.values()){
				try{
					dep.safeGuard(item); //Try apply it.
					nms = dep; //If we made it this far, we've found a working version.
					rename(item.getItemStack());
					return; //End of loop.
				}
				catch(Exception e){}
				catch(Error e){}
			}
			throw new ClassNotFoundException("This version of QuickShop is incompatible."); //We haven't got code to support your version!
		}
		else{ //We have a known safeguarder.
			nms.safeGuard(item);
			rename(item.getItemStack());
		}
	}
	
	/** 
	 * Renames the given itemstack to ChatColor.RED + "QuickShop " + Util.getName(iStack).
	 * This prevents items stacking (Unless, ofcourse, the other item has a jerky name too - Rare)
	 * @param iStack the itemstack to rename.
	 */
	private static void rename(ItemStack iStack){
		//This might stop it merging with other items. * Unless they're named funnily... In which case, shit.
		ItemMeta meta = iStack.getItemMeta();
		meta.setDisplayName(ChatColor.RED + "QuickShop " + Util.getName(iStack));
		iStack.setItemMeta(meta);
	}
	
	/**
	 * Returns a byte array of the given ItemStack.
	 * @param iStack The given itemstack
	 * @return The compressed NBT tag version of the given stack.
	 * Will return null if used with an invalid Bukkit version.
	 * @throws ClassNotFoundException if QS is not updated to this build of bukkit.
	 */
	public static byte[] getNBTBytes(ItemStack iStack) throws ClassNotFoundException{
		if(nms == null){ //We haven't found it yet.
			for(NMSDependent dep : dependents.values()){
				try{
					byte[] bytes = dep.getNBTBytes(iStack); //Try apply it.
					nms = dep; //If we made it this far, we've found a working version.
					return bytes; //End of loop.
				}
				catch(Exception e){}
				catch(Error e){}
			}
			throw new ClassNotFoundException("This version of QuickShop is incompatible."); //We haven't got code to support your version!
		}
		else{ //We have a known safeguarder.
			return nms.getNBTBytes(iStack);
		}
	}
	
	/**
	 * Converts the given bytes into an itemstack
	 * @param bytes The bytes to convert to an itemstack
	 * @return The itemstack
	 * @throws ClassNotFoundException if QS is not updated to this build of bukkit.
	 */
	public static ItemStack getItemStack(byte[] bytes) throws ClassNotFoundException{
		if(nms == null){ //We haven't found it yet.
			for(NMSDependent dep : dependents.values()){
				try{
					ItemStack iStack = dep.getItemStack(bytes); //Try apply it.
					nms = dep; //If we made it this far, we've found a working version.
					return iStack; //End of loop.
				}
				catch(Exception e){}
				catch(Error e){}
			}
			throw new ClassNotFoundException("This version of QuickShop is incompatible."); //We haven't got code to support your version!
		}
		else{ //We have a known safeguarder.
			return nms.getItemStack(bytes);
		}
	}
	
	private interface NMSDependent{
		public void safeGuard(Item item);
		public byte[] getNBTBytes(ItemStack iStack);
		public ItemStack getItemStack(byte[] bytes);
	}
}