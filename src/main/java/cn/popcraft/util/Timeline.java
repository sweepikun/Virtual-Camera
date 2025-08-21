package cn.popcraft.util;

import cn.popcraft.model.CameraPreset;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * 时间轴类，用于管理相机路径、文本显示和命令执行的时间点
 */
public class Timeline {
    private final List<Location> keyframes;
    private final TreeMap<Long, CameraPreset.TextAction> textActions;
    private final TreeMap<Long, CameraPreset.CommandAction> commandActions;
    private long totalDuration; // 总持续时间(毫秒)
    
    public Timeline() {
        this.keyframes = new ArrayList<>();
        this.textActions = new TreeMap<>();
        this.commandActions = new TreeMap<>();
        this.totalDuration = 0;
    }
    
    /**
     * 添加关键帧
     * @param location 位置
     */
    public void addKeyframe(Location location) {
        keyframes.add(location);
    }
    
    /**
     * 添加文本动作
     * @param delay 延迟时间(毫秒)
     * @param textAction 文本动作
     */
    public void addTextAction(long delay, CameraPreset.TextAction textAction) {
        textActions.put(delay, textAction);
        // 更新总持续时间
        long end = delay + textAction.getDuration();
        if (end > totalDuration) {
            totalDuration = end;
        }
    }
    
    /**
     * 添加命令动作
     * @param delay 延迟时间(毫秒)
     * @param commandAction 命令动作
     */
    public void addCommandAction(long delay, CameraPreset.CommandAction commandAction) {
        commandActions.put(delay, commandAction);
        if (delay > totalDuration) {
            totalDuration = delay;
        }
    }
    
    /**
     * 获取指定时间点的位置
     * @param elapsed 已经过的时间(毫秒)
     * @return 对应的时间点位置
     */
    public Location getLocationAt(long elapsed) {
        if (keyframes.isEmpty()) {
            return null;
        }
        
        if (keyframes.size() == 1) {
            return keyframes.get(0);
        }
        
        // 计算当前进度
        float progress = (float) elapsed / totalDuration;
        if (progress >= 1.0f) {
            return keyframes.get(keyframes.size() - 1);
        }
        
        // 计算在关键帧中的位置
        float scaledProgress = progress * (keyframes.size() - 1);
        int index = (int) Math.floor(scaledProgress);
        float segmentProgress = scaledProgress - index;
        
        if (index >= keyframes.size() - 1) {
            return keyframes.get(keyframes.size() - 1);
        }
        
        // 使用PathInterpolator进行插值
        return PathInterpolator.easeInOutQuad(keyframes.get(index), keyframes.get(index + 1), segmentProgress);
    }
    
    /**
     * 获取在指定时间需要执行的文本动作
     * @param elapsed 已经过的时间(毫秒)
     * @param delta 检查的时间间隔(毫秒)
     * @return 需要执行的文本动作列表
     */
    public List<CameraPreset.TextAction> getTextActionsAt(long elapsed, long delta) {
        List<CameraPreset.TextAction> actions = new ArrayList<>();
        long start = elapsed;
        long end = elapsed + delta;
        
        for (Long time : textActions.keySet()) {
            if (time >= start && time < end) {
                actions.add(textActions.get(time));
            }
        }
        
        return actions;
    }
    
    /**
     * 获取在指定时间需要执行的命令动作
     * @param elapsed 已经过的时间(毫秒)
     * @param delta 检查的时间间隔(毫秒)
     * @return 需要执行的命令动作列表
     */
    public List<CameraPreset.CommandAction> getCommandActionsAt(long elapsed, long delta) {
        List<CameraPreset.CommandAction> actions = new ArrayList<>();
        long start = elapsed;
        long end = elapsed + delta;
        
        for (Long time : commandActions.keySet()) {
            if (time >= start && time < end) {
                actions.add(commandActions.get(time));
            }
        }
        
        return actions;
    }
    
    /**
     * 获取总持续时间
     * @return 总持续时间(毫秒)
     */
    public long getTotalDuration() {
        return totalDuration;
    }
    
    /**
     * 检查时间轴是否为空
     * @return 是否为空
     */
    public boolean isEmpty() {
        return keyframes.isEmpty() && textActions.isEmpty() && commandActions.isEmpty();
    }
}