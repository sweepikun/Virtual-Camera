package cn.popcraft.command;

import cn.popcraft.VirtualCameraPlugin;
import cn.popcraft.manager.CameraManager;
import cn.popcraft.manager.RandomSwitchController;
import cn.popcraft.model.CameraPreset;
import cn.popcraft.model.CameraSequence;
import cn.popcraft.session.CameraSession;
import cn.popcraft.session.SessionManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * 相机命令执行器
 */
public class CameraCommand implements CommandExecutor {
    private final VirtualCameraPlugin plugin;
    private final SessionManager sessionManager;
    private final CameraManager cameraManager;
    private final RandomSwitchController randomController;

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
                
            case "random":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "请指定随机切换操作！");
                    return true;
                }
                handleRandom(player, args);
                break;

            default:
                player.sendMessage(ChatColor.RED + "未知命令！使用 /camera help 查看帮助。");
                break;
        }

        return true;
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
        player.sendMessage(ChatColor.GREEN + "已进入相机模式。使用 /camera exit 退出。");
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
        player.sendMessage(ChatColor.GRAY + "/camera enter" + ChatColor.WHITE + " - 进入相机模式");
        player.sendMessage(ChatColor.GRAY + "/camera exit" + ChatColor.WHITE + " - 退出相机模式");
        player.sendMessage(ChatColor.GRAY + "/camera save <名称>" + ChatColor.WHITE + " - 保存当前相机位置为预设");
        player.sendMessage(ChatColor.GRAY + "/camera load <名称>" + ChatColor.WHITE + " - 加载预设");
        player.sendMessage(ChatColor.GRAY + "/camera delete <名称>" + ChatColor.WHITE + " - 删除预设");
        player.sendMessage(ChatColor.GRAY + "/camera list" + ChatColor.WHITE + " - 列出所有预设和序列");
        player.sendMessage(ChatColor.GRAY + "/camera play <序列>" + ChatColor.WHITE + " - 播放相机序列");
        player.sendMessage(ChatColor.GRAY + "/camera stop" + ChatColor.WHITE + " - 停止序列播放");
        player.sendMessage(ChatColor.GRAY + "/camera random start <时间>" + ChatColor.WHITE + " - 开始随机切换预设");
        player.sendMessage(ChatColor.GRAY + "/camera random stop" + ChatColor.WHITE + " - 停止随机切换预设");
        player.sendMessage(ChatColor.GRAY + "/camera random add <名称>" + ChatColor.WHITE + " - 添加预设到随机切换池");
        player.sendMessage(ChatColor.GRAY + "/camera random remove <名称>" + ChatColor.WHITE + " - 从随机切换池中移除预设");
        player.sendMessage(ChatColor.GRAY + "/camera random list" + ChatColor.WHITE + " - 列出随机切换池中的预设");
        player.sendMessage(ChatColor.GRAY + "/camera help" + ChatColor.WHITE + " - 显示此帮助信息");
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
                player.sendMessage(ChatColor.RED + "未知的随机切换操作！使用 /camera help 查看帮助。");
                break;
        }
    }
}