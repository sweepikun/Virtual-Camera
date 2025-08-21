package cn.popcraft.manager;

import cn.popcraft.VirtualCamera;
import cn.popcraft.model.CameraPreset;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class RandomSwitchController {
    private final VirtualCamera plugin;
    private final Map<UUID, BukkitTask> activeTasks;
    private final Map<UUID, Set<String>> playerPresetPools;
    private final Map<UUID, Integer> switchIntervals;
    private final Random random;

    public RandomSwitchController(VirtualCamera plugin) {
        this.plugin = plugin;
        this.activeTasks = new HashMap<>();
        this.playerPresetPools = new HashMap<>();
        this.switchIntervals = new HashMap<>();
        this.random = new Random();
    }

    /**
     * 开始随机切换预设
     * @param player 玩家
     * @param presetNames 预设名称列表
     * @param intervalTicks 切换间隔（游戏刻）
     * @return 是否成功启动随机切换
     */
    public boolean startRandomSwitch(Player player, List<String> presetNames, int intervalTicks) {
        if (presetNames == null || presetNames.isEmpty()) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        stopRandomSwitch(player);

        // 验证所有预设是否存在
        List<String> validPresets = new ArrayList<>();
        for (String presetName : presetNames) {
            CameraPreset preset = plugin.getPresetManager().getPreset(presetName);
            if (preset != null) {
                validPresets.add(presetName);
            }
        }

        if (validPresets.isEmpty()) {
            return false;
        }

        playerPresetPools.put(playerId, new HashSet<>(validPresets));
        switchIntervals.put(playerId, intervalTicks);

        // 立即切换到随机预设
        String randomPreset = getRandomPreset(player);
        plugin.getCameraManager().switchToPreset(player, randomPreset);

        // 如果只有一个预设，不需要启动任务
        if (validPresets.size() > 1) {
            BukkitTask task = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline() || !playerPresetPools.containsKey(playerId)) {
                        stopRandomSwitch(player);
                        return;
                    }

                    String nextPreset = getRandomPreset(player);
                    plugin.getCameraManager().switchToPreset(player, nextPreset);
                }
            }.runTaskTimer(plugin.getPlugin(), intervalTicks, intervalTicks);

            activeTasks.put(playerId, task);
        }

        return true;
    }
    
    /**
     * 开始随机切换预设（使用玩家预设池）
     * @param player 玩家
     * @param intervalTicks 切换间隔（游戏刻）
     * @return 是否成功启动随机切换
     */
    public boolean startRandomSwitch(Player player, int intervalTicks) {
        Set<String> presetPool = playerPresetPools.get(player.getUniqueId());
        if (presetPool == null || presetPool.isEmpty()) {
            return false;
        }
        
        return startRandomSwitch(player, new ArrayList<>(presetPool), intervalTicks);
    }

    /**
     * 获取随机预设
     * @param player 玩家
     * @return 随机预设名称
     */
    private String getRandomPreset(Player player) {
        UUID playerId = player.getUniqueId();
        Set<String> presets = playerPresetPools.get(playerId);
        if (presets == null || presets.isEmpty()) {
            return null;
        }
        List<String> presetList = new ArrayList<>(presets);
        return presetList.get(random.nextInt(presetList.size()));
    }

    /**
     * 停止玩家的随机切换
     * @param player 玩家
     */
    public void stopRandomSwitch(Player player) {
        UUID playerId = player.getUniqueId();
        if (activeTasks.containsKey(playerId)) {
            activeTasks.get(playerId).cancel();
            activeTasks.remove(playerId);
        }
        playerPresetPools.remove(playerId);
        switchIntervals.remove(playerId);
    }

    /**
     * 检查玩家是否在随机切换中
     * @param player 玩家
     * @return 是否在随机切换中
     */
    public boolean isInRandomSwitch(Player player) {
        return playerPresetPools.containsKey(player.getUniqueId());
    }

    /**
     * 获取玩家当前的预设池
     * @param player 玩家
     * @return 预设名称列表，如果不在随机切换中则返回null
     */
    public List<String> getPlayerPresetPool(Player player) {
        Set<String> presetPool = playerPresetPools.get(player.getUniqueId());
        return presetPool != null ? new ArrayList<>(presetPool) : null;
    }
    
    /**
     * 获取玩家的预设池
     * @param player 玩家
     * @return 预设池
     */
    public Set<String> getPresetPool(Player player) {
        UUID uuid = player.getUniqueId();
        return playerPresetPools.computeIfAbsent(uuid, k -> new HashSet<>());
    }
    
    /**
     * 向预设池中添加预设
     * @param player 玩家
     * @param preset 预设名称
     */
    public void addPresetToPool(Player player, String preset) {
        getPresetPool(player).add(preset);
    }
    
    /**
     * 从预设池中移除预设
     * @param player 玩家
     * @param preset 预设名称
     */
    public void removePresetFromPool(Player player, String preset) {
        getPresetPool(player).remove(preset);
    }

    /**
     * 获取玩家当前的切换间隔
     * @param player 玩家
     * @return 切换间隔（游戏刻），如果不在随机切换中则返回-1
     */
    public int getSwitchInterval(Player player) {
        UUID playerId = player.getUniqueId();
        if (switchIntervals.containsKey(playerId)) {
            return switchIntervals.get(playerId);
        }
        return -1;
    }

    /**
     * 清理所有随机切换
     */
    public void cleanupAllRandomSwitches() {
        for (BukkitTask task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();
        playerPresetPools.clear();
        switchIntervals.clear();
    }
}