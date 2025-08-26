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

@SuppressWarnings("unchecked")

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
            List<Map<?, ?>> locationMaps = presetSection.getMapList("locations");
            List<Location> locations = new ArrayList<>();
            for (Map<?, ?> map : locationMaps) {
                locations.add(mapToLocation((Map<String, Object>) map));
            }
            preset.setLocations(locations);

            // 加载命令
            List<Map<?, ?>> commandMaps = presetSection.getMapList("commands");
            for (Map<?, ?> cmdMap : commandMaps) {
                Map<String, Object> typedCmdMap = (Map<String, Object>) cmdMap;
                String command = (String) typedCmdMap.get("command");
                long delayMs = ((Number) typedCmdMap.getOrDefault("delay", 0L)).longValue();
                long safeDelay = delayMs > Integer.MAX_VALUE ? Integer.MAX_VALUE : delayMs;
                preset.addCommand(command, safeDelay);
            }

            // 加载文本
            List<Map<?, ?>> textMaps = presetSection.getMapList("texts");
            for (Map<?, ?> textMap : textMaps) {
                Map<String, Object> typedTextMap = (Map<String, Object>) textMap;
                String text = (String) typedTextMap.get("text");
                long delayMs = ((Number) typedTextMap.getOrDefault("delay", 0L)).longValue();
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