package io.totemo.iratepoultry;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;

// ----------------------------------------------------------------------------
/**
 * Reads and exposes the plugin configuration.
 */
public class Configuration {

    public double BLAZE_CHANCE;
    public double BLAST_RADIUS_SCALE;
    public int MIN_REINFORCEMENTS;
    public int MAX_REINFORCEMENTS;
    public double REINFORCEMENT_RANGE;
    public double MIN_REINFORCEMENT_SPEED;
    public double MAX_REINFORCEMENT_SPEED;
    public double REINFORCEMENT_EGG_CHANCE;

    public double DROPS_FEATHER_CHANCE;
    public String DROPS_FEATHER_NAME;
    public ArrayList<String> DROPS_FEATHER_LORE;

    public double DROPS_EGG_CHANCE;
    public String DROPS_EGG_NAME;
    public ArrayList<String> DROPS_EGG_LORE;

    public double DROPS_WISHBONE_CHANCE;
    public String DROPS_WISHBONE_NAME;
    public ArrayList<String> DROPS_WISHBONE_LORE;

    public double DROPS_CORPSE_CHANCE;
    public String DROPS_CORPSE_NAME_RAW;
    public String DROPS_CORPSE_NAME_COOKED;
    public ArrayList<String> DROPS_CORPSE_LORE;

    public double DROPS_STUFFING_CHANCE;
    public String DROPS_STUFFING_NAME;
    public ArrayList<String> DROPS_STUFFING_LORE;

    public double DROPS_CRANBERRY_SAUCE_CHANCE;
    public String DROPS_CRANBERRY_SAUCE_NAME;
    public ArrayList<String> DROPS_CRANBERRY_SAUCE_LORE;

    public double DROPS_PUMPKIN_PIE_CHANCE;
    public String DROPS_PUMPKIN_PIE_NAME;
    public ArrayList<String> DROPS_PUMPKIN_PIE_LORE;

    public boolean DEBUG_DEATH;
    public boolean DEBUG_REMOVE;
    public boolean DEBUG_SPAWN_NATURAL;
    public boolean DEBUG_SPAWN_REINFORCEMENT;

    // ------------------------------------------------------------------------
    /**
     * Constructor.
     *
     * @param plugin the owning plugin.
     */
    public Configuration(Plugin plugin) {
        _plugin = plugin;
    }

    // ------------------------------------------------------------------------
    /**
     * Load the plugin configuration.
     */
    public void reload() {
        _plugin.reloadConfig();
        BLAZE_CHANCE = _plugin.getConfig().getDouble("difficulty.blaze.chance", 0.1);
        BLAST_RADIUS_SCALE = _plugin.getConfig().getDouble("difficulty.blastscale", 0.3);
        MIN_REINFORCEMENTS = _plugin.getConfig().getInt("reinforcements.min", 1);
        MAX_REINFORCEMENTS = _plugin.getConfig().getInt("reinforcements.max", 3);
        REINFORCEMENT_RANGE = _plugin.getConfig().getDouble("reinforcements.range", 3.0);
        MIN_REINFORCEMENT_SPEED = _plugin.getConfig().getDouble("reinforcements.velocity.min", 1);
        MAX_REINFORCEMENT_SPEED = _plugin.getConfig().getDouble("reinforcements.velocity.max", 3);
        REINFORCEMENT_EGG_CHANCE = _plugin.getConfig().getDouble("reinforcements.egg.chance", 0.2);

        DROPS_FEATHER_CHANCE = _plugin.getConfig().getDouble("drops.feather.chance", 0.4);
        DROPS_FEATHER_NAME = _plugin.getConfig().getString("drops.feather.name", "Feather");
        DROPS_FEATHER_LORE = loadAndTranslateLore("drops.feather.lore");

        DROPS_EGG_CHANCE = _plugin.getConfig().getDouble("drops.egg.chance", 0.2);
        DROPS_EGG_NAME = _plugin.getConfig().getString("drops.egg.name", "Egg");
        DROPS_EGG_LORE = loadAndTranslateLore("drops.egg.lore");

        DROPS_WISHBONE_CHANCE = _plugin.getConfig().getDouble("drops.wishbone.chance", 0.025);
        DROPS_WISHBONE_NAME = _plugin.getConfig().getString("drops.wishbone.name", "Wishbone");
        DROPS_WISHBONE_LORE = loadAndTranslateLore("drops.wishbone.lore");

        DROPS_STUFFING_CHANCE = _plugin.getConfig().getDouble("drops.stuffing.chance", 0.2);
        DROPS_STUFFING_NAME = _plugin.getConfig().getString("drops.stuffing.name", "Stuffing");
        DROPS_STUFFING_LORE = loadAndTranslateLore("drops.stuffing.lore");

        DROPS_CORPSE_CHANCE = _plugin.getConfig().getDouble("drops.corpse.chance", 0.2);
        DROPS_CORPSE_NAME_RAW = _plugin.getConfig().getString("drops.corpse.name.raw", "Raw Poultry");
        DROPS_CORPSE_NAME_COOKED = _plugin.getConfig().getString("drops.corpse.name.cooked", "Cooked Poultry");
        DROPS_CORPSE_LORE = loadAndTranslateLore("drops.corpse.lore");

        DROPS_CRANBERRY_SAUCE_CHANCE = _plugin.getConfig().getDouble("drops.cranberry_sauce.chance", 0.05);
        DROPS_CRANBERRY_SAUCE_NAME = _plugin.getConfig().getString("drops.cranberry_sauce.name", "Cranberry Sauce");
        DROPS_CRANBERRY_SAUCE_LORE = loadAndTranslateLore("drops.cranberry_sauce.lore");

        DROPS_PUMPKIN_PIE_CHANCE = _plugin.getConfig().getDouble("drops.pumpkin_pie.chance", 0.2);
        DROPS_PUMPKIN_PIE_NAME = _plugin.getConfig().getString("drops.pumpkin_pie.name", "Pumpkin Pie");
        DROPS_PUMPKIN_PIE_LORE = loadAndTranslateLore("drops.pumpkin_pie.lore");

        DEBUG_DEATH = _plugin.getConfig().getBoolean("debug.death", false);
        DEBUG_REMOVE = _plugin.getConfig().getBoolean("debug.remove", false);
        DEBUG_SPAWN_NATURAL = _plugin.getConfig().getBoolean("debug.spawn.natural", false);
        DEBUG_SPAWN_REINFORCEMENT = _plugin.getConfig().getBoolean("debug.spawn.reinforcement", false);
    } // reload

    // ------------------------------------------------------------------------
    /**
     * Load the specified key from the configuration, split it into parts,
     * separated by '|', and translate alternate colour codes in the parts to
     * make a list of lore strings.
     *
     * @param key the config key.
     * @return a list of lore strings.
     */
    protected ArrayList<String> loadAndTranslateLore(String key) {
        ArrayList<String> loreList = new ArrayList<String>();
        for (String lore : _plugin.getConfig().getString(key, "").split("\\|")) {
            loreList.add(ChatColor.translateAlternateColorCodes('&', lore));
        }
        return loreList;
    }

    // ------------------------------------------------------------------------

    public static void main(String[] args) {
        String[] parts = "&6Thanksgiving 2015|&oYou knocked the stuffing out of it!".split("\\|");
        for (String part : parts) {
            System.out.println(part);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Owning plugin.
     */
    private final Plugin _plugin;
} // class Configuration