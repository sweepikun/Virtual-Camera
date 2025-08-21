package cn.popcraft.session;

import cn.popcraft.model.Camera;
import cn.popcraft.model.CameraPreset;
import cn.popcraft.util.Timeline;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

/**
 * 相机会话类，管理玩家的相机状态
 */
public class CameraSession {
    private final Player player;
    private Camera activeCamera;
    private Location originalLocation;
    private org.bukkit.GameMode originalGameMode;
    private boolean inCameraMode;
    private boolean ignoreNextMove;
    
    // 新增字段
    private Timeline timeline;
    private List<BukkitTask> scheduledTasks = new ArrayList<>();
    private BukkitTask animationTask;
    private long startTime;
    private boolean isPlaying;
    
    // 为兼容性保留的字段
    private List<Location> pathPoints = new ArrayList<>();
    private long duration;
    private int currentPointIndex;

    /**
     * 创建一个新的相机会话
     * @param player 玩家
     */
    public CameraSession(Player player) {
        this.player = player;
        this.activeCamera = new Camera(player.getLocation());
        this.originalLocation = null;
        this.inCameraMode = false;
        this.ignoreNextMove = false;
        this.timeline = new Timeline();
        this.isPlaying = false;
    }

    /**
     * 获取会话所属玩家
     * @return 玩家
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * 获取当前活动的相机
     * @return 活动相机
     */
    public Camera getActiveCamera() {
        return activeCamera;
    }

    /**
     * 获取当前相机(与getActiveCamera()相同)
     * @return 当前相机
     */
    public Camera getCamera() {
        return activeCamera;
    }

    /**
     * 设置当前活动的相机
     * @param camera 要设置的相机
     */
    public void setCamera(Camera camera) {
        this.activeCamera = camera;
    }

    /**
     * 保存玩家的原始位置
     * @param location 原始位置
     */
    public void saveOriginalLocation(Location location) {
        setOriginalLocation(location);
    }

    /**
     * 获取玩家的原始位置
     * @return 原始位置
     */
    public Location getOriginalLocation() {
        return originalLocation;
    }

    /**
     * 设置玩家的原始位置
     * @param originalLocation 原始位置
     */
    
    /**
     * 保存玩家的原始游戏模式
     * @param gameMode 原始游戏模式
     */
    public void saveOriginalGameMode(org.bukkit.GameMode gameMode) {
        this.originalGameMode = gameMode;
    }

    /**
     * 获取保存的原始游戏模式
     * @return 原始游戏模式
     */
    public org.bukkit.GameMode getOriginalGameMode() {
        return originalGameMode;
    }
    public void setOriginalLocation(Location originalLocation) {
        this.originalLocation = originalLocation != null ? originalLocation.clone() : null;
    }

    /**
     * 检查玩家是否在相机模式中
     * @return 是否在相机模式中
     */
    public boolean isInCameraMode() {
        return inCameraMode;
    }

    /**
     * 设置玩家是否在相机模式中
     * @param inCameraMode 是否在相机模式中
     */
    public void setInCameraMode(boolean inCameraMode) {
        this.inCameraMode = inCameraMode;
    }

    /**
     * 检查是否忽略下一次移动事件
     * @return 是否忽略下一次移动事件
     */
    public boolean isIgnoreNextMove() {
        return ignoreNextMove;
    }

    /**
     * 设置是否忽略下一次移动事件
     * @param ignoreNextMove 是否忽略下一次移动事件
     */
    public void setIgnoreNextMove(boolean ignoreNextMove) {
        this.ignoreNextMove = ignoreNextMove;
    }

    /**
     * 进入相机模式
     */
    public void enterCameraMode() {
        if (!inCameraMode) {
            // 保存玩家的原始位置
            Location location = player.getLocation();
            originalLocation = location.clone();
            
            // 更新相机位置为玩家当前位置
            activeCamera.updateCamera(location.clone(),
                    location.getYaw(),
                    location.getPitch()
            );
            
            inCameraMode = true;
        }
    }

    /**
     * 退出相机模式
     */
    public void exitCameraMode() {
        if (inCameraMode) {
            // 停止任何正在进行的动画
            stopAnimation();
            
            // 如果有保存的原始位置，将玩家传送回去
            if (originalLocation != null) {
                player.teleport(originalLocation);
            }
            
            inCameraMode = false;
            originalLocation = null;
        }
    }

    /**
     * 更新相机位置
     * @param location 新位置
     * @param yaw 新偏航角
     * @param pitch 新俯仰角
     */
    public void updateCamera(Location location, float yaw, float pitch) {
        if (inCameraMode) {
            activeCamera.updateCamera(location, yaw, pitch);
            // 标记下一次移动事件为插件触发的
            ignoreNextMove = true;
            // 传送玩家到新位置
            Location newLocation = location.clone();
            newLocation.setYaw(yaw);
            newLocation.setPitch(pitch);
            player.teleport(newLocation);
        }
    }

    /**
     * 设置相机时间轴
     * @param timeline 时间轴
     */
    public void setTimeline(Timeline timeline) {
        this.timeline = timeline;
    }

    /**
     * 启动相机动画
     */
    public void startAnimation() {
        if (timeline.isEmpty()) return;
        
        stopAnimation(); // 停止任何正在进行的动画
        
        this.startTime = System.currentTimeMillis();
        this.isPlaying = true;
        
        // 创建动画任务
        animationTask = Bukkit.getScheduler().runTaskTimer(
                Bukkit.getPluginManager().getPlugin("VirtualCamera"),
                this::updateAnimation,
                1L, 1L // 每tick更新一次
        );
    }

