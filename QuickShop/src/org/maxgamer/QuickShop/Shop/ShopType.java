package org.maxgamer.QuickShop.Shop;
public enum ShopType{
	SELLING(),
	BUYING();
	public static ShopType fromID(int id){
		if(id == 0){
			return ShopType.SELLING;
		}
		if(id == 1){
			return ShopType.BUYING;
		}
		return null;
	}
	public static int toID(ShopType shopType){
		if(shopType == ShopType.SELLING){
			return 0;
		}
		if(shopType == ShopType.BUYING){
			return 1;
		}
		else{
			return -1;
		}
	}
	public int toID(){
		return ShopType.toID(this);
	}
}