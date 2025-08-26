package cn.popcraft.util;

import cn.popcraft.model.CameraPreset;
import cn.popcraft.model.TransitionType;
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
    private final List<TransitionType> transitionTypes; // 每个段落的过渡类型
    private final List<Long> segmentDurations; // 每个段落的持续时间
    private long totalDuration; // 总持续时间(毫秒)
    
    public Timeline() {
        this.keyframes = new ArrayList<>();
        this.textActions = new TreeMap<>();
        this.commandActions = new TreeMap<>();
        this.transitionTypes = new ArrayList<>();
        this.segmentDurations = new ArrayList<>();
        this.totalDuration = 0;
    }
    
    /**
     * 添加关键帧
     * @param location 位置
     */
    public void addKeyframe(Location location) {
        keyframes.add(location);
        // 如果不是第一个点，添加默认过渡类型
        if (keyframes.size() > 1) {
            transitionTypes.add(TransitionType.SMOOTH);
            segmentDurations.add(3000L); // 默认3秒
        }
    }
    
    /**
     * 设置段落的过渡类型和持续时间
     * @param segmentIndex 段落索引
     * @param transitionType 过渡类型
     * @param duration 持续时间(毫秒)
     */
    public void setSegmentTransition(int segmentIndex, TransitionType transitionType, long duration) {
        if (segmentIndex >= 0 && segmentIndex < transitionTypes.size()) {
            transitionTypes.set(segmentIndex, transitionType);
            segmentDurations.set(segmentIndex, duration);
            
            // 重新计算总持续时间
            recalculateTotalDuration();
        }
    }
    
    /**
     * 重新计算总持续时间
     */
    private void recalculateTotalDuration() {
        totalDuration = 0;
        for (Long duration : segmentDurations) {
            totalDuration += duration;
        }
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
        
        // 如果时间超过了总持续时间，返回最后一个关键帧
        if (elapsed >= totalDuration) {
            return keyframes.get(keyframes.size() - 1);
        }
        
        // 找到当前所在的段落
        long accumulatedTime = 0;
        for (int i = 0; i < segmentDurations.size(); i++) {
            long segmentDuration = segmentDurations.get(i);
            if (elapsed <= accumulatedTime + segmentDuration || i == segmentDurations.size() - 1) {
                // 计算在当前段落中的进度
                long timeInSegment = elapsed - accumulatedTime;
                float progress = (float) timeInSegment / segmentDuration;
                
                // 获取过渡类型
                TransitionType transitionType = transitionTypes.get(i);
                
                // 使用对应的插值函数
                return PathInterpolator.interpolate(
                    keyframes.get(i), 
                    keyframes.get(i + 1), 
                    progress, 
                    transitionType
                );
            }
            accumulatedTime += segmentDuration;
        }
        
        // 默认返回最后一个关键帧
        return keyframes.get(keyframes.size() - 1);
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
    
    /**
     * 获取关键帧数量
     * @return 关键帧数量
     */
    public int getKeyframeCount() {
        return keyframes.size();
    }
}