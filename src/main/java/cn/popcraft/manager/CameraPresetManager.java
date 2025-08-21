package cn.popcraft.manager;

import cn.popcraft.VirtualCamera;
import cn.popcraft.model.CameraPreset;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CameraPresetManager {
    private final VirtualCamera plugin;
    private final Map<String, CameraPreset> presets;

    public CameraPresetManager(VirtualCamera plugin) {
        this.plugin = plugin;
        this.presets = new HashMap<>();
        loadPresets();
    }

    /**
     * 加载所有预设
     */
    public void loadPresets() {
        presets.clear();
        FileConfiguration config = plugin.getPlugin().getConfig();
        ConfigurationSection presetsSection = config.getConfigurationSection("presets");
        if (presetsSection != null) {
            Set<String> presetNames = presetsSection.getKeys(false);
            for (String name : presetNames) {
                String path = "presets." + name + ".";
                double x = config.getDouble(path + "x");
                double y = config.getDouble(path + "y");
                double z = config.getDouble(path + "z");
                float yaw = (float) config.getDouble(path + "yaw");
                float pitch = (float) config.getDouble(path + "pitch");
                String worldName = config.getString(path + "world");

                CameraPreset preset = new CameraPreset(name);
                Location location = new Location(
                    plugin.getPlugin().getServer().getWorld(worldName),
                    x, y, z, yaw, pitch
                );
                preset.addLocation(location);

                presets.put(name, preset);
            }
        }
    }

    /**
     * 保存预设
     * @param preset 预设
     */
    public void savePreset(CameraPreset preset) {
        presets.put(preset.getName(), preset);
        
        FileConfiguration config = plugin.getPlugin().getConfig();
        String path = "presets." + preset.getName() + ".";
        
        // 获取预设的第一个位置信息
        if (!preset.getLocations().isEmpty()) {
            Location location = preset.getLocations().get(0);
            config.set(path + "x", location.getX());
            config.set(path + "y", location.getY());
            config.set(path + "z", location.getZ());
            config.set(path + "yaw", location.getYaw());
            config.set(path + "pitch", location.getPitch());
            config.set(path + "world", location.getWorld().getName());
        }
        
        plugin.getPlugin().saveConfig();
    }

    /**
     * 删除预设
     * @param name 预设名称
     * @return 是否成功删除
     */
    public boolean deletePreset(String name) {
        if (presets.containsKey(name)) {
            presets.remove(name);
            
            FileConfiguration config = plugin.getPlugin().getConfig();
            config.set("presets." + name, null);
            plugin.getPlugin().saveConfig();
            
            return true;
        }
        return false;
    }

    /**
     * 获取预设
     * @param name 预设名称
     * @return 预设对象，如果不存在则返回null
     */
    public CameraPreset getPreset(String name) {
        return presets.get(name);
    }

    /**
     * 获取所有预设
     * @return 预设映射
     */
    public Map<String, CameraPreset> getPresets() {
        return presets;
    }
}