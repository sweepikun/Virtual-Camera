package cn.popcraft.model;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * 相机类，表示一个虚拟相机
 */
public class Camera {
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private CameraType type;

    /**
     * 从位置创建相机
     * @param location 位置
     */
    public Camera(Location location) {
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
        this.type = CameraType.NORMAL;
    }

    /**
     * 从预设创建相机
     * @param preset 预设
     */
    public Camera(CameraPreset preset) {
        this.x = preset.getX();
        this.y = preset.getY();
        this.z = preset.getZ();
        this.yaw = preset.getYaw();
        this.pitch = preset.getPitch();
        this.type = CameraType.NORMAL;
    }

    /**
     * 获取X坐标
     * @return X坐标
     */
    public double getX() {
        return x;
    }

    /**
     * 设置X坐标
     * @param x X坐标
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * 获取Y坐标
     * @return Y坐标
     */
    public double getY() {
        return y;
    }

    /**
     * 设置Y坐标
     * @param y Y坐标
     */
    public void setY(double y) {
        this.y = y;
    }

    /**
     * 获取Z坐标
     * @return Z坐标
     */
    public double getZ() {
        return z;
    }

    /**
     * 设置Z坐标
     * @param z Z坐标
     */
    public void setZ(double z) {
        this.z = z;
    }

    /**
     * 获取偏航角
     * @return 偏航角
     */
    public float getYaw() {
        return yaw;
    }

    /**
     * 设置偏航角
     * @param yaw 偏航角
     */
    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    /**
     * 获取俯仰角
     * @return 俯仰角
     */
    public float getPitch() {
        return pitch;
    }

    /**
     * 设置俯仰角
     * @param pitch 俯仰角
     */
    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    /**
     * 获取相机类型
     * @return 相机类型
     */
    public CameraType getType() {
        return type;
    }

    /**
     * 设置相机类型
     * @param type 相机类型
     */
    public void setType(CameraType type) {
        this.type = type;
    }

    /**
     * 转换为位置对象
     * @param world 世界
     * @return 位置对象
     */
    public Location toLocation(World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }

    /**
     * 从位置更新相机
     * @param location 位置
     */
    public void updateFromLocation(Location location) {
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
    }

    /**
     * 设置相机位置
     * @param location 位置
     */
    public void setLocation(Location location) {
        updateFromLocation(location);
    }

    /**
     * 更新相机位置和视角
     * @param location 位置
     * @param yaw 偏航角
     * @param pitch 俯仰角
     */
    public void updateCamera(Location location, float yaw, float pitch) {
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = yaw;
        this.pitch = pitch;
    }

    @Override
    public String toString() {
        return String.format("Camera{x=%s, y=%s, z=%s, yaw=%s, pitch=%s, type=%s}",
                x, y, z, yaw, pitch, type);
    }
}