package io.totemo.iratepoultry;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Spider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;

// ----------------------------------------------------------------------------
/**
 * Main plugin class.
 *
 * IratePoultry is a plugin that turns hostile mobs into very angry birds.
 *
 * The main features are:
 * <ul>
 * <li>The plugin affects mobs in the overworld only, and spawner mobs are not
 * modified.</li>
 * <li>Naturally spawned creepers, skeletons, large spiders and zombies are
 * disguised as chickens.</li>
 * <li>A configurable percentage of hostile mobs are transformed into blazes,
 * disguised as chickens.</li>
 * <li>When a disguised creeper explodes, a configurable number of hostile mobs
 * are spawned and launched away from the explosion. These hostile mobs are
 * disguised as baby chickens.</li>
 * <li>Disguised skeletons shoot eggs that hatch more hostile mobs, which are
 * also disguised as baby chickens.</li>
 * <li>For disguised mobs to drop special drops when they die, they must have
 * been recently hurt by a player.</li>
 * </ul>
 */
public class IratePoultry extends JavaPlugin implements Listener {
    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {
        if (DISGUISED_META == null) {
            DISGUISED_META = new FixedMetadataValue(this, null);
        }

        _overworld = Bukkit.getWorld("world");

        saveDefaultConfig();
        _config.reload();
        getServer().getPluginManager().registerEvents(this, this);
    }

