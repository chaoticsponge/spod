# spod

Client-side quality-of-life mod for Minecraft 1.21.1 with NeoForge 21.1.172 or newer 21.1.x.

- **Directional placement:** Ctrl+Scroll or R selects dispenser/observer facing with a placement preview; Shift+R restores vanilla placement.
- **Hive counters:** Pixel counters on hive tops and front upper-left corners show bee counts; multiplayer non-operator counts use existing Jade server support.
- **Mega Showdown markers:** Names Mega Sites, Keystone Megaroids, Observatories, Archaeological Sites and Wishing Wealds on Xaero's world map by recognizing loaded block patterns; requires Xaero's Minimap.
- **Spawn overlay:** Red outlines and labels show ground surfaces eligible under current hostile-spawn light/block rules in a 30×30 area, ignoring player distance.
- **Overlay toggle:** O hides or shows all overlays; keybinds can be changed in Minecraft's Controls settings.
- **Clicker:** `/click left` or `/click right` holds the action; `/click left 1 1` repeats one second held and one second released; `/click stop` stops it.
- **Tool replacement:** At 10 durability or less, the clicker swaps to a hotbar tool of the same type in any material or stops; `/click left minecraft:diamond_pickaxe` selects the pickaxe type.
- **Mending:** `/click mending` swings a sword for 0.1s with 1s intervals while XP repairs damaged Mending hotbar items in the offhand, returning each when full; `/click stop` cancels.
- **Mining tools:** `/mining` toggles manual-mining tool selection: obsidian hammer by default, excavator for shovel blocks, pickaxe for ores; `/mining stop` disables it, and tools at 10 durability or less are skipped.
- **Mining floor:** While `/mining` is active, fills reachable holes in the 3×3 floor ahead using common stone blocks from your hotbar, prioritizing your walking path and restoring the mining tool after placement.
- **Mining torches:** `/mining` places offhand torches on reachable floor when current light permits hostile spawning ahead, spacing placements using actual light levels; works with the overlay hidden.
- **Mining defense:** `/mining` switches to a hotbar sword to attack visible hostile mobs within melee reach at full cooldown, then restores your mining tool; swords at 10 durability or less are skipped.
- **Auto reconnect:** After a multiplayer disconnect, retries after 1, 2, 3, 4, then 5 minutes and every 5 minutes until connected; leaving the screen or pressing Stop cancels retries.
- **Word scramble:** `/scramble` sends a shuffled English word as a normal chat message from you for everyone to solve.

**Install:** [Download spod.jar](dist/spod.jar?raw=true), place it in your client's `mods` folder, replace any older spod/Hive Count JAR, and restart Minecraft.
