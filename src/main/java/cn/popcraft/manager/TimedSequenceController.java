package cn.popcraft.manager;

import cn.popcraft.VirtualCamera;
import cn.popcraft.model.CameraPreset;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TimedSequenceController {
    private final VirtualCamera plugin;
    private final Map<UUID, BukkitTask> activeTasks;
    private final Map<UUID, List<String>> playerSequences;
    private final Map<UUID, Integer> currentIndexes;
    private final Map<UUID, Integer> delayTicks;

    public TimedSequenceController(VirtualCamera plugin) {
        this.plugin = plugin;
        this.activeTasks = new HashMap<>();
        this.playerSequences = new HashMap<>();
        this.currentIndexes = new HashMap<>();
        this.delayTicks = new HashMap<>();
    }

    /**
     * 开始一个预设序列
     * @param player 玩家
     * @param presetNames 预设名称列表
     * @param delayTicks 每个预设之间的延迟（游戏刻）
     * @return 是否成功启动序列
     */
    public boolean startSequence(Player player, List<String> presetNames, int delayTicks) {
        if (presetNames == null || presetNames.isEmpty()) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        stopSequence(player);

        // 验证所有预设是否存在
        for (String presetName : presetNames) {
            CameraPreset preset = plugin.getPresetManager().getPreset(presetName);
            if (preset == null) {
                return false;
            }
        }

        playerSequences.put(playerId, new ArrayList<>(presetNames));
        currentIndexes.put(playerId, 0);
        this.delayTicks.put(playerId, delayTicks);

        // 立即切换到第一个预设
        String firstPreset = presetNames.get(0);
        plugin.getCameraManager().switchToPreset(player, firstPreset);

        // 如果只有一个预设，不需要启动任务
        if (presetNames.size() > 1) {
            BukkitTask task = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline() || !playerSequences.containsKey(playerId)) {
                        stopSequence(player);
                        return;
                    }

                    int nextIndex = (currentIndexes.get(playerId) + 1) % playerSequences.get(playerId).size();
                    currentIndexes.put(playerId, nextIndex);
                    String nextPreset = playerSequences.get(playerId).get(nextIndex);
                    plugin.getCameraManager().switchToPreset(player, nextPreset);
                }
            }.runTaskTimer(plugin.getPlugin(), delayTicks, delayTicks);

            activeTasks.put(playerId, task);
        }

        return true;
    }

    /**
     * 停止玩家的预设序列
     * @param player 玩家
     */
    public void stopSequence(Player player) {
        UUID playerId = player.getUniqueId();
        if (activeTasks.containsKey(playerId)) {
            activeTasks.get(playerId).cancel();
            activeTasks.remove(playerId);
        }
        playerSequences.remove(playerId);
        currentIndexes.remove(playerId);
        delayTicks.remove(playerId);
    }

    /**
     * 检查玩家是否在序列中
     * @param player 玩家
     * @return 是否在序列中
     */
    public boolean isInSequence(Player player) {
        return playerSequences.containsKey(player.getUniqueId());
    }

    /**
     * 获取玩家当前的序列
     * @param player 玩家
     * @return 预设名称列表，如果不在序列中则返回null
     */
    public List<String> getPlayerSequence(Player player) {
        return playerSequences.get(player.getUniqueId());
    }

    /**
     * 获取玩家当前的序列索引
     * @param player 玩家
     * @return 当前索引，如果不在序列中则返回-1
     */
    public int getCurrentIndex(Player player) {
        UUID playerId = player.getUniqueId();
        if (currentIndexes.containsKey(playerId)) {
            return currentIndexes.get(playerId);
        }
        return -1;
    }

    /**
     * 获取玩家当前的延迟时间
     * @param player 玩家
     * @return 延迟时间（游戏刻），如果不在序列中则返回-1
     */
    public int getDelayTicks(Player player) {
        UUID playerId = player.getUniqueId();
        if (delayTicks.containsKey(playerId)) {
            return delayTicks.get(playerId);
        }
        return -1;
    }

    /**
     * 清理所有序列
     */
    public void cleanupAllSequences() {
        for (BukkitTask task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();
        playerSequences.clear();
        currentIndexes.clear();
        delayTicks.clear();
    }
}