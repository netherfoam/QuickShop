package org.maxgamer.QuickShop.Shop;

import org.bukkit.Chunk;
import org.bukkit.World;

public class ShopChunk {
	private World world;
	private int x;
	private int z;
	public ShopChunk(World world, int x, int z){
		this.world = world;
		this.x = x;
		this.z = z;
	}
	
	public int getX(){
		return this.x;
	}
	public int getZ(){
		return this.z;
	}
	public World getWorld(){
		return this.world;
	}
	public Chunk getChunk(){
		return this.world.getChunkAt(this.x, this.z);
	}
	
	@Override
	public boolean equals(Object obj){
		if(obj.getClass() != this.getClass()){
			return false;
		}
		else{
			ShopChunk shopChunk = (ShopChunk) obj;
			return (this.getWorld() == shopChunk.getWorld() && this.getX() == shopChunk.getX() && this.getZ() == shopChunk.getZ());
		}
	}
	
	@Override
	public int hashCode(){
		return this.x * this.z + world.hashCode();
	}

}
