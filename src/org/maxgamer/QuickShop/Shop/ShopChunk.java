package org.maxgamer.QuickShop.Shop;

public class ShopChunk {
	private String world;
	private int x;
	private int z;
	public ShopChunk(String world, int x, int z){
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
	public String getWorld(){
		return this.world;
	}
	
	@Override
	public boolean equals(Object obj){
		if(obj.getClass() != this.getClass()){
			return false;
		}
		else{
			ShopChunk shopChunk = (ShopChunk) obj;
			return (this.getWorld().equals(shopChunk.getWorld()) && this.getX() == shopChunk.getX() && this.getZ() == shopChunk.getZ());
		}
	}
	
	@Override
	public int hashCode(){
		return this.x * this.z + world.hashCode();
	}

}
