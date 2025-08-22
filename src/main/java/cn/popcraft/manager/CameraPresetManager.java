package cn.popcraft.manager;

import cn.popcraft.VirtualCamera;
import cn.popcraft.model.CameraPreset;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CameraPresetManager {
    private final VirtualCamera plugin;
    private final Map<String, CameraPreset> presets = new ConcurrentHashMap<>();

    public CameraPresetManager(VirtualCamera plugin) {
        this.plugin = plugin;
        loadPresets();
    }

    /**
     * 从配置文件加载预设
     */
    private void loadPresets() {
        FileConfiguration config = plugin.getPlugin().getConfig();
        ConfigurationSection presetsSection = config.getConfigurationSection("presets");
        
        if (presetsSection == null) {
            return;
        }

        for (String presetName : presetsSection.getKeys(false)) {
            ConfigurationSection presetSection = presetsSection.getConfigurationSection(presetName);
            if (presetSection != null) {
                CameraPreset preset = new CameraPreset(presetName);
                
                // 加载相机类型
                String typeStr = presetSection.getString("type", "NORMAL");
                try {
                    CameraPreset.CameraType type = CameraPreset.CameraType.valueOf(typeStr);
                    preset.setType(type);
                } catch (IllegalArgumentException e) {
                    preset.setType(CameraPreset.CameraType.NORMAL);
                }
                
                // 加载位置点
                ConfigurationSection locationsSection = presetSection.getConfigurationSection("locations");
                if (locationsSection != null) {
                    for (String locKey : locationsSection.getKeys(false)) {
                        ConfigurationSection locSection = locationsSection.getConfigurationSection(locKey);
                        if (locSection != null) {
                            double x = locSection.getDouble("x");
                            double y = locSection.getDouble("y");
                            double z = locSection.getDouble("z");
                            float yaw = (float) locSection.getDouble("yaw");
                            float pitch = (float) locSection.getDouble("pitch");
                            
                            Location loc = new Location(null, x, y, z, yaw, pitch);
                            preset.addLocation(loc);
                        }
                    }
                }
                
                presets.put(presetName, preset);
            }
        }
    }

    /**
     * 获取预设
     * @param name 预设名称
     * @return 相机预设，如果不存在则返回null
     */
    public CameraPreset getPreset(String name) {
        return presets.get(name);
    }

    /**
     * 添加预设
     * @param name 预设名称
     * @param preset 相机预设
     */
    public void addPreset(String name, CameraPreset preset) {
        presets.put(name, preset);
    }

    /**
     * 移除预设
     * @param name 预设名称
     * @return 被移除的预设，如果不存在则返回null
     */
    public CameraPreset removePreset(String name) {
        return presets.remove(name);
    }

    /**
     * 获取所有预设名称
     * @return 预设名称集合
     */
    public Iterable<String> getPresetNames() {
        return presets.keySet();
    }

    /**
     * 检查预设是否存在
     * @param name 预设名称
     * @return 是否存在
     */
    public boolean hasPreset(String name) {
        return presets.containsKey(name);
    }

    /**
     * 获取预设数量
     * @return 预设数量
     */
    public int getPresetCount() {
        return presets.size();
    }
}