package cn.popcraft.manager;

import cn.popcraft.model.CameraPreset;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PresetManager {
    private final JavaPlugin plugin;
    private final Map<String, CameraPreset> presets = new HashMap<>();

    public PresetManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadPresets();
    }

    /**
     * 加载所有预设
     */
    public void loadPresets() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection presetsSection = config.getConfigurationSection("presets");
        if (presetsSection == null) {
            plugin.getLogger().warning("未找到预设配置");
            return;
        }

        presets.clear();
        for (String presetName : presetsSection.getKeys(false)) {
            ConfigurationSection presetSection = presetsSection.getConfigurationSection(presetName);
            if (presetSection == null) continue;

            CameraPreset preset = new CameraPreset(presetName);
            
            // 加载位置点
            // 加载位置点
            List<Map<String, Object>> locationMaps = presetSection.getMapList("locations").stream()
                .map(map -> (Map<String, Object>) map)
                .collect(Collectors.toList());
            List<Location> locations = locationMaps.stream()
                .map(this::mapToLocation)
                .collect(Collectors.toList());
            preset.setLocations(locations);

            // 加载命令
            List<Map<String, Object>> commandMaps = presetSection.getMapList("commands").stream()
                .map(map -> (Map<String, Object>) map)
                .collect(Collectors.toList());
            for (Map<String, Object> cmdMap : commandMaps) {
                String command = (String) cmdMap.get("command");
                long delayMs = ((Number) cmdMap.getOrDefault("delay", 0L)).longValue();
                long safeDelay = delayMs > Integer.MAX_VALUE ? Integer.MAX_VALUE : delayMs;
                preset.addCommand(command, safeDelay);
            }

            // 加载文本
            List<Map<String, Object>> textMaps = presetSection.getMapList("texts").stream()
                .map(map -> (Map<String, Object>) map)
                .collect(Collectors.toList());
            for (Map<String, Object> textMap : textMaps) {
                String text = (String) textMap.get("text");
                long delayMs = ((Number) textMap.getOrDefault("delay", 0L)).longValue();
                long safeDelay = delayMs > Integer.MAX_VALUE ? Integer.MAX_VALUE : delayMs;
                preset.addText(text, safeDelay);
            }

            // 设置相机类型
            String typeStr = presetSection.getString("type", "NORMAL");
            preset.setType(CameraPreset.CameraType.valueOf(typeStr));

            presets.put(presetName, preset);
        }
    }

    /**
     * 将Map转换为Location
     */
    private Location mapToLocation(Map<String, Object> map) {
        String world = (String) map.get("world");
        double x = ((Number) map.get("x")).doubleValue();
        double y = ((Number) map.get("y")).doubleValue();
        double z = ((Number) map.get("z")).doubleValue();
        float yaw = ((Number) map.getOrDefault("yaw", 0f)).floatValue();
        float pitch = ((Number) map.getOrDefault("pitch", 0f)).floatValue();
        return new Location(plugin.getServer().getWorld(world), x, y, z, yaw, pitch);
    }

    /**
     * 获取预设
     */
    public CameraPreset getPreset(String name) {
        return presets.get(name);
    }

    /**
     * 获取所有预设名称
     */
    public List<String> getPresetNames() {
        return new ArrayList<>(presets.keySet());
    }
}