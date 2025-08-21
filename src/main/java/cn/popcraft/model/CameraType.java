package cn.popcraft.model;

/**
 * 相机类型枚举
 */
public enum CameraType {
    /**
     * 固定相机 - 相机位置和角度固定不变
     */
    FIXED,
    
    /**
     * 跟随相机 - 相机跟随目标实体移动，通常在目标上方
     */
    FOLLOW,
    
    /**
     * 轨道相机 - 相机围绕目标实体旋转
     */
    ORBIT,
    
    /**
     * 第一人称相机 - 从目标实体的视角观看
     */
    FIRST_PERSON,
    
    /**
     * 第三人称相机 - 从目标实体后方观看
     */
    THIRD_PERSON,
    
    /**
     * 普通相机模式 - 默认相机模式
     */
    NORMAL,
    
    /**
     * 观察者模式 - 可以自由飞行观察
     */
    SPECTATOR,
    
    /**
     * 电影模式 - 用于录制电影的特殊模式
     */
    CINEMATIC;
}