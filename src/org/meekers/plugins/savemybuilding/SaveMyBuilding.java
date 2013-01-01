package org.meekers.plugins.savemybuilding;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

/**
 *
 * @author jaredm
 */
public class SaveMyBuilding extends JavaPlugin {

    private FileConfiguration customConfig;
    private File customConfigFile = null;
    public String customConfigFileName = "";

    public void onEnable() {
        this.saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(new SaveMyBuildingPluginListener(this), this);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("smb")) {
            Player player = Bukkit.getPlayer(sender.getName());
            Location ploc = player.getLocation();
            World pworld = player.getWorld();
            Block lookedAtBlock = player.getTargetBlock(null, 100);
            customConfigFileName = player.getName() + "-saves.yml";
            this.reloadCustomConfig();

            if (args.length < 1) {
                return false;
            }

            if ("save".equals(args[0])) {
                if (args.length < 5) {
                    player.sendMessage("/sms save bldgname length width height");
                    return true;
                }

                if (player.getGameMode() == GameMode.CREATIVE) {
                    player.sendMessage("");
                }
                String buildingName = args[1];
                HashMap<String, HashMap> building = new HashMap<String, HashMap>();

                // Record player game mode
                HashMap<String, Integer> gamemode = new HashMap<String, Integer>();
                gamemode.put("value", player.getGameMode().getValue());
                building.put(buildingName + ".gamemode", gamemode);

                // Force length and width to be positive, height can be negative or positive
                int length = Math.abs(Integer.parseInt(args[2]));
                int width = Math.abs(Integer.parseInt(args[3]));
                int height = Integer.parseInt(args[4]);

                // height of -1 is the level we're on... give one more so it does what we expect
                if (height < 0) {
                    height--;
                }

                // X is East (positive) to West (negative)
                // Z is North (negative) to South (positive)
                Vector direction = ploc.getDirection();
                double v_x = direction.getX();
                double v_z = direction.getZ();

                // Define our facing direction
                boolean facingXpos = false;
                boolean facingZpos = false;
                boolean facingX = false;

                // Choose base block
                Location baseLoc = lookedAtBlock.getLocation().clone();
                Location blockLoc = baseLoc.clone();
                blockLoc.setWorld(ploc.getWorld());

                if (Math.abs(v_x) > Math.abs(v_z)) {
                    // length is X, width is Z
                    facingX = true;
                    if (v_x > 0) {
                        facingXpos = true;  //East
                    }
                    baseLoc.setX(baseLoc.getX());
                } else {
                    // length is Z, width is X
                    if (v_z > 0) {
                        facingZpos = true; // South
                    }
                    baseLoc.setZ(baseLoc.getZ());
                }

                // Loop over user-specified dimensions
                int blockCount = 0;
                for (int l = 0; l < length; l++) {
                    if (facingX) {
                        if (facingXpos) {
                            blockLoc.setX(baseLoc.getX() + l);
                        } else {
                            blockLoc.setX(baseLoc.getX() - l);
                        }
                    } else {
                        if (facingZpos) {
                            blockLoc.setZ(baseLoc.getZ() + l);
                        } else {
                            blockLoc.setZ(baseLoc.getZ() - l);
                        }
                    }
                    for (int h = 0; h < Math.abs(height); h++) {
                        if (height > 0) {
                            blockLoc.setY(baseLoc.getY() + h);
                        } else {
                            blockLoc.setY(baseLoc.getY() - h);
                        }
                        for (int w = 0; w < width; w++) {
                            if (facingX) {
                                if (facingXpos) {
                                    blockLoc.setZ(baseLoc.getZ() + w);
                                } else {
                                    blockLoc.setZ(baseLoc.getZ() - w);
                                }
                            } else {
                                if (facingZpos) {
                                    blockLoc.setX(baseLoc.getX() - w);
                                } else {
                                    blockLoc.setX(baseLoc.getX() + w);
                                }
                            }

                            // Save block type and position
                            blockCount++;
                            HashMap<String, Integer> blockmap = new HashMap<String, Integer>();
                            Block baseBlock = pworld.getBlockAt(blockLoc);
                            String s = String.format("%d", baseBlock.getData());
                            int si = Integer.parseInt(s);
                            int xoffset = baseBlock.getX() - lookedAtBlock.getX();
                            int yoffset = baseBlock.getY() - lookedAtBlock.getY();
                            int zoffset = baseBlock.getZ() - lookedAtBlock.getZ();
                            blockmap.put("x", xoffset);
                            blockmap.put("y", yoffset);
                            blockmap.put("z", zoffset);
                            blockmap.put("type", baseBlock.getTypeId());
                            blockmap.put("data", si);
                            building.put(buildingName + ".b" + blockCount, blockmap);
                        }
                    }
                }

                // Save the building
                Iterator it = building.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pairs = (Map.Entry) it.next();
                    customConfig.set((String) pairs.getKey(), pairs.getValue());
                }
                saveCustomConfig();
                reloadCustomConfig();
                player.sendMessage(buildingName + " successfully saved.");
                return true;
            } else if ("list".equals(args[0])) {
                player.sendMessage("Available saved buildings: ");
                Set<String> names = customConfig.getKeys(false);
                String buildings = "";
                int count = 0;
                for (String bldgname : names) {
                    count++;
                    int gamemode = customConfig.getInt(bldgname + ".gamemode.value");
                    if (count > 1) {
                        buildings += ", ";
                    }
                    buildings += bldgname + " (" + GameMode.getByValue(gamemode).name().toLowerCase() + ")";
                }
                player.sendMessage(buildings);
                return true;
            } else if ("load".equalsIgnoreCase(args[0])) {
                //this.getCustomConfig();
                if (args.length != 2) {
                    player.sendMessage("/sms load bldgname");
                    return true;
                }

                String bldgname = args[1];

                int gamemode = customConfig.getInt(bldgname + ".gamemode.value");
                if (player.getGameMode().getValue() != gamemode) {
                    player.sendMessage("You cannot restore buildings across game modes. Switch to " + GameMode.getByValue(gamemode) + " to restore " + bldgname);
                    return true;
                }
                player.sendMessage("Loading " + bldgname + "...");

                // Load the building
                int startX = lookedAtBlock.getX();
                int startY = lookedAtBlock.getY() + 1;
                int startZ = lookedAtBlock.getZ();

                Set<String> blocks = customConfig.getConfigurationSection(bldgname).getKeys(false);
                placeBlocks(player, bldgname, startX, startY, startZ, blocks, true);
                player.sendMessage("Load complete.");
                return true;
            }
        }
        return false;
    }

    public void placeBlocks(Player player, String bldgname, int startX, int startY, int startZ, Set<String> blocks, boolean enableignores) {
        int blockX = 0;
        int blockY = 0;
        int blockZ = 0;
        int blockType = 0;
        byte blockData = 0;

        // For saving things like torches
        Set<String> lastBlocks = new HashSet();

        for (String blockName : blocks) {
            if ("gamemode".equalsIgnoreCase(blockName)) {
                continue;
            }
            Map<String, Object> bldgconfig = customConfig.getConfigurationSection(bldgname + "." + blockName).getValues(true);
            Iterator it = bldgconfig.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry) it.next();
                String key = pairs.getKey().toString();
                int value = (int) pairs.getValue();
                if ("x".equals(key)) {
                    blockX = startX + value;
                } else if ("y".equals(key)) {
                    blockY = startY + value;
                } else if ("z".equals(key)) {
                    blockZ = startZ + value;
                } else if ("type".equals(key)) {
                    blockType = value;
                } else if ("data".equals(key)) {
                    blockData = (byte) value;
                }
            }
            Block newblock = player.getWorld().getBlockAt(blockX, blockY, blockZ);
            if (blockType == Material.TORCH.getId() && enableignores == true) {
                lastBlocks.add(blockName);
                continue;
            }
            if (newblock.getTypeId() != blockType) {
                newblock.setTypeIdAndData(blockType, blockData, false);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SaveMyBuilding.class.getName()).log(Level.SEVERE, null, ex);
                }
                player.sendBlockChange(newblock.getLocation(), Material.getMaterial(blockType), blockData);
            }
        }
        if (lastBlocks.size() > 0) {
            placeBlocks(player, bldgname, startX, startY, startZ, lastBlocks, false);
        }
    }

    //Implementation for Reloading
    //Then, write the method that is responsible for loading the config from disk. It will load the file, and search the jar for a default yml file.
    public void reloadCustomConfig() {
        if (customConfigFile == null) {
            //this.getLogger().log(Level.SEVERE, "saving this file: " + customConfigFileName);
            customConfigFile = new File(getDataFolder(), customConfigFileName);
        }
        customConfig = YamlConfiguration.loadConfiguration(customConfigFile);

        // Look for defaults in the jar
        InputStream defConfigStream = this.getResource(customConfigFileName);
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            customConfig.setDefaults(defConfig);
        }
    }

    //Implementation for Getting
    //Next, you need to write the getter method. Check if customConfig is null, if it is load from disk.
    public FileConfiguration getCustomConfig() {
        if (customConfig == null) {
            this.reloadCustomConfig();
        }
        return customConfig;
    }

    //Implementation for Saving
    //Finally, write the save method, which saves changes and overwrites the file on disk.
    public void saveCustomConfig() {
        if (customConfig == null || customConfigFile == null) {
            return;
        }
        try {
            getCustomConfig().save(customConfigFile);
        } catch (IOException ex) {
            this.getLogger().log(Level.SEVERE, "Could not save config to " + customConfigFile, ex);
        }
    }
}
