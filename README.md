A plug-in for the open-source Minecraft server framework, Bukkit. QuickShop was designed to combine concepts from a number of expired "shop" plugins which had become inactive. The goal of the project was to provide something user friendly to allow players to trade items through the Minecraft server via an economy. It had to be simple, given the target audience and quick to use. 

The plugin was conceptually done through inspiration from other plugins which had unique ways of displaying sales, interfacing with shops and monitoring stock. The implementation of this plugin managed to successfully combine these ideals and became a hit with 117,000 downloads at time of writing. The project has now been discontinued, and only works on older versions of Bukkit due to changes in the framework.

Download available at http://dev.bukkit.org/bukkit-plugins/quickshop/

The Predecessors
========
Showcase was abandoned. Chestshop needed a display, with signs whose order you could never remember. SCS has too many commands. I'm yet to find someone who uses essentials shops.

Don't get me wrong though, they're all awesome plugins each in a few ways. 


QuickShop
========

So, I set out to write this. QuickShop. QuickShop is a shop plugin, that allows players to sell items from a chest with no commands. It allows players to purchase any number of items easily. In fact, this plugin doesn't even have any commands that a player would ever need! 


Features
========

Easy to use
Togglable Display Item on top of chest
NBT Data, Enchants, Tool Damage, Potion and Mob Egg support
Unlimited chest support
Blacklist support & bypass permissions
Shops that buy items and sell items at the same time are possible (Using double chests)
Herochat support
Checks a player can open a chest before letting them create a shop! 

No longer accepting feature requests
For those people that hate reading, here's an awesome video from UltiByte:

https://www.youtube.com/watch?feature=player_embedded&v=6NpkVd2mA7Y 

Installation Guide:
========

http://www.youtube.com/watch?v=eJsv7fqaXNk 

How to Create a Shop
========

Place a chest on the ground
Hit the chest with the item you want to trade
Type in price for the item (As prompted) in chat
Fill the chest with the items you wish to sell 

Advanced
========

Face the chest
Type either /qs sell or /qs buy to make the shop buy/sell instead (Optional: use /shop instead of /qs)
Stock the shop accordingly, if necessary. 

How to Buy/Sell to a Shop
========
Find a shop
Hit / Left click the shop
Enter the amount you wish to trade in chat
Menu Example: Menu

See it in action: http://maxgamer.org or play.maxgamer.org:25571 and /warp market! 


Commands
========
<pre>
/qs unlimited - Makes the shop you're looking at become unlimited.
/qs setowner <player> - Changes shop owner to <player>.
/qs buy - Changes your shop you're looking at to one that buys items
/qs sell - Changes your shop you're looking at to one that sells items
/qs price <price> - Change the price of your shop that you're looking at
/qs clean - Removes any existing shop that has 0 stock.
/qs find <item> - Use to find the nearest shop that begins with <item> - E.g. '/qs find dia' will find the nearest diamond shop.
/shop - Alias (Optional) of /qs
</pre>

Permissions
========
<pre>
Player Nodes
quickshop.use - Required to use ANY quickshop
quickshop.create.sell - Required to make a QuickShop (At all)
quickshop.create.buy - Required to use /qs buy (Change the shop type from Sell -> Buy)
quickshop.create.double - Required to make doublechest shops.
quickshop.create.changeprice - Required to use /qs price (Ability to change the price of a shop without destroying it. This may allow for some scammers to quickly change the price while someone is buying.
quickshop.bypass.ItemID - Required to sell blacklisted items (E.g. bedrock)
quickshop.find - Required to use /qs find <item>
Admin Nodes
quickshop.unlimited - Required to use /quickshop unlimited
quickshop.setowner - Allows use of /qs setowner
quickshop.other.destroy - Allow breaking other peoples QuickShops if they're locked by this plugin
quickshop.other.open - Allow opening (And stealing/Stocking) other players QuickShops
quickshop.other.price - Allow changing price of other people's shops
quickshop.refill - Ability to refill chests using a command (=Unlimited items)
quickshop.empty - Ability to empty chests of all items
quickshop.clean - Permission to purge any shops that have 0 stock.
</pre>  
