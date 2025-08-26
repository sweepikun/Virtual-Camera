package cn.popcraft.util;

import cn.popcraft.model.TransitionType;
import org.bukkit.Location;

/**
 * 路径插值器，用于在关键帧之间进行平滑插值计算
 */
public class PathInterpolator {
    
    /**
     * 线性插值函数
     * @param start 起始位置
     * @param end 结束位置
     * @param progress 进度 (0.0 到 1.0)
     * @return 插值后的位置
     */
    public static Location lerp(Location start, Location end, float progress) {
        if (progress <= 0) return start.clone();
        if (progress >= 1) return end.clone();
        
        double x = start.getX() + (end.getX() - start.getX()) * progress;
        double y = start.getY() + (end.getY() - start.getY()) * progress;
        double z = start.getZ() + (end.getZ() - start.getZ()) * progress;
        
        // 处理角度插值
        float yaw = interpolateAngle(start.getYaw(), end.getYaw(), progress);
        float pitch = interpolateAngle(start.getPitch(), end.getPitch(), progress);
        
        return new Location(start.getWorld(), x, y, z, yaw, pitch);
    }
    
    /**
     * 根据过渡类型获取插值后的位置
     * @param start 起始位置
     * @param end 结束位置
     * @param progress 进度 (0.0 到 1.0)
     * @param transitionType 过渡类型
     * @return 插值后的位置
     */
    public static Location interpolate(Location start, Location end, float progress, TransitionType transitionType) {
        // 使用TransitionType中的计算方法
        double adjustedProgress = transitionType.calculateProgress(progress);
        return lerp(start, end, (float) adjustedProgress);
    }
    
    /**
     * 角度插值函数，处理角度环绕问题
     * @param startAngle 起始角度
     * @param endAngle 结束角度
     * @param progress 进度
     * @return 插值后的角度
     */
    private static float interpolateAngle(float startAngle, float endAngle, float progress) {
        float difference = endAngle - startAngle;
        
        // 处理角度环绕问题
        while (difference < -180) difference += 360;
        while (difference > 180) difference -= 360;
        
        return startAngle + difference * progress;
    }
}