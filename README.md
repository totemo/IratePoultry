Irate Poultry
=============

*IratePoultry* is a Thanksgiving-themed Bukkit plugin for Survival-mode
Minecraft servers.  It disguises naturally-spawned, overworld creepers,
skeletons, zombies and spiders into hostile chickens.  A configurable
percentage of those mobs are turned into blazes, also disguised as chickens.
With the right texture pack, those mobs become angry turkeys, hell bent on
exacting revenge for the slaughter of their avian comrades.

Disguised skeletons shoot eggs rather than arrows, and a configurable percentage
of those eggs hatch into spiders disguised as baby chickens.  Disguised creepers
do a configurable fraction of their normal damage and spawn a configurable
number of spiders disguised as baby chickens that attack the player.

Various custom drops are given when a turkey (disguised mob) is killed. These
are somewhat configurable, in terms of their drop chance, name and lore.  Certain
custom drops are considered "high value" and can only be dropped if the mob was
recently damaged by a player.  The Looting enchant adds 1% to the drop chance
of all custom drops per enchantment level.

IratePoultry alters player death messages so that disguised mobs are referred
to as turkeys (with configurable names) in a manner that is compatible with
custom death messages supplied by buzzie71's
[MaskOfFutures](http://github.com/buzzie71/MaskOfFutures).

Dungeon spawner mobs and those mobs not in the overworld are *not* affected by the
above mechanics.


Dependencies
------------

You will need to install compatible versions of
[Lib's Disguises](https://www.spigotmc.org/resources/libs-disguises.81/) and
[ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/)
to use IratePoultry.


Building
--------

A Maven pom.xml file is provided.  To build, simply change into the top-level
project directory and run Maven:
```
cd IratePoultry
mvn
```

The plugin JAR file will be named `target/IratePoultry-<version>.jar`.


Issues
------
Currently, disguised mobs are not being correctly removed from the world on
server restarts.  Bukkit doesn't generate chunk unload events for restarts,
so the plugin hooks the `PluginDisableEvent` to kill these mobs.  This works
in testing but not so well in production.  The reason is currently unknown.

IratePoultry also inherits any currently open issues from Lib's Disguises.  In
practice, we have observed that some
[disguised mobs are invisible](https://github.com/libraryaddict/LibsDisguises/issues/59).
Lib's Disguises reputedly re-sends disguise entities to clients periodically to
fix this.  However, we found that blazes had a tendency to stay invisble
indefinitely.  Disguised spiders spawned from creeper explosions were also
frequently observed to be invisible when not expected to be so.  The damage 
sound of disguised mobs is sometimes not properly disguised.

Reinforcement mobs (such as the disguised spiders spawned in creeper explosions)
were also sometimes visible *undisguised* (as spiders) for a fraction of a
second when first spawned.

