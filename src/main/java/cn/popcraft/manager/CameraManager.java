package cn.popcraft.manager;

import cn.popcraft.VirtualCamera;
import cn.popcraft.model.Camera;
import cn.popcraft.model.CameraPreset;
import cn.popcraft.model.CameraSequence;
import cn.popcraft.session.CameraSession;
import cn.popcraft.session.SessionManager;
import cn.popcraft.util.Timeline; // 添加Timeline导入
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

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
     * 保存预设和序列到配置文件
     */
    public void saveToConfig() {
        FileConfiguration config = plugin.getPlugin().getConfig();
        
        // 清除旧数据
        config.set("presets", null);
        config.set("sequences", null);
        
        // 保存预设
        for (Map.Entry<String, CameraPreset> entry : presets.entrySet()) {
            String presetName = entry.getKey();
            CameraPreset preset = entry.getValue();
            
            String basePath = "presets." + presetName + ".";
            config.set(basePath + "type", preset.getType().name());
            
            // 保存位置点
            List<Location> locations = preset.getLocations();
            for (int i = 0; i < locations.size(); i++) {
                Location loc = locations.get(i);
                String locPath = basePath + "locations." + i + ".";
                config.set(locPath + "x", loc.getX());
                config.set(locPath + "y", loc.getY());
                config.set(locPath + "z", loc.getZ());
                config.set(locPath + "yaw", loc.getYaw());
                config.set(locPath + "pitch", loc.getPitch());
            }
            
            // 保存命令
            List<CameraPreset.CommandAction> commands = preset.getCommands();
            for (int i = 0; i < commands.size(); i++) {
                CameraPreset.CommandAction cmd = commands.get(i);
                String cmdPath = basePath + "commands." + i + ".";
                config.set(cmdPath + "command", cmd.getCommand());
                config.set(cmdPath + "delay", cmd.getDelay());
            }
            
            // 保存文本
            List<CameraPreset.TextAction> texts = preset.getTexts();
            for (int i = 0; i < texts.size(); i++) {
                CameraPreset.TextAction text = texts.get(i);
                String textPath = basePath + "texts." + i + ".";
                config.set(textPath + "text", text.getText());
                config.set(textPath + "delay", text.getDelay());
                config.set(textPath + "duration", text.getDuration());
            }
        }
        
        // 保存序列
        for (Map.Entry<String, CameraSequence> entry : sequences.entrySet()) {
            String sequenceName = entry.getKey();
            CameraSequence sequence = entry.getValue();
            
            String path = "sequences." + sequenceName + ".";
            config.set(path + "loop", sequence.isLoop());
            
            // 保存序列条目
            for (int i = 0; i < sequence.getEntryCount(); i++) {
                CameraSequence.SequenceEntry sequenceEntry = sequence.getEntry(i);
                String entryPath = path + "entries." + i + ".";
                
                config.set(entryPath + "preset", sequenceEntry.getPresetName());
                config.set(entryPath + "duration", sequenceEntry.getDuration());
            }
        }
        
        // 保存配置
        plugin.getPlugin().saveConfig();
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
            // 停止任何正在播放的序列
            stopSequence(player);
            
            // 恢复玩家原始位置
            Location originalLocation = session.getOriginalLocation();
            if (originalLocation != null) {
                player.teleport(originalLocation);
            }
            
            // 清除相机模式标志和相机实例
            session.setInCameraMode(false);
            session.setCamera(null);
            
            // 恢复玩家状态（例如，显示HUD、恢复游戏模式等）
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
        
        // 保存预设
        presets.put(presetName, preset);
        
        // 保存到配置文件
        saveToConfig();
        
        return true;
    }

    /**
     * 加载预设
     * @param player 玩家
     * @param presetName 预设名称
     * @return 是否成功加载
     */
    public boolean loadPreset(Player player, String presetName) {
        CameraPreset preset = presets.get(presetName);
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
        if (!presets.containsKey(presetName)) {
            return false;
        }
        
        // 从预设集合中移除
        presets.remove(presetName);
        
        // 从所有序列中移除包含此预设的条目
        for (CameraSequence sequence : sequences.values()) {
            sequence.removeEntriesByPresetName(presetName);
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
}