    // ------------------------------------------------------------------------
    /**
     * Handle commands.
     *
     * <ul>
     * <li>/iratepoultry reload</li>
     * </ul>
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String name, String[] args) {
        if (command.getName().equalsIgnoreCase("iratepoultry")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                _config.reload();
                sender.sendMessage(ChatColor.GOLD + "IratePoultry configuration reloaded.");
                return true;
            }
        }

        sender.sendMessage(ChatColor.RED + "Invalid command syntax.");
        return true;
    }

    // ------------------------------------------------------------------------
    /**
     * In the overworld, disguise naturally spawned creepers, regular spiders,
     * skeletons and zombies as chickens.
     *
     * Also, replace a percentage of these mobs with blazes. Disguised mobs are
     * tagged with the DISGUISED metadata.
     *
     * Spawner-spawned mobs are not affected in any way.
     */
    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!isInOverworld(event)) {
            return;
        }

        if (event.getSpawnReason() == SpawnReason.NATURAL && isDisguiseableHostileMob(event.getEntityType())) {
            Entity entity = event.getEntity();
            Location loc = entity.getLocation();

            // Replace some hostiles with blazes.
            if (Math.random() < _config.BLAZE_CHANCE) {
                entity.remove();

                // This spawn can probably cause re-entry of this event handler.
                entity = loc.getWorld().spawnEntity(loc, EntityType.BLAZE);
            }

            addDisguise(entity, true);
            if (_config.DEBUG_SPAWN_NATURAL) {
                getLogger().info("Disguised " + entity.getType() + " spawned at " + formatLocation(loc));
            }
        }
    } // onCreatureSpawn

    // ------------------------------------------------------------------------
    /**
     * Disguised mobs can't go through portals.
     *
     * They die.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityPortalEnter(EntityPortalEnterEvent event) {
        if (!isInOverworld(event)) {
            return;
        }

        Entity entity = event.getEntity();
        if (isDisguised(entity)) {
            handleDeath(entity);
            entity.remove();
        }
    }

    // ------------------------------------------------------------------------
    /**
     * When a chunk is unloaded, we remove all the disguised mobs, since neither
     * metadata nor disguises persist.
     *
     * Server restarts do not trigger chunk unload events, however.
     */
    @EventHandler(ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        if (event.getWorld() != _overworld) {
            return;
        }
        removeDisguisedMobs(event.getChunk());
    }

    // ------------------------------------------------------------------------
    /**
     * When the plugin is disabled, remove all disguised mobs in all loaded
     * chunks.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPluginDisable(@SuppressWarnings("unused") PluginDisableEvent event) {
        for (Chunk chunk : _overworld.getLoadedChunks()) {
            removeDisguisedMobs(chunk);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Post-process death messages to replace hostile mob type names with a
     * coonfigurable mob name.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!isInOverworld(event)) {
            return;
        }

        Player player = event.getEntity();
        String randomAttacker = getRandomMobName();
        if (player.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent lastDamageEvent = (EntityDamageByEntityEvent) player.getLastDamageCause();
            Entity damager = lastDamageEvent.getDamager();
            if (isDisguised(damager)) {
                Pattern pattern = Pattern.compile(damager.getType().name(), Pattern.CASE_INSENSITIVE);
                Matcher m = pattern.matcher(event.getDeathMessage());
                event.setDeathMessage(m.replaceAll(randomAttacker));
            } else if (damager instanceof SmallFireball) {
                SmallFireball projectile = (SmallFireball) damager;
                ProjectileSource shooter = projectile.getShooter();
                if (shooter instanceof Blaze && isDisguised((Blaze) shooter)) {
                    event.setDeathMessage(event.getDeathMessage().replaceAll("Blaze", randomAttacker));
                }
            }
        }
    } // onPlayerDeath

    // ------------------------------------------------------------------------
    /**
     * Tag disguised mobs hurt by players.
     *
     * Only those disguised mobs hurt recently by players will have special
     * drops.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!isInOverworld(event)) {
            return;
        }

        Entity entity = event.getEntity();
        Location loc = entity.getLocation();
        loc.getWorld().spigot().playEffect(loc, Effect.COLOURED_DUST, 0, 0, 0.5f, 0.5f, 0.5f, 0, 10, 32);

        if (entity.hasMetadata(DISGUISED)) {
            int lootingLevel = 0;
            boolean isPlayerAttack = false;
            if (event.getDamager() instanceof Player) {
                isPlayerAttack = true;
                Player player = (Player) event.getDamager();
                lootingLevel = player.getEquipment().getItemInMainHand().getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS);
            } else if (event.getDamager() instanceof Projectile) {
                Projectile projectile = (Projectile) event.getDamager();
                if (projectile.getShooter() instanceof Player) {
                    isPlayerAttack = true;
                }
            }

            // Tag mobs hurt by players with the damage time stamp.
            if (isPlayerAttack) {
                entity.setMetadata(PLAYER_DAMAGE_TIME, new FixedMetadataValue(this, new Long(entity.getWorld().getFullTime())));
                entity.setMetadata(PLAYER_LOOTING_LEVEL, new FixedMetadataValue(this, lootingLevel));
            }
        }
    } // onEntityDamageByEntity

    // ------------------------------------------------------------------------
    /**
     * Event handler called when an explosive is primed.
     *
     * We use it to detect impending creeper explosions. The event is fired
     * immediately before the explosion.
     */
    @EventHandler(ignoreCancelled = true)
    public void onCreeperDetonate(ExplosionPrimeEvent event) {
        if (!isInOverworld(event)) {
            return;
        }

        Entity entity = event.getEntity();
        if (entity.getType() == EntityType.CREEPER && isDisguised(entity)) {
            event.setRadius((float) _config.BLAST_RADIUS_SCALE * event.getRadius());

            launchReinforcements(entity);

            Location loc = entity.getLocation();
            loc.getWorld().playSound(loc, Sound.ENTITY_CHICKEN_HURT, 1, 1);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * On disguised mob death, de-register the disguise and do special drops if
     * a player hurt the mob recently.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!isInOverworld(event)) {
            return;
        }

        Entity entity = event.getEntity();
        if (isDisguised(entity)) {
            handleDeath(entity);

            int lootingLevel = getLootingLevelMeta(entity);
            boolean specialDrops = false;
            Long damageTime = getPlayerDamageTime(entity);
            if (damageTime != null) {
                Location loc = entity.getLocation();
                World world = loc.getWorld();
                if (world.getFullTime() - damageTime < PLAYER_DAMAGE_TICKS) {
                    specialDrops = true;
                }
            }

            doCustomDrops(event.getDrops(), entity.getFireTicks() > 0, specialDrops, lootingLevel);
        }
    } // onEntityDeath

    // ------------------------------------------------------------------------
    /**
     * When a disguised skeleton in the overworld shoots a bow, replace the
     * arrow with an egg.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!isInOverworld(event)) {
            return;
        }

        LivingEntity shooter = event.getEntity();
        if (isDisguised(shooter)) {
            Projectile arrow = (Projectile) event.getProjectile();
            Vector velocity = arrow.getVelocity();
            shooter.launchProjectile(Egg.class, velocity);
            arrow.remove();
        }
    }

    // ------------------------------------------------------------------------
    /**
     * When a projectile hits, if the shooter was a disguised skeleton, then
     * randomly spawn reinforcement mobs at the impact point.
     */
    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!isInOverworld(event)) {
            return;
        }

        Projectile projectile = event.getEntity();
        ProjectileSource shooter = projectile.getShooter();
        if (shooter instanceof Skeleton) {
            Skeleton skeleton = (Skeleton) shooter;
            if (isDisguised(skeleton)) {
                if (Math.random() < _config.REINFORCEMENT_EGG_CHANCE) {
                    spawnReinforcement(projectile.getLocation());
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return the world time when a player damaged the specified entity, if
     * stored as a PLAYER_DAMAGE_TIME metadata value, or null if that didn't
     * happen.
     *
     * @param entity the entity (mob).
     * @return the damage time stamp as Long, or null.
     */
    protected Long getPlayerDamageTime(Entity entity) {
        List<MetadataValue> playerDamageTime = entity.getMetadata(PLAYER_DAMAGE_TIME);
        if (playerDamageTime.size() > 0) {
            MetadataValue value = playerDamageTime.get(0);
            if (value.value() instanceof Long) {
                return (Long) value.value();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the looting level metadata value from an entity.
     *
     * This metadata is added when a player damages a disguised mob. It is the
     * level of the Looting enchant on the weapon that did the damage, or 0 if
     * there was no such enchant.
     *
     * @param entity the damaged entity.
     * @return the level of the Looting enchant, or 0 if not so enchanted.
     */
    protected int getLootingLevelMeta(Entity entity) {
        List<MetadataValue> lootingLevel = entity.getMetadata(PLAYER_LOOTING_LEVEL);
        if (lootingLevel.size() > 0) {
            return lootingLevel.get(0).asInt();
        }
        return 0;
    }

    // ------------------------------------------------------------------------
    /**
     * Handle the death of a disguised mob by showing death particle effects and
     * removing the disguise.
     *
     * @param mob the mob.
     */
    protected void handleDeath(Entity mob) {
        Location loc = mob.getLocation();
        loc.getWorld().spigot().playEffect(loc, Effect.EXPLOSION, 0, 0, 0.5f, 0.5f, 0.5f, 0, 10, 32);
        removeDisguise(mob);

        if (_config.DEBUG_DEATH) {
            getLogger().info("Disguised " + mob.getType() + " died at " + formatLocation(mob.getLocation()));
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Clear the drops from an entity death event and add in custom drops.
     *
     * @param drops the list of drops for the EntityDeathEvent.
     * @param cooked true if the entity died while on fire.
     * @param special if true, low-probability, special drops are possible;
     *        otherwise, the drops are custom but mundane.
     * @param lootingLevel the level of looting on the weapon ([0,3]).
     */
    protected void doCustomDrops(List<ItemStack> drops, boolean cooked, boolean special, int lootingLevel) {
        drops.clear();

        double lootingBoost = 0.01 * lootingLevel;
        if (Math.random() < _config.DROPS_FEATHER_CHANCE + lootingBoost) {
            ItemStack feather = new ItemStack(Material.FEATHER);
            setItemNameAndLore(feather, _config.DROPS_FEATHER_NAME, _config.DROPS_FEATHER_LORE);
            drops.add(feather);
        }

        if (Math.random() < _config.DROPS_EGG_CHANCE + lootingBoost) {
            ItemStack egg = new ItemStack(Material.EGG);
            setItemNameAndLore(egg, _config.DROPS_EGG_NAME, _config.DROPS_EGG_LORE);
            drops.add(egg);
        }

        if (Math.random() < _config.DROPS_CORPSE_CHANCE + lootingBoost) {
            ItemStack corpse = new ItemStack(cooked ? Material.COOKED_CHICKEN : Material.RAW_CHICKEN);
            String corpseName = cooked ? _config.DROPS_CORPSE_NAME_COOKED : _config.DROPS_CORPSE_NAME_RAW;
            setItemNameAndLore(corpse, corpseName, _config.DROPS_CORPSE_LORE);
            drops.add(corpse);
        }

        if (special) {
            if (Math.random() < _config.DROPS_WISHBONE_CHANCE + lootingBoost) {
                ItemStack wishBone = new ItemStack(Material.BONE);
                setItemNameAndLore(wishBone, _config.DROPS_WISHBONE_NAME, _config.DROPS_WISHBONE_LORE);
                drops.add(wishBone);
            }

            if (Math.random() < _config.DROPS_STUFFING_CHANCE + lootingBoost) {
                ItemStack stuffing = new ItemStack(Material.BREAD);
                setItemNameAndLore(stuffing, _config.DROPS_STUFFING_NAME, _config.DROPS_STUFFING_LORE);
                drops.add(stuffing);
            }

            if (Math.random() < _config.DROPS_CRANBERRY_SAUCE_CHANCE + lootingBoost) {
                ItemStack cranberrySauce = new ItemStack(Material.POTION, 1);
                PotionMeta meta = (PotionMeta) cranberrySauce.getItemMeta();
                meta.setBasePotionData(new PotionData(PotionType.INSTANT_HEAL, false, true));
                cranberrySauce.setItemMeta(meta);
                setItemNameAndLore(cranberrySauce, _config.DROPS_CRANBERRY_SAUCE_NAME, _config.DROPS_CRANBERRY_SAUCE_LORE);
                drops.add(cranberrySauce);
            }

            if (Math.random() < _config.DROPS_PUMPKIN_PIE_CHANCE + lootingBoost) {
                ItemStack pumpkinPie = new ItemStack(Material.PUMPKIN_PIE);
                setItemNameAndLore(pumpkinPie, _config.DROPS_PUMPKIN_PIE_NAME, _config.DROPS_PUMPKIN_PIE_LORE);
                drops.add(pumpkinPie);
            }
        }
    } // doCustomDrops

    // ------------------------------------------------------------------------
    /**
     * Set the item name and lore lines of an item.
     *
     * @param item the item.
     * @param name the name to give the item.
     * @param lore a list of lore lines to add in sequence, with colour codes
     *        already translated.
     */
    protected static void setItemNameAndLore(ItemStack item, String name, List<String> lores) {
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        meta.setLore(lores);
        item.setItemMeta(meta);
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the specified entity-related event occurred in the
     * overworld.
     *
     * @param event the event pertaining to an entity.
     * @return true if the specified entity-related event occurred in the
     *         overworld.
     */
    protected boolean isInOverworld(EntityEvent event) {
        return (event.getEntity().getWorld() == _overworld);
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the specified entity type is that of a hostile mob that
     * can and should be disguised as a chicken.
     *
     * @param type the entity's type.
     * @return true if the specified entity type is that of a hostile mob that
     *         can and should be disguised as a chicken.
     */
    protected boolean isDisguiseableHostileMob(EntityType type) {
        return type == EntityType.BLAZE ||
               type == EntityType.CREEPER ||
               type == EntityType.SPIDER ||
               type == EntityType.SKELETON ||
               type == EntityType.ZOMBIE;
    }

    // ------------------------------------------------------------------------
    /**
     * When a disguised creeper explodes, reinforcements are launched from him
     * with random velocities by this method.
     *
     * Reinforcements are always launched at a 45 degree angle, a configurable
     * range from the exploding creeper.
     *
     * @param creeper the exploding creeper.
     */
    protected void launchReinforcements(Entity creeper) {
        final int numReinforcements = random(_config.MIN_REINFORCEMENTS, _config.MAX_REINFORCEMENTS);
        for (int i = 0; i < numReinforcements; ++i) {
            // Compute unit velocity vector components, given 45 degree pitch.
            double yaw = 2.0 * Math.PI * Math.random();
            double y = INV_ROOT_2;
            double x = INV_ROOT_2 * Math.cos(yaw);
            double z = INV_ROOT_2 * Math.sin(yaw);

            // Spawn one reinforcement.
            Location origin = creeper.getLocation();
            Location loc = origin.clone().add(_config.REINFORCEMENT_RANGE * x, 0.5, _config.REINFORCEMENT_RANGE * z);
            Spider reinforcement = spawnReinforcement(loc);
            if (reinforcement != null) {
                double speed = random(_config.MIN_REINFORCEMENT_SPEED, _config.MAX_REINFORCEMENT_SPEED);
                Vector velocity = new Vector(speed * x, speed * y, speed * z);
                reinforcement.setVelocity(velocity);
            }
        }
    } // launchReinforcements

    // ------------------------------------------------------------------------
    /**
     * Spawn a disguised spider as a reinforcement at the specified location.
     *
     * @param loc the initial location of the spawned mob.
     * @return the spawned Spider.
     */
    protected Spider spawnReinforcement(Location loc) {
        World world = loc.getWorld();
        Spider reinforcement = (Spider) world.spawnEntity(loc, EntityType.SPIDER);
        if (reinforcement != null) {
            // Disguise the reinforcement as a baby chicken.
            addDisguise(reinforcement, false);

            // In order to hide the true actual spawned mob until LibsDisguises
            // can catch up and disguise it, spawn with invisibility for 5
            // ticks. Also, since LibsDisguises sometimes spawns disguises
            // invisible for the first few seconds (irrespective of the potion
            // effect), make the mobs slow and weak for a few seconds to give
            // players a chance.
            reinforcement.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 5, 1, false, false));
            reinforcement.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 20, false, false));
            reinforcement.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 2, false, false));

            loc.getWorld().spigot().playEffect(loc, Effect.EXPLOSION, 0, 0, 0.5f, 0.5f, 0.5f, 0, 10, 32);

            if (_config.DEBUG_SPAWN_REINFORCEMENT) {
                getLogger().info("Spawning reinforcement " + reinforcement.getType() +
                                 " at " + formatLocation(reinforcement.getLocation()));
            }
        }
        return reinforcement;
    }

    // ------------------------------------------------------------------------
    /**
     * Disguise a mob as a chicken, and add the DISGUISED_META metadata to make
     * it easier to track.
     *
     * Only disguised mobs can drop special drops, and they are undisguised and
     * killed (without drops) when the chunk unloads.
     *
     * Spawner mobs are never disguised.
     *
     * @param mob the mob.
     * @param isAdult if true, the disguise is an adult chicken; otherwise it is
     *        a baby.
     */
    protected void addDisguise(Entity mob, boolean isAdult) {
        mob.setMetadata(DISGUISED, DISGUISED_META);
        MobDisguise disguise = new MobDisguise(DisguiseType.CHICKEN, isAdult);
        DisguiseAPI.disguiseToAll(mob, disguise);
        if (mob instanceof Monster) {
            // Don't let disguised mobs pick up player gear. Players won't
            // easily find it, and the mob will be removed on restart.
            ((Monster) mob).setCanPickupItems(false);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Remove the disguise on a mob.
     *
     * @param mob the disguised mob.
     */
    protected void removeDisguise(Entity mob) {
        if (isDisguised(mob)) {
            DisguiseAPI.undisguiseToAll(mob);
            mob.removeMetadata(DISGUISED, this);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the specified mob is disguised as a chicken.
     *
     * @param mob the mob.
     * @return true if the specified mob is disguised as a chicken.
     */
    protected boolean isDisguised(Entity mob) {
        return mob.hasMetadata(DISGUISED);
    }

    // ------------------------------------------------------------------------
    /**
     * Remove all disguised mobs in the specified chunk.
     *
     * This method is called when the chunk is unloaded, or when the plugin is
     * disabled at restart.
     *
     * @param chunk the chunk.
     */
    protected void removeDisguisedMobs(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            if (isDisguised(entity)) {
                removeDisguise(entity);
                entity.remove();
                if (_config.DEBUG_REMOVE) {
                    getLogger().info("Removing disguised " + entity.getType() +
                                     " at " + formatLocation(entity.getLocation()));
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return a randomly selected name for a disguised mob.
     *
     * @return a randomly selected name for a disguised mob.
     */
    protected String getRandomMobName() {
        int index = random(0, _config.MESSAGES_TURKEY.size() - 1);
        return _config.MESSAGES_TURKEY.get(index);
    }

    // ------------------------------------------------------------------------
    /**
     * Format a Location as a string.
     *
     * @param loc the Location.
     * @return a String containing (world, x, y, z).
     */
    protected String formatLocation(Location loc) {
        StringBuilder s = new StringBuilder('(').append(loc.getWorld().getName());
        s.append(", ").append(loc.getBlockX());
        s.append(", ").append(loc.getBlockY());
        s.append(", ").append(loc.getBlockZ());
        return s.append(')').toString();
    }

    // ------------------------------------------------------------------------
    /**
     * Return a random integer in the range [min,max].
     *
     * @return a random integer in the range [min,max].
     */
    protected int random(int min, int max) {
        return min + (int) Math.rint(Math.random() * (max - min));
    }

    // ------------------------------------------------------------------------
    /**
     * Return a random double in the range [min,max].
     *
     * @return a random double in the range [min,max].
     */
    protected double random(double min, double max) {
        return min + Math.random() * (max - min);
    }

    // ------------------------------------------------------------------------
    /**
     * Metadata name used to tag mobs disguised entities, including mobs.
     */
    protected static final String DISGUISED = "IratePoultry_Disguised";

    /**
     * Shared metadata value for all disguised entities.
     */
    protected static FixedMetadataValue DISGUISED_META;

    /**
     * Metadata name used for metadata stored on mobs to record last damage time
     * (Long) by a player.
     */
    protected static final String PLAYER_DAMAGE_TIME = "IratePoultry_PlayerDamageTime";

    /**
     * Metadata name used for metadata stored on mobs to record looting
     * enchantment level of Looting weapon used by a player.
     */
    protected static final String PLAYER_LOOTING_LEVEL = "IratePoultry_PlayerLootingLevel";

    /**
     * Time in ticks (1/20ths of a second) for which player attack damage
     * "sticks" to a mob. The time between the last player damage on a mob and
     * its death must be less than this for it to drop special stuff.
     */
    protected static final int PLAYER_DAMAGE_TICKS = 100;

    /**
     * Inverse of the square root of 2; cos()/sin() of a 45 degree angle.
     */
    protected static final double INV_ROOT_2 = 1 / Math.sqrt(2.0);

    /**
     * Configuration.
     */
    protected Configuration _config = new Configuration(this);

    /**
     * Cached reference to the overworld.
     */
    protected World _overworld;
} // class IratePoultry
