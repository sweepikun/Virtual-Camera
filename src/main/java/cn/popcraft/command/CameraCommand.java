package cn.popcraft.command;

import cn.popcraft.VirtualCameraPlugin;
import cn.popcraft.manager.CameraManager;
import cn.popcraft.manager.RandomSwitchController;
import cn.popcraft.model.CameraPreset;
import cn.popcraft.model.CameraSequence;
import cn.popcraft.model.TransitionType;
import cn.popcraft.session.CameraSession;
import cn.popcraft.session.SessionManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 相机命令执行器
 */
public class CameraCommand implements CommandExecutor {
    private final VirtualCameraPlugin plugin;
    private final SessionManager sessionManager;
    private final CameraManager cameraManager;
    private final RandomSwitchController randomController;
    
    // 存储正在创建的预设信息
    private final Map<UUID, PresetCreationData> presetCreationData = new HashMap<>();

    public CameraCommand(VirtualCameraPlugin plugin, SessionManager sessionManager, CameraManager cameraManager) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
        this.cameraManager = cameraManager;
        this.randomController = plugin.getRandomController();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家执行！");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "enter":
                handleEnter(player);
                break;

            case "exit":
                handleExit(player);
                break;

            case "save":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "请指定预设名称！");
                    return true;
                }
                handleSave(player, args[1]);
                break;

            case "load":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "请指定预设名称！");
                    return true;
                }
                handleLoad(player, args[1]);
                break;

            case "delete":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "请指定预设名称！");
                    return true;
                }
                handleDelete(player, args[1]);
                break;

            case "list":
                handleList(player);
                break;

            case "play":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "请指定序列名称！");
                    return true;
                }
                handlePlay(player, args[1]);
                break;

            case "stop":
                handleStop(player);
                break;

            case "help":
                sendHelp(player);
                break;
                
            case "create":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "请指定预设名称！");
                    return true;
                }
                handleCreatePreset(player, args[1]);
                break;
                
            case "addpoint":
                handleAddPoint(player, args);
                break;
                
            case "finish":
                handleFinishPreset(player);
                break;
                
            case "segment":
                handleSegmentConfig(player, args);
                break;

            case "random":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "请指定随机切换操作！");
                    return true;
                }
                handleRandom(player, args);
                break;

            default:
                player.sendMessage(ChatColor.RED + "未知命令！使用 /vcam help 查看帮助。");
                break;
        }

        return true;
    }

    /**
     * 处理创建预设命令
     */
    private void handleCreatePreset(Player player, String presetName) {
        if (!player.hasPermission("virtualcamera.preset.create")) {
            player.sendMessage(ChatColor.RED + "你没有权限创建预设！");
            return;
        }
        
        // 检查是否已存在同名预设
        if (plugin.getPresetManager().getPreset(presetName) != null) {
            player.sendMessage(ChatColor.RED + "预设 '" + presetName + "' 已存在！");
            return;
        }
        
        // 创建新的预设创建数据
        presetCreationData.put(player.getUniqueId(), new PresetCreationData(presetName));
        player.sendMessage(ChatColor.GREEN + "开始创建预设: " + presetName);
        player.sendMessage(ChatColor.YELLOW + "请使用以下命令添加路径点:");
        player.sendMessage(ChatColor.GRAY + "/vcam addpoint [运行时间(秒)] - 添加当前位置(包括角度)为路径点");
        player.sendMessage(ChatColor.GRAY + "/vcam finish - 完成预设创建");
        player.sendMessage(ChatColor.YELLOW + "提示: 第一个点不需要指定运行时间，每个点都会记录当前位置和角度");
    }
    
    /**
     * 处理添加路径点命令
     */
    private void handleAddPoint(Player player, String[] args) {
        if (!player.hasPermission("virtualcamera.preset.create")) {
            player.sendMessage(ChatColor.RED + "你没有权限创建预设！");
            return;
        }
        
        UUID playerId = player.getUniqueId();
        if (!presetCreationData.containsKey(playerId)) {
            player.sendMessage(ChatColor.RED + "你没有正在进行的预设创建任务！请先使用 /vcam create <名称> 开始创建。");
            return;
        }
        
        PresetCreationData data = presetCreationData.get(playerId);
        Location location = player.getLocation(); // 这已经包含了玩家的相机角度 (yaw 和 pitch)
        
        // 如果是第一个点，不需要运行时间
        if (data.getPoints().isEmpty()) {
            data.addPoint(location, 0); // 第一个点运行时间为0
            player.sendMessage(ChatColor.GREEN + "已添加起点: " + formatLocation(location));
        } else {
            // 需要指定运行时间
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "请指定运行时间(秒)！");
                return;
            }
            
            try {
                double duration = Double.parseDouble(args[1]);
                if (duration < 0) {
                    player.sendMessage(ChatColor.RED + "运行时间不能为负数！");
                    return;
                }
                
                data.addPoint(location, duration);
                player.sendMessage(ChatColor.GREEN + "已添加路径点: " + formatLocation(location) + " (运行时间: " + duration + "秒)");
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "无效的运行时间！请输入一个数字。");
                return;
            }
        }
    }
    
    /**
     * 处理完成预设创建命令
     */
    private void handleFinishPreset(Player player) {
        if (!player.hasPermission("virtualcamera.preset.create")) {
            player.sendMessage(ChatColor.RED + "你没有权限创建预设！");
            return;
        }
        
        UUID playerId = player.getUniqueId();
        if (!presetCreationData.containsKey(playerId)) {
            player.sendMessage(ChatColor.RED + "你没有正在进行的预设创建任务！请先使用 /vcam create <名称> 开始创建。");
            return;
        }
        
        PresetCreationData data = presetCreationData.get(playerId);
        List<Location> points = data.getPoints();
        
        if (points.size() < 1) {
            player.sendMessage(ChatColor.RED + "预设至少需要一个点！");
            return;
        }
        
        // 创建预设
        String presetName = data.getPresetName();
        CameraPreset preset = new CameraPreset(presetName);
        preset.setType(CameraPreset.CameraType.NORMAL); // 默认类型
        
        // 添加所有点
        for (Location point : points) {
            preset.addLocation(point);
        }
        
        // 设置段落信息
        List<Double> durations = data.getDurations();
        for (int i = 0; i < durations.size() && i < preset.getLocationCount() - 1; i++) {
            long durationMs = (long) (durations.get(i) * 1000); // 转换为毫秒
            preset.setSegmentInfo(i, TransitionType.SMOOTH, durationMs);
        }
        
        // 保存预设到内存和文件
        plugin.getPresetManager().addPreset(presetName, preset);
        cameraManager.savePresetToFile(presetName, preset);
        
        // 清理临时数据
        presetCreationData.remove(playerId);
        
        player.sendMessage(ChatColor.GREEN + "预设 '" + presetName + "' 创建成功！共 " + points.size() + " 个路径点。");
    }
    
    /**
     * 处理段落配置命令
     * /vcam segment <段落索引> <过渡类型> <持续时间(秒)> <预设名称>
     */
    private void handleSegmentConfig(Player player, String[] args) {
        if (!player.hasPermission("virtualcamera.preset.edit")) {
            player.sendMessage(ChatColor.RED + "你没有权限编辑预设！");
            return;
        }
        
        if (args.length < 5) {
            player.sendMessage(ChatColor.RED + "用法: /vcam segment <段落索引> <过渡类型> <持续时间(秒)> <预设名称>");
            player.sendMessage(ChatColor.GRAY + "过渡类型: linear, ease_in_out, ease_in, ease_out, bounce, elastic, smooth");
            return;
        }
        
        try {
            int segmentIndex = Integer.parseInt(args[1]);
            
            // 解析过渡类型
            TransitionType transitionType;
            try {
                transitionType = TransitionType.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "无效的过渡类型！可用类型: linear, ease_in_out, ease_in, ease_out, bounce, elastic, smooth");
                return;
            }
            
            // 解析持续时间
            double durationSeconds = Double.parseDouble(args[3]);
            if (durationSeconds <= 0) {
                player.sendMessage(ChatColor.RED + "持续时间必须大于0！");
                return;
            }
            
            // 获取预设名称
            String presetName = args[4];
            CameraPreset preset = plugin.getPresetManager().getPreset(presetName);
            if (preset == null) {
                player.sendMessage(ChatColor.RED + "预设 '" + presetName + "' 不存在！");
                return;
            }
            
            if (segmentIndex < 0 || segmentIndex >= preset.getLocationCount() - 1) {
                player.sendMessage(ChatColor.RED + "段落索引超出范围！有效范围: 0-" + (preset.getLocationCount() - 2));
                return;
            }
            
            long durationMs = (long) (durationSeconds * 1000);
            
            // 设置段落信息
            preset.setSegmentInfo(segmentIndex, transitionType, durationMs);
            
            // 保存到配置文件
            cameraManager.savePresetToFile(presetName, preset);
            
            player.sendMessage(ChatColor.GREEN + "已更新预设 '" + presetName + "' 的段落 " + segmentIndex + 
                              "，过渡类型: " + transitionType + "，持续时间: " + durationSeconds + "秒");
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "无效的数字参数！请检查段落索引和持续时间。");
        }
    }

    /**
     * 处理进入相机模式命令
     */
    private void handleEnter(Player player) {
        if (!player.hasPermission("virtualcamera.use")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
            return;
        }

        CameraSession session = sessionManager.getSession(player);
        if (session.isInCameraMode()) {
            player.sendMessage(ChatColor.RED + "你已经在相机模式中！");
            return;
        }

        cameraManager.enterCameraMode(player);
        player.sendMessage(ChatColor.GREEN + "已进入相机模式。使用 /vcam exit 退出。");
    }

    /**
     * 处理退出相机模式命令
     */
    private void handleExit(Player player) {
        CameraSession session = sessionManager.getSession(player);
        if (!session.isInCameraMode()) {
            player.sendMessage(ChatColor.RED + "你不在相机模式中！");
            return;
        }

        cameraManager.exitCameraMode(player);
        player.sendMessage(ChatColor.GREEN + "已退出相机模式。");
    }

    /**
     * 处理保存预设命令
     */
    private void handleSave(Player player, String presetName) {
        if (!player.hasPermission("virtualcamera.preset.save")) {
            player.sendMessage(ChatColor.RED + "你没有权限保存预设！");
            return;
        }

        CameraSession session = sessionManager.getSession(player);
        if (!session.isInCameraMode()) {
            player.sendMessage(ChatColor.RED + "你必须在相机模式中才能保存预设！");
            return;
        }

        if (cameraManager.savePreset(player, presetName)) {
            player.sendMessage(ChatColor.GREEN + "已保存预设：" + presetName);
            cameraManager.saveToConfig(); // 确保保存到文件
        } else {
            player.sendMessage(ChatColor.RED + "保存预设失败！");
        }
    }

    /**
     * 处理加载预设命令
     */
    private void handleLoad(Player player, String presetName) {
        if (!player.hasPermission("virtualcamera.preset.load")) {
            player.sendMessage(ChatColor.RED + "你没有权限加载预设！");
            return;
        }

        if (cameraManager.loadPreset(player, presetName)) {
            player.sendMessage(ChatColor.GREEN + "已加载预设：" + presetName);
        } else {
            player.sendMessage(ChatColor.RED + "找不到预设：" + presetName);
        }
    }

    /**
     * 处理删除预设命令
     */
    private void handleDelete(Player player, String presetName) {
        if (!player.hasPermission("virtualcamera.preset.delete")) {
            player.sendMessage(ChatColor.RED + "你没有权限删除预设！");
            return;
        }

        if (cameraManager.deletePreset(presetName)) {
            player.sendMessage(ChatColor.GREEN + "已删除预设：" + presetName);
        } else {
            player.sendMessage(ChatColor.RED + "找不到预设：" + presetName);
        }
    }

    /**
     * 处理列出预设命令
     */
    private void handleList(Player player) {
        if (!player.hasPermission("virtualcamera.preset.list")) {
            player.sendMessage(ChatColor.RED + "你没有权限查看预设列表！");
            return;
        }

        Map<String, CameraPreset> presets = cameraManager.getAllPresets();
        Map<String, CameraSequence> sequences = cameraManager.getAllSequences();

        if (presets.isEmpty() && sequences.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "没有保存的预设或序列。");
            return;
        }

        if (!presets.isEmpty()) {
            player.sendMessage(ChatColor.GREEN + "预设列表：");
            for (String presetName : presets.keySet()) {
                player.sendMessage(ChatColor.GRAY + "- " + presetName);
            }
        }

        if (!sequences.isEmpty()) {
            player.sendMessage(ChatColor.GREEN + "序列列表：");
            for (String sequenceName : sequences.keySet()) {
                CameraSequence sequence = sequences.get(sequenceName);
                player.sendMessage(ChatColor.GRAY + "- " + sequenceName + 
                    " (" + sequence.getEntryCount() + "个预设, " + 
                    (sequence.isLoop() ? "循环" : "不循环") + ")");
            }
        }
    }

    /**
     * 处理播放序列命令
     */
    private void handlePlay(Player player, String sequenceName) {
        if (!player.hasPermission("virtualcamera.sequence.play")) {
            player.sendMessage(ChatColor.RED + "你没有权限播放序列！");
            return;
        }

        if (cameraManager.playSequence(player, sequenceName)) {
            player.sendMessage(ChatColor.GREEN + "正在播放序列：" + sequenceName);
        } else {
            player.sendMessage(ChatColor.RED + "找不到序列：" + sequenceName);
        }
    }

    /**
     * 处理停止播放命令
     */
    private void handleStop(Player player) {
        if (!player.hasPermission("virtualcamera.sequence.stop")) {
            player.sendMessage(ChatColor.RED + "你没有权限停止序列播放！");
            return;
        }

        cameraManager.stopSequence(player);
        player.sendMessage(ChatColor.GREEN + "已停止序列播放。");
    }

    /**
     * 发送帮助信息
     */
    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GREEN + "=== VirtualCamera 帮助 ===");
        player.sendMessage(ChatColor.GRAY + "/vcam enter" + ChatColor.WHITE + " - 进入相机模式");
        player.sendMessage(ChatColor.GRAY + "/vcam exit" + ChatColor.WHITE + " - 退出相机模式");
        player.sendMessage(ChatColor.GRAY + "/vcam save <名称>" + ChatColor.WHITE + " - 保存当前相机位置为预设");
        player.sendMessage(ChatColor.GRAY + "/vcam load <名称>" + ChatColor.WHITE + " - 加载预设");
        player.sendMessage(ChatColor.GRAY + "/vcam delete <名称>" + ChatColor.WHITE + " - 删除预设");
        player.sendMessage(ChatColor.GRAY + "/vcam list" + ChatColor.WHITE + " - 列出所有预设和序列");
        player.sendMessage(ChatColor.GRAY + "/vcam play <序列>" + ChatColor.WHITE + " - 播放相机序列");
        player.sendMessage(ChatColor.GRAY + "/vcam stop" + ChatColor.WHITE + " - 停止序列播放");
        player.sendMessage(ChatColor.GRAY + "/vcam create <名称>" + ChatColor.WHITE + " - 创建多点相机预设");
        player.sendMessage(ChatColor.GRAY + "/vcam addpoint [时间]" + ChatColor.WHITE + " - 添加当前点为路径点");
        player.sendMessage(ChatColor.GRAY + "/vcam finish" + ChatColor.WHITE + " - 完成预设创建");
        player.sendMessage(ChatColor.GRAY + "/vcam segment <索引> <类型> <时间> <预设>" + ChatColor.WHITE + " - 设置段落过渡效果");
        player.sendMessage(ChatColor.GRAY + "/vcam random start <时间>" + ChatColor.WHITE + " - 开始随机切换预设");
        player.sendMessage(ChatColor.GRAY + "/vcam random stop" + ChatColor.WHITE + " - 停止随机切换预设");
        player.sendMessage(ChatColor.GRAY + "/vcam random add <名称>" + ChatColor.WHITE + " - 添加预设到随机切换池");
        player.sendMessage(ChatColor.GRAY + "/vcam random remove <名称>" + ChatColor.WHITE + " - 从随机切换池中移除预设");
        player.sendMessage(ChatColor.GRAY + "/vcam random list" + ChatColor.WHITE + " - 列出随机切换池中的预设");
        player.sendMessage(ChatColor.GRAY + "/vcam help" + ChatColor.WHITE + " - 显示此帮助信息");
    }
    
    /**
     * 处理随机切换相关命令
     */
    private void handleRandom(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "请指定随机切换操作！");
            return;
        }
        
        String operation = args[1].toLowerCase();
        
        switch (operation) {
            case "start":
                if (!player.hasPermission("virtualcamera.random.start")) {
                    player.sendMessage(ChatColor.RED + "你没有权限开始随机切换！");
                    return;
                }
                
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "请指定切换间隔！");
                    return;
                }
                
                try {
                    int interval = Integer.parseInt(args[2]);
                    if (interval <= 0) {
                        player.sendMessage(ChatColor.RED + "切换间隔必须大于0！");
                        return;
                    }
                    
                    if (randomController.getPresetPool(player).isEmpty()) {
                        player.sendMessage(ChatColor.RED + "随机切换池为空！请先添加预设。");
                        return;
                    }
                    
                    randomController.startRandomSwitch(player, interval * 20); // 转换为游戏刻
                    player.sendMessage(ChatColor.GREEN + "已开始随机切换预设，间隔：" + interval + "秒。");
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "无效的时间间隔！请输入一个整数。");
                }
                break;
                
            case "stop":
                if (!player.hasPermission("virtualcamera.random.stop")) {
                    player.sendMessage(ChatColor.RED + "你没有权限停止随机切换！");
                    return;
                }
                
                if (!randomController.isInRandomSwitch(player)) {
                    player.sendMessage(ChatColor.RED + "你没有正在进行的随机切换！");
                    return;
                }
                
                randomController.stopRandomSwitch(player);
                player.sendMessage(ChatColor.GREEN + "已停止随机切换预设。");
                break;
                
            case "add":
                if (!player.hasPermission("virtualcamera.random.add")) {
                    player.sendMessage(ChatColor.RED + "你没有权限添加预设到随机切换池！");
                    return;
                }
                
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "请指定要添加的预设名称！");
                    return;
                }
                
                String presetToAdd = args[2];
                // 检查预设是否存在
                if (plugin.getPresetManager().getPreset(presetToAdd) == null) {
                    player.sendMessage(ChatColor.RED + "预设 '" + presetToAdd + "' 不存在！");
                    return;
                }
                
                if (randomController.getPresetPool(player).contains(presetToAdd)) {
                    player.sendMessage(ChatColor.RED + "预设 '" + presetToAdd + "' 已在随机切换池中！");
                    return;
                }
                
                randomController.addPresetToPool(player, presetToAdd);
                player.sendMessage(ChatColor.GREEN + "已将预设 '" + presetToAdd + "' 添加到随机切换池。");
                break;
                
            case "remove":
                if (!player.hasPermission("virtualcamera.random.remove")) {
                    player.sendMessage(ChatColor.RED + "你没有权限从随机切换池中移除预设！");
                    return;
                }
                
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "请指定要移除的预设名称！");
                    return;
                }
                
                String presetToRemove = args[2];
                if (!randomController.getPresetPool(player).contains(presetToRemove)) {
                    player.sendMessage(ChatColor.RED + "预设 '" + presetToRemove + "' 不在随机切换池中！");
                    return;
                }
                
                randomController.removePresetFromPool(player, presetToRemove);
                player.sendMessage(ChatColor.GREEN + "已从随机切换池中移除预设 '" + presetToRemove + "'。");
                break;
                
            case "list":
                if (!player.hasPermission("virtualcamera.random.list")) {
                    player.sendMessage(ChatColor.RED + "你没有权限查看随机切换池！");
                    return;
                }
                
                List<String> presetPool = randomController.getPlayerPresetPool(player);
                if (presetPool == null || presetPool.isEmpty()) {
                    player.sendMessage(ChatColor.YELLOW + "随机切换池为空。");
                    return;
                }
                
                player.sendMessage(ChatColor.GREEN + "随机切换池中的预设：");
                for (String presetName : presetPool) {
                    player.sendMessage(ChatColor.GRAY + "- " + presetName);
                }
                
                // 显示切换间隔
                int intervalTicks = randomController.getSwitchInterval(player);
                if (intervalTicks > 0) {
                    double intervalSeconds = intervalTicks / 20.0;
                    player.sendMessage(ChatColor.GRAY + "切换间隔: " + intervalSeconds + " 秒");
                }
                break;
                
            default:
                player.sendMessage(ChatColor.RED + "未知的随机切换操作！使用 /vcam help 查看帮助。");
                break;
        }
    }
    
    /**
     * 格式化位置信息
     */
    private String formatLocation(Location location) {
        return String.format("x:%.2f, y:%.2f, z:%.2f, yaw:%.2f, pitch:%.2f", 
                location.getX(), location.getY(), location.getZ(), 
                location.getYaw(), location.getPitch());
    }
    
    /**
     * 预设创建数据类
     */
    private static class PresetCreationData {
        private final String presetName;
        private final List<Location> points;
        private final List<Double> durations; // 每个点到下一个点的运行时间
        
        public PresetCreationData(String presetName) {
            this.presetName = presetName;
            this.points = new ArrayList<>();
            this.durations = new ArrayList<>();
        }
        
        public void addPoint(Location location, double duration) {
            points.add(location.clone());
            durations.add(duration);
        }
        
        public String getPresetName() {
            return presetName;
        }
        
        public List<Location> getPoints() {
            return new ArrayList<>(points);
        }
        
        public List<Double> getDurations() {
            return new ArrayList<>(durations);
        }
    }
}