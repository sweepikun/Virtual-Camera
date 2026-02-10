package cn.popcraft.manager;

import cn.popcraft.VirtualCamera;
import cn.popcraft.model.CameraPreset;
import cn.popcraft.model.TransitionType;
import cn.popcraft.session.CameraSession;
import cn.popcraft.session.SessionManager;
import cn.popcraft.util.Timeline;
import cn.popcraft.model.Camera;
import cn.popcraft.model.CameraSequence;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 相机管理器
 * 负责管理相机预设、序列和相机模式的进入/退出
 */
public class CameraManager {
    private final VirtualCamera plugin;
    private final SessionManager sessionManager;
    
    // 存储所有预设
    private final Map<String, CameraPreset> presets = new ConcurrentHashMap<>();
    
    // 存储所有序列
    private final Map<String, CameraSequence> sequences = new ConcurrentHashMap<>();
    
    // 存储正在播放序列的玩家任务
    private final Map<UUID, BukkitTask> sequenceTasks = new HashMap<>();

    /**
     * 构造函数
     * @param plugin 插件实例
     * @param sessionManager 会话管理器
     */
    public CameraManager(VirtualCamera plugin, SessionManager sessionManager) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
        
        // 从配置文件加载预设和序列
        loadFromConfig();
        
        // 从单独的预设文件加载预设
        loadAllPresets();
        