    /**
     * 更新动画状态
     */
    private void updateAnimation() {
        if (!isPlaying) return;
        
        long elapsed = System.currentTimeMillis() - startTime;
        
        // 更新相机位置
        Location location = timeline.getLocationAt(elapsed);
        if (location != null) {
            updateCamera(location, location.getYaw(), location.getPitch());
        }
        
        // 检查并执行文本动作
        List<CameraPreset.TextAction> textActions = timeline.getTextActionsAt(elapsed, 50); // 50ms检查间隔
        for (CameraPreset.TextAction action : textActions) {
            player.sendActionBar(ChatColor.translateAlternateColorCodes('&', action.getText()));
            
            // 如果设置了持续时间，调度清除动作
            if (action.getDuration() > 0) {
                Bukkit.getScheduler().runTaskLater(
                        Bukkit.getPluginManager().getPlugin("VirtualCamera"),
                        () -> player.sendActionBar(""),
                        action.getDuration() / 50
                );
            }
        }
        
        // 检查并执行命令动作
        List<CameraPreset.CommandAction> commandActions = timeline.getCommandActionsAt(elapsed, 50); // 50ms检查间隔
        for (CameraPreset.CommandAction action : commandActions) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), action.getCommand());
        }
        
        // 检查是否已完成
        if (elapsed >= timeline.getTotalDuration()) {
            stopAnimation();
        }
    }

    /**
     * 停止动画
     */
    public void stopAnimation() {
        isPlaying = false;
        if (animationTask != null && !animationTask.isCancelled()) {
            animationTask.cancel();
            animationTask = null;
        }
        // 清除动作栏
        player.sendActionBar("");
    }

    /**
     * 检查动画是否正在播放
     * @return 是否正在播放
     */
    public boolean isPlaying() {
        return isPlaying;
    }

    /**
     * 设置相机路径点
     * @param points 路径点列表
     * @param duration 总持续时间(毫秒)
     */
    public void setPathPoints(List<Location> points, long duration) {
        this.pathPoints = new ArrayList<>(points);
        this.duration = duration;
        this.currentPointIndex = 0;
    }

    /**
     * 启动相机移动
     */
    public void startCameraMovement() {
        if (pathPoints.isEmpty()) return;
        
        this.startTime = System.currentTimeMillis();
        this.currentPointIndex = 0;
        
        // 设置初始位置
        updateCamera(pathPoints.get(0), 
                    pathPoints.get(0).getYaw(), 
                    pathPoints.get(0).getPitch());
    }

    /**
     * 更新相机位置(插值计算)
     */
    public void updateCameraMovement() {
        if (pathPoints.size() < 2 || currentPointIndex >= pathPoints.size() - 1) {
            return;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        float progress = Math.min((float)elapsed / duration, 1.0f);

        Location from = pathPoints.get(currentPointIndex);
        Location to = pathPoints.get(currentPointIndex + 1);
        
        // 线性插值计算
        double x = from.getX() + (to.getX() - from.getX()) * progress;
        double y = from.getY() + (to.getY() - from.getY()) * progress;
        double z = from.getZ() + (to.getZ() - from.getZ()) * progress;
        float yaw = from.getYaw() + (to.getYaw() - from.getYaw()) * progress;
        float pitch = from.getPitch() + (to.getPitch() - from.getPitch()) * progress;

        updateCamera(new Location(from.getWorld(), x, y, z, yaw, pitch), yaw, pitch);

        // 检查是否到达下一个点
        if (progress >= 1.0f) {
            currentPointIndex++;
        }
    }

    /**
     * 执行预设命令
     * @param preset 相机预设
     */
    public void scheduleCommands(CameraPreset preset) {
        for (CameraPreset.CommandAction cmd : preset.getCommands()) {
            BukkitTask task = Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("VirtualCamera"),
                () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.getCommand()),
                cmd.getDelay() / 50
            );
            scheduledTasks.add(task);
        }
    }

    /**
     * 显示预设文本
     * @param preset 相机预设
     */
    public void scheduleTexts(CameraPreset preset) {
        for (CameraPreset.TextAction text : preset.getTexts()) {
            BukkitTask task = Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("VirtualCamera"),
                () -> {
                    player.sendActionBar(ChatColor.translateAlternateColorCodes('&', text.getText()));
                    // 如果设置了持续时间，调度清除动作
                    if (text.getDuration() > 0) {
                        Bukkit.getScheduler().runTaskLater(
                                Bukkit.getPluginManager().getPlugin("VirtualCamera"),
                                () -> player.sendActionBar(""),
                                text.getDuration() / 50
                        );
                    }
                },
                text.getDelay() / 50
            );
            scheduledTasks.add(task);
        }
    }

    /**
     * 清理所有任务
     */
    private void cleanupTasks() {
        scheduledTasks.forEach(task -> {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        });
        scheduledTasks.clear();
        
        stopAnimation();
    }

    /**
     * 重置会话状态
     */
    public void reset() {
        if (inCameraMode) {
            exitCameraMode();
        }
        cleanupTasks();
        originalLocation = null;
        inCameraMode = false;
        ignoreNextMove = false;
        pathPoints.clear();
    }
}