        // 从单独的序列文件加载序列
        loadAllSequences();
    }
    
    /**
     * 切换到指定预设
     * @param player 玩家
     * @param presetName 预设名称
     * @return 是否成功切换
     */
    public boolean switchToPreset(Player player, String presetName) {
        CameraPreset preset = plugin.getPresetManager().getPreset(presetName);
        if (preset == null || preset.getLocations().isEmpty()) {
            // 尝试从CameraManager中获取预设
            preset = presets.get(presetName);
            if (preset == null || preset.getLocations().isEmpty()) {
                player.sendMessage(ChatColor.RED + ChatColor.translateAlternateColorCodes('&', "找不到预设: " + presetName));
                return false;
            }
        }
        
        CameraSession session = sessionManager.getSession(player);
        
        // 如果玩家不在相机模式，先进入相机模式
        if (!session.isInCameraMode()) {
            enterCameraMode(player);
        }
        
        // 构建时间轴
        Timeline timeline = new Timeline();
        
        // 添加路径点(使用玩家当前世界)
        for (Location loc : preset.getLocations()) {
            Location newLoc = loc.clone();
            newLoc.setWorld(player.getWorld());
            timeline.addKeyframe(newLoc);
        }
        
        // 设置每个段落的过渡类型和持续时间
        for (int i = 0; i < preset.getSegmentInfos().size(); i++) {
            CameraPreset.SegmentInfo segmentInfo = preset.getSegmentInfos().get(i);
            timeline.setSegmentTransition(i, segmentInfo.getTransitionType(), segmentInfo.getDuration());
        }
        
        // 添加文本动作
        for (CameraPreset.TextAction text : preset.getTexts()) {
            timeline.addTextAction(text.getDelay(), text);
        }
        
        // 添加命令动作
        for (CameraPreset.CommandAction cmd : preset.getCommands()) {
            timeline.addCommandAction(cmd.getDelay(), cmd);
        }
        
        // 设置时间轴并启动动画
        session.setTimeline(timeline);
        
        // 使用ProtocolLib摄像机控制器播放动画（如果可用）
        if (plugin instanceof cn.popcraft.VirtualCameraPlugin) {
            cn.popcraft.VirtualCameraPlugin vcPlugin = (cn.popcraft.VirtualCameraPlugin) plugin;
            if (vcPlugin.getProtocolCameraController() != null) {
                // 计算总持续时间
                long totalDuration = 0;
                for (CameraPreset.SegmentInfo segmentInfo : preset.getSegmentInfos()) {
                    totalDuration += segmentInfo.getDuration();
                }
                
                // 如果没有段落信息，则使用默认时间
                if (totalDuration == 0 && preset.getLocationCount() > 1) {
                    totalDuration = (preset.getLocationCount() - 1) * 3000; // 默认每段3秒
                }
                
                session.playProtocolCameraAnimation(vcPlugin, timeline, totalDuration);
            } else {
                // 回退到原来的实现
                session.startAnimation();
            }
        } else {
            // 回退到原来的实现
            session.startAnimation();
        }
        
        player.sendMessage(ChatColor.GREEN + ChatColor.translateAlternateColorCodes('&', "开始播放预设: " + presetName));
        return true;
    }

    /**
     * 调度预设命令
     * @param player 玩家
     * @param preset 相机预设
     */
    private void scheduleCommands(Player player, CameraPreset preset) {
        for (CameraPreset.CommandAction cmd : preset.getCommands()) {
            long delayMs = cmd.getDelay();
            long ticks = delayMs / 50;
            long safeTicks = ticks;
            if (safeTicks <= Integer.MAX_VALUE) {
                Bukkit.getScheduler().runTaskLater(
                    plugin.getPlugin(),
                    () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.getCommand()),
                    safeTicks
                );
            } else {
                // 对于超大延迟，分段执行
                Bukkit.getScheduler().runTaskLater(
                    plugin.getPlugin(),
                    () -> scheduleCommandWithDelay(cmd.getCommand(), safeTicks - Integer.MAX_VALUE),
                    Integer.MAX_VALUE
                );
            }
        }
    }
    
    /**
     * 处理超大延迟的命令执行
     * @param command 要执行的命令
     * @param remainingTicks 剩余的tick数
     */
    private void scheduleCommandWithDelay(String command, long remainingTicks) {
        if (remainingTicks <= Integer.MAX_VALUE) {
            Bukkit.getScheduler().runTaskLater(
                plugin.getPlugin(),
                () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command),
                remainingTicks
            );
        } else {
            // 继续分段执行
            Bukkit.getScheduler().runTaskLater(
                plugin.getPlugin(),
                () -> scheduleCommandWithDelay(command, remainingTicks - Integer.MAX_VALUE),
                Integer.MAX_VALUE
            );
        }
    }

    /**
     * 调度预设文本
     * @param player 玩家
     * @param preset 相机预设
     */
    private void scheduleTexts(Player player, CameraPreset preset) {
        for (CameraPreset.TextAction text : preset.getTexts()) {
            long delayMs = text.getDelay();
            long ticks = delayMs / 50;
            long safeTicks = ticks;
            
            if (safeTicks <= Integer.MAX_VALUE) {
                Bukkit.getScheduler().runTaskLater(
                    plugin.getPlugin(),
                    () -> scheduleTextDisplay(player, text),
                    safeTicks
                );
            } else {
                // 对于超大延迟，分段执行
                Bukkit.getScheduler().runTaskLater(
                    plugin.getPlugin(),
                    () -> scheduleTextWithDelay(player, text, safeTicks - Integer.MAX_VALUE),
                    Integer.MAX_VALUE
                );
            }
        }
    }
    
    /**
     * 处理超大延迟的文本显示
     * @param player 玩家
     * @param text 文本动作
     * @param remainingTicks 剩余的tick数
     */
    private void scheduleTextWithDelay(Player player, CameraPreset.TextAction text, long remainingTicks) {
        if (remainingTicks <= Integer.MAX_VALUE) {
            Bukkit.getScheduler().runTaskLater(
                plugin.getPlugin(),
                () -> scheduleTextDisplay(player, text),
                remainingTicks
            );
        } else {
            // 继续分段执行
            Bukkit.getScheduler().runTaskLater(
                plugin.getPlugin(),
                () -> scheduleTextWithDelay(player, text, remainingTicks - Integer.MAX_VALUE),
                Integer.MAX_VALUE
            );
        }
    }
    
    /**
     * 显示文本并处理持续时间
     * @param player 玩家
     * @param text 文本动作
     */
    private void scheduleTextDisplay(Player player, CameraPreset.TextAction text) {
        player.sendActionBar(org.bukkit.ChatColor.translateAlternateColorCodes('&', text.getText()));
        // 如果设置了持续时间，调度文本消失
        if (text.getDuration() > 0) {
            long durationTicks = text.getDuration() / 50;
            long safeDurationTicks = durationTicks;
            
            if (safeDurationTicks <= Integer.MAX_VALUE) {
                Bukkit.getScheduler().runTaskLater(
                    plugin.getPlugin(),
                    () -> player.sendActionBar(""),
                    safeDurationTicks
                );
            } else {
                // 对于超大持续时间，分段执行
                Bukkit.getScheduler().runTaskLater(
                    plugin.getPlugin(),
                    () -> scheduleClearActionBar(player, safeDurationTicks - Integer.MAX_VALUE),
                    Integer.MAX_VALUE
                );
            }
        }
    }
    
    /**
     * 处理超大延迟的清除动作栏
     * @param player 玩家
     * @param remainingTicks 剩余的tick数
     */
    private void scheduleClearActionBar(Player player, long remainingTicks) {
        if (remainingTicks <= Integer.MAX_VALUE) {
            Bukkit.getScheduler().runTaskLater(
                plugin.getPlugin(),
                () -> player.sendActionBar(""),
                remainingTicks
            );
        } else {
            // 继续分段执行
            Bukkit.getScheduler().runTaskLater(
                plugin.getPlugin(),
                () -> scheduleClearActionBar(player, remainingTicks - Integer.MAX_VALUE),
                Integer.MAX_VALUE
            );
        }
    }

    /**
     * 从配置文件加载预设和序列
     */
    private void loadFromConfig() {
        FileConfiguration config = plugin.getPlugin().getConfig();
        
        // 加载预设
        ConfigurationSection presetsSection = config.getConfigurationSection("presets");
        if (presetsSection != null) {
            for (String presetName : presetsSection.getKeys(false)) {
                ConfigurationSection presetSection = presetsSection.getConfigurationSection(presetName);
                if (presetSection != null) {
                    CameraPreset preset = new CameraPreset(presetName);
                    
                    // 加载相机类型
                    CameraPreset.CameraType type = CameraPreset.CameraType.valueOf(presetSection.getString("type", "NORMAL"));
                    preset.setType(type);
                    
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
                    
                    // 加载命令
                    ConfigurationSection commandsSection = presetSection.getConfigurationSection("commands");
                    if (commandsSection != null) {
                        for (String cmdKey : commandsSection.getKeys(false)) {
                            ConfigurationSection cmdSection = commandsSection.getConfigurationSection(cmdKey);
                            if (cmdSection != null) {
                                String command = cmdSection.getString("command");
                                long delay = cmdSection.getLong("delay");
                                if (command != null) {
                                    preset.addCommand(command, delay);
                                }
                            }
                        }
                    }
                    
                    // 加载文本
                    ConfigurationSection textsSection = presetSection.getConfigurationSection("texts");
                    if (textsSection != null) {
                        for (String textKey : textsSection.getKeys(false)) {
                            ConfigurationSection textSection = textsSection.getConfigurationSection(textKey);
                            if (textSection != null) {
                                String text = textSection.getString("text");
                                long delay = textSection.getLong("delay");
                                long duration = textSection.getLong("duration", 3000); // 默认持续3秒
                                if (text != null) {
                                    preset.addText(text, delay, duration);
                                }
                            }
                        }
                    }
                    
                    presets.put(presetName, preset);
                }
            }
        }
        
        // 加载序列
        ConfigurationSection sequencesSection = config.getConfigurationSection("sequences");
        if (sequencesSection != null) {
            for (String sequenceName : sequencesSection.getKeys(false)) {
                ConfigurationSection sequenceSection = sequencesSection.getConfigurationSection(sequenceName);
                if (sequenceSection != null) {
                    CameraSequence sequence = new CameraSequence(sequenceName);
                    
                    // 设置是否循环
                    boolean loop = sequenceSection.getBoolean("loop", false);
                    sequence.setLoop(loop);
                    
                    // 加载序列条目
                    ConfigurationSection entriesSection = sequenceSection.getConfigurationSection("entries");
                    if (entriesSection != null) {
                        for (String entryKey : entriesSection.getKeys(false)) {
                            ConfigurationSection entrySection = entriesSection.getConfigurationSection(entryKey);
                            if (entrySection != null) {
                                String presetName = entrySection.getString("preset");
                                double duration = entrySection.getDouble("duration", 5.0);
                                
                                if (presetName != null && presets.containsKey(presetName)) {
                                    sequence.addEntry(presetName, duration);
                                }
                            }
                        }
                    }
                    
                    sequences.put(sequenceName, sequence);
                }
            }
        }
        
        plugin.getPlugin().getLogger().info("已加载 " + presets.size() + " 个预设和 " + sequences.size() + " 个序列");
    }

    /**
     * 保存序列到配置文件
     */
    public void saveToConfig() {
        // 保存每个序列到单独的文件
        for (Map.Entry<String, CameraSequence> entry : sequences.entrySet()) {
            String sequenceName = entry.getKey();
            CameraSequence sequence = entry.getValue();
            saveSequenceToFile(sequenceName, sequence);
        }
    }

    /**
     * 保存序列到单独的YML文件
     * @param sequenceName 序列名称
     * @param sequence 序列对象
     */
    public void saveSequenceToFile(String sequenceName, CameraSequence sequence) {
        try {
            File sequencesDir = new File(plugin.getPlugin().getDataFolder(), "sequences");
            if (!sequencesDir.exists()) {
                sequencesDir.mkdirs();
            }
            
            File sequenceFile = new File(sequencesDir, sequenceName + ".yml");
            FileConfiguration sequenceConfig = YamlConfiguration.loadConfiguration(sequenceFile);
            
            // 清除旧数据
            sequenceConfig.set(sequenceName, null);
            
            // 保存序列数据 (使用扁平格式)
            sequenceConfig.set("loop", sequence.isLoop());
            
            // 保存序列条目 (使用列表格式)
            List<Map<String, Object>> entries = new ArrayList<>();
            for (int i = 0; i < sequence.getEntryCount(); i++) {
                CameraSequence.SequenceEntry sequenceEntry = sequence.getEntry(i);
                Map<String, Object> entryMap = new HashMap<>();
                entryMap.put("preset", sequenceEntry.getPresetName());
                entryMap.put("duration", sequenceEntry.getDuration());
                entries.add(entryMap);
            }
            sequenceConfig.set("entries", entries);
            
            // 保存到文件
            sequenceConfig.save(sequenceFile);
        } catch (IOException e) {
            plugin.getPlugin().getLogger().severe("无法保存序列文件 " + sequenceName + ": " + e.getMessage());
        }
    }

    /**
     * 进入相机模式
     * @param player 玩家
     */
    public void enterCameraMode(Player player) {
        CameraSession session = sessionManager.getSession(player);
        if (!session.isInCameraMode()) {
            // 保存玩家当前位置
            session.saveOriginalLocation(player.getLocation());
            
            // 设置相机模式标志
            session.setInCameraMode(true);
            
            // 创建相机实例
            Camera camera = new Camera(player.getLocation());
            session.setCamera(camera);
            
            // 应用相机效果（例如，隐藏HUD、切换到旁观模式等）
            applyCamera(player, camera);
        }
    }

    /**
     * 退出相机模式
     * @param player 玩家
     */
    public void exitCameraMode(Player player) {
        CameraSession session = sessionManager.getSession(player);
        if (session.isInCameraMode()) {
            stopSequence(player);
            
            if (plugin instanceof cn.popcraft.VirtualCameraPlugin) {
                cn.popcraft.VirtualCameraPlugin vcPlugin = (cn.popcraft.VirtualCameraPlugin) plugin;
                if (vcPlugin.getProtocolCameraController() != null) {
                    vcPlugin.getProtocolCameraController().stopCameraMode(player);
                }
            }
            
            Location originalLocation = session.getOriginalLocation();
            if (originalLocation != null) {
                player.teleport(originalLocation);
            }
            
            session.setInCameraMode(false);
            session.setCamera(null);
            
            restorePlayerState(player);
        }
    }

    /**
     * 应用相机效果
     * @param player 玩家
     * @param camera 相机实例
     */
    private void applyCamera(Player player, Camera camera) {
        // 根据相机类型应用不同效果
        switch (camera.getType()) {
            case NORMAL:
                // 普通相机模式，只是移动玩家
                player.teleport(camera.toLocation(player.getWorld()));
                break;
            case SPECTATOR:
                // 旁观者模式相机
                player.teleport(camera.toLocation(player.getWorld()));
                // 保存玩家原始游戏模式
                sessionManager.getSession(player).saveOriginalGameMode(player.getGameMode());
                // 切换到旁观模式
                player.setAllowFlight(true);
                player.setFlying(true);
                break;
            case CINEMATIC:
                // 电影模式相机（可以添加额外效果，如隐藏HUD）
                player.teleport(camera.toLocation(player.getWorld()));
                // 可以使用数据包或其他方法隐藏HUD
                break;
        }
    }

    /**
     * 恢复玩家状态
     * @param player 玩家
     */
    private void restorePlayerState(Player player) {
        CameraSession session = sessionManager.getSession(player);
        
        // 恢复游戏模式
        if (session.getOriginalGameMode() != null) {
            player.setGameMode(session.getOriginalGameMode());
        }
        
        // 恢复其他状态（如果有）
    }

    /**
     * 保存当前相机位置为预设
     * @param player 玩家
     * @param presetName 预设名称
     * @return 是否成功保存
     */
    public boolean savePreset(Player player, String presetName) {
        CameraSession session = sessionManager.getSession(player);
        if (!session.isInCameraMode() || session.getCamera() == null) {
            return false;
        }
        
        // 从当前相机创建预设
        Camera camera = session.getCamera();
        CameraPreset preset = new CameraPreset(presetName);
        Location location = new Location(
            player.getWorld(),
            camera.getX(),
            camera.getY(),
            camera.getZ(),
            camera.getYaw(),
            camera.getPitch()
        );
        preset.addLocation(location);
        preset.setType(camera.getType());
        
        // 保存预设到内存和文件
        presets.put(presetName, preset);
        savePresetToFile(presetName, preset);
        
        return true;
    }

    /**
     * 保存预设到单独的YML文件
     * @param presetName 预设名称
     * @param preset 预设对象
     */
    public void savePresetToFile(String presetName, CameraPreset preset) {
        try {
            File presetsDir = new File(plugin.getPlugin().getDataFolder(), "presets");
            if (!presetsDir.exists()) {
                presetsDir.mkdirs();
            }
            
            File presetFile = new File(presetsDir, presetName + ".yml");
            FileConfiguration presetConfig = YamlConfiguration.loadConfiguration(presetFile);
            
            // 清除旧数据
            presetConfig.set(presetName, null);
            
            // 保存预设数据 (使用扁平格式，符合README中的示例)
            presetConfig.set("type", preset.getType().name());
            
            // 保存位置点 (使用列表格式)
            List<Map<String, Object>> locations = new ArrayList<>();
            for (Location loc : preset.getLocations()) {
                Map<String, Object> locMap = new HashMap<>();
                locMap.put("x", loc.getX());
                locMap.put("y", loc.getY());
                locMap.put("z", loc.getZ());
                locMap.put("yaw", loc.getYaw());
                locMap.put("pitch", loc.getPitch());
                locations.add(locMap);
            }
            presetConfig.set("locations", locations);
            
            // 保存段落信息 (使用列表格式)
            List<Map<String, Object>> segments = new ArrayList<>();
            for (int i = 0; i < preset.getSegmentInfos().size(); i++) {
                CameraPreset.SegmentInfo segmentInfo = preset.getSegmentInfos().get(i);
                Map<String, Object> segmentMap = new HashMap<>();
                segmentMap.put("index", i);
                segmentMap.put("transition", segmentInfo.getTransitionType().name());
                segmentMap.put("duration", segmentInfo.getDuration());
                segments.add(segmentMap);
            }
            if (!segments.isEmpty()) {
                presetConfig.set("segments", segments);
            }
            
            // 保存命令 (使用列表格式)
            List<Map<String, Object>> commands = new ArrayList<>();
            for (CameraPreset.CommandAction cmd : preset.getCommands()) {
                Map<String, Object> cmdMap = new HashMap<>();
                cmdMap.put("command", cmd.getCommand());
                cmdMap.put("delay", cmd.getDelay());
                commands.add(cmdMap);
            }
            if (!commands.isEmpty()) {
                presetConfig.set("commands", commands);
            }
            
            // 保存文本 (使用列表格式)
            List<Map<String, Object>> texts = new ArrayList<>();
            for (CameraPreset.TextAction text : preset.getTexts()) {
                Map<String, Object> textMap = new HashMap<>();
                textMap.put("text", text.getText());
                textMap.put("delay", text.getDelay());
                textMap.put("duration", text.getDuration());
                texts.add(textMap);
            }
            if (!texts.isEmpty()) {
                presetConfig.set("texts", texts);
            }
            
            // 保存到文件
            presetConfig.save(presetFile);
        } catch (IOException e) {
            plugin.getPlugin().getLogger().severe("无法保存预设文件 " + presetName + ": " + e.getMessage());
        }
    }

    /**
     * 从文件加载预设
     * @param presetName 预设名称
     * @return 相机预设，如果不存在则返回null
     */
    public CameraPreset loadPresetFromFile(String presetName) {
        try {
            File presetFile = new File(plugin.getPlugin().getDataFolder(), "presets/" + presetName + ".yml");
            if (!presetFile.exists()) {
                return null;
            }
            
            FileConfiguration presetConfig = YamlConfiguration.loadConfiguration(presetFile);
            
            CameraPreset preset = new CameraPreset(presetName);
            
            // 加载相机类型
            String typeStr = presetConfig.getString("type", "NORMAL");
            try {
                CameraPreset.CameraType type = CameraPreset.CameraType.valueOf(typeStr);
                preset.setType(type);
            } catch (IllegalArgumentException e) {
                preset.setType(CameraPreset.CameraType.NORMAL);
            }
            
            // 加载位置点 (支持列表格式)
            List<Map<?, ?>> locationsList = presetConfig.getMapList("locations");
            if (locationsList != null) {
                for (Map<?, ?> locMap : locationsList) {
                    double x = ((Number) locMap.get("x")).doubleValue();
                    double y = ((Number) locMap.get("y")).doubleValue();
                    double z = ((Number) locMap.get("z")).doubleValue();
                    float yaw = ((Number) locMap.get("yaw")).floatValue();
                    float pitch = ((Number) locMap.get("pitch")).floatValue();
                    
                    Location loc = new Location(null, x, y, z, yaw, pitch);
                    preset.addLocation(loc);
                }
            }
            
            // 加载命令 (支持列表格式)
            List<Map<?, ?>> commandsList = presetConfig.getMapList("commands");
            if (commandsList != null) {
                for (Map<?, ?> cmdMap : commandsList) {
                    String command = (String) cmdMap.get("command");
                    long delay = ((Number) cmdMap.get("delay")).longValue();
                    
                    if (command != null) {
                        preset.addCommand(command, delay);
                    }
                }
            }
            
            // 加载文本 (支持列表格式)
            List<Map<?, ?>> textsList = presetConfig.getMapList("texts");
            if (textsList != null) {
                for (Map<?, ?> textMap : textsList) {
                    String text = (String) textMap.get("text");
                    long delay = ((Number) textMap.get("delay")).longValue();
                    long duration = ((Number) textMap.get("duration")).longValue();
                    
                    if (text != null) {
                        preset.addText(text, delay, duration);
                    }
                }
            }
            
            // 加载段落信息 (支持列表格式)
            List<Map<?, ?>> segmentsList = presetConfig.getMapList("segments");
            if (segmentsList != null) {
                for (Map<?, ?> segmentMap : segmentsList) {
                    int index = ((Number) segmentMap.get("index")).intValue();
                    String transitionStr = (String) segmentMap.get("transition");
                    long duration = ((Number) segmentMap.get("duration")).longValue();
                    
                    if (transitionStr != null) {
                        try {
                            TransitionType transitionType = TransitionType.valueOf(transitionStr);
                            preset.setSegmentInfo(index, transitionType, duration);
                        } catch (IllegalArgumentException e) {
                            preset.setSegmentInfo(index, TransitionType.SMOOTH, duration);
                        }
                    }
                }
            }
            
            return preset;
        } catch (Exception e) {
            plugin.getPlugin().getLogger().severe("加载预设文件失败 " + presetName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * 加载所有预设文件
     */
    public void loadAllPresets() {
        File presetsDir = new File(plugin.getPlugin().getDataFolder(), "presets");
        if (!presetsDir.exists()) {
            return;
        }
        
        File[] presetFiles = presetsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (presetFiles == null) {
            return;
        }
        
        for (File presetFile : presetFiles) {
            String presetName = presetFile.getName().replace(".yml", "");
            CameraPreset preset = loadPresetFromFile(presetName);
            if (preset != null) {
                presets.put(presetName, preset);
            }
        }
    }

    /**
     * 加载预设
     * @param player 玩家
     * @param presetName 预设名称
     * @return 是否成功加载
     */
    public boolean loadPreset(Player player, String presetName) {
        // 首先尝试从内存中获取预设
        CameraPreset preset = presets.get(presetName);
        
        // 如果内存中没有，则尝试从文件加载
        if (preset == null) {
            preset = loadPresetFromFile(presetName);
            if (preset != null) {
                // 将加载的预设添加到内存中
                presets.put(presetName, preset);
            }
        }
        
        if (preset == null) {
            return false;
        }
        
        CameraSession session = sessionManager.getSession(player);
        
        // 如果玩家不在相机模式，先进入相机模式
        if (!session.isInCameraMode()) {
            enterCameraMode(player);
        }
        
        // 构建时间轴
        Timeline timeline = new Timeline();
        
        // 添加路径点(使用玩家当前世界)
        for (Location loc : preset.getLocations()) {
            Location newLoc = loc.clone();
            newLoc.setWorld(player.getWorld());
            timeline.addKeyframe(newLoc);
        }
        
        // 设置每个段落的过渡类型和持续时间
        for (int i = 0; i < preset.getSegmentInfos().size(); i++) {
            CameraPreset.SegmentInfo segmentInfo = preset.getSegmentInfos().get(i);
            timeline.setSegmentTransition(i, segmentInfo.getTransitionType(), segmentInfo.getDuration());
        }
        
        // 添加文本动作
        for (CameraPreset.TextAction text : preset.getTexts()) {
            timeline.addTextAction(text.getDelay(), text);
        }
        
        // 添加命令动作
        for (CameraPreset.CommandAction cmd : preset.getCommands()) {
            timeline.addCommandAction(cmd.getDelay(), cmd);
        }
        
        // 设置时间轴并启动动画
        session.setTimeline(timeline);
        session.startAnimation();
        
        return true;
    }

    /**
     * 删除预设
     * @param presetName 预设名称
     * @return 是否成功删除
     */
    public boolean deletePreset(String presetName) {
        // 从内存中移除
        if (!presets.containsKey(presetName)) {
            return false;
        }
        
        // 从预设集合中移除
        presets.remove(presetName);
        
        // 从所有序列中移除包含此预设的条目
        for (CameraSequence sequence : sequences.values()) {
            sequence.removeEntriesByPresetName(presetName);
        }
        
        // 删除预设文件
        File presetFile = new File(plugin.getPlugin().getDataFolder(), "presets/" + presetName + ".yml");
        if (presetFile.exists()) {
            presetFile.delete();
        }
        
        // 保存到配置文件
        saveToConfig();
        
        return true;
    }

    /**
     * 获取所有预设
     * @return 预设映射
     */
    public Map<String, CameraPreset> getAllPresets() {
        return new HashMap<>(presets);
    }
    
    /**
     * 添加预设到内存映射
     * @param presetName 预设名称
     * @param preset 预设对象
     */
    public void addPreset(String presetName, CameraPreset preset) {
        presets.put(presetName, preset);
    }

    /**
     * 获取所有序列
     * @return 序列映射
     */
    public Map<String, CameraSequence> getAllSequences() {
        return new HashMap<>(sequences);
    }

    /**
     * 播放序列
     * @param player 玩家
     * @param sequenceName 序列名称
     * @return 是否成功播放
     */
    public boolean playSequence(Player player, String sequenceName) {
        CameraSequence sequence = sequences.get(sequenceName);
        if (sequence == null || sequence.getEntryCount() == 0) {
            return false;
        }
        
        // 停止当前正在播放的序列
        stopSequence(player);
        
        // 如果玩家不在相机模式，先进入相机模式
        CameraSession session = sessionManager.getSession(player);
        if (!session.isInCameraMode()) {
            enterCameraMode(player);
        }
        
        // 创建序列播放任务
        BukkitTask task = plugin.getPlugin().getServer().getScheduler().runTaskTimer((org.bukkit.plugin.Plugin)plugin, new Runnable() {
            private int currentIndex = 0;
            private long startTime = System.currentTimeMillis();
            private CameraPreset currentPreset = null;
            private long currentDuration = 0; // 当前预设的持续时间(毫秒)
            
            @Override
            public void run() {
                // 获取当前时间
                long currentTime = System.currentTimeMillis();
                
                // 如果是第一次运行或者需要切换到下一个预设
                if (currentPreset == null || 
                    (currentTime - startTime) >= currentDuration) {
                    
                    // 更新开始时间
                    startTime = currentTime;
                    
                    // 获取当前预设
                    String presetName = sequence.getEntry(currentIndex).getPresetName();
                    currentPreset = presets.get(presetName);
                    
                    // 设置当前预设的持续时间(转换为毫秒)
                    currentDuration = (long) (sequence.getEntry(currentIndex).getDuration() * 1000);
                    
                    // 应用当前预设
                    if (currentPreset != null) {
                        switchToPreset(player, presetName);
                    }
                    
                    // 移动到下一个预设
                    currentIndex++;
                    
                    // 如果到达序列末尾
                    if (currentIndex >= sequence.getEntryCount()) {
                        // 如果循环播放，重置索引
                        if (sequence.isLoop()) {
                            currentIndex = 0;
                        } else {
                            // 否则停止序列
                            stopSequence(player);
                        }
                    }
                }
            }
        }, 0L, 1L); // 每tick运行一次
        
        // 保存任务
        sequenceTasks.put(player.getUniqueId(), task);
        
        return true;
    }

    /**
     * 停止序列播放
     * @param player 玩家
     */
    public void stopSequence(Player player) {
        BukkitTask task = sequenceTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        // 停止当前动画
        CameraSession session = sessionManager.getSession(player);
        session.stopAnimation();
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        // 取消所有序列任务
        for (BukkitTask task : sequenceTasks.values()) {
            task.cancel();
        }
        sequenceTasks.clear();
        
        // 保存配置
        saveToConfig();
    }
    
    /**
     * 清理所有会话
     */
    public void cleanupAllSessions() {
        // 清理所有序列任务
        cleanup();
        
        // 可以添加其他会话清理逻辑
    }
    
    /**
     * 从文件加载序列
     * @param sequenceName 序列名称
     * @return 相机序列，如果不存在则返回null
     */
    public CameraSequence loadSequenceFromFile(String sequenceName) {
        try {
            File sequenceFile = new File(plugin.getPlugin().getDataFolder(), "sequences/" + sequenceName + ".yml");
            if (!sequenceFile.exists()) {
                return null;
            }
            
            FileConfiguration sequenceConfig = YamlConfiguration.loadConfiguration(sequenceFile);
            
            CameraSequence sequence = new CameraSequence(sequenceName);
            
            // 加载循环设置
            boolean loop = sequenceConfig.getBoolean("loop", false);
            sequence.setLoop(loop);
            
            // 加载序列条目 (支持列表格式)
            List<Map<?, ?>> entriesList = sequenceConfig.getMapList("entries");
            if (entriesList != null) {
                for (Map<?, ?> entryMap : entriesList) {
                    String presetName = (String) entryMap.get("preset");
                    double duration = ((Number) entryMap.get("duration")).doubleValue();
                    
                    if (presetName != null) {
                        sequence.addEntry(presetName, duration);
                    }
                }
            }
            
            return sequence;
        } catch (Exception e) {
            plugin.getPlugin().getLogger().severe("加载序列文件失败 " + sequenceName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * 从sequences目录加载所有序列
     */
    public void loadAllSequences() {
        File sequencesDir = new File(plugin.getPlugin().getDataFolder(), "sequences");
        if (!sequencesDir.exists()) {
            return;
        }
        
        File[] sequenceFiles = sequencesDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (sequenceFiles == null) {
            return;
        }
        
        for (File sequenceFile : sequenceFiles) {
            String sequenceName = sequenceFile.getName().replace(".yml", "");
            CameraSequence sequence = loadSequenceFromFile(sequenceName);
            if (sequence != null) {
                sequences.put(sequenceName, sequence);
            }
        }
    }
}