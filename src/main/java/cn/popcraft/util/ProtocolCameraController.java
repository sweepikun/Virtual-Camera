package cn.popcraft.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 基于ProtocolLib的摄像机控制器
 * 通过拦截和修改数据包实现真正的摄像机移动效果，而不移动玩家实体
 */
public class ProtocolCameraController {
    private final Plugin plugin;
    private final ProtocolManager protocolManager;
    private final Map<UUID, CameraSession> cameraSessions = new HashMap<>();
    
    public ProtocolCameraController(Plugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        
        // 注册数据包监听器
        registerPacketListeners();
    }
    
    /**
     * 开始摄像机模式
     * @param player 玩家
     */
    public void startCameraMode(Player player) {
        UUID playerId = player.getUniqueId();
        if (!cameraSessions.containsKey(playerId)) {
            // 保存玩家原始位置和视角
            Location originalLocation = player.getLocation().clone();
            cameraSessions.put(playerId, new CameraSession(player, originalLocation));
        }
    }
    
    /**
     * 停止摄像机模式
     * @param player 玩家
     */
    public void stopCameraMode(Player player) {
        UUID playerId = player.getUniqueId();
        CameraSession session = cameraSessions.remove(playerId);
        if (session != null) {
            // 恢复玩家原始位置
            player.teleport(session.getOriginalLocation());
        }
    }
    
    /**
     * 设置摄像机位置和视角
     * @param player 玩家
     * @param location 目标位置和视角
     */
    public void setCameraPosition(Player player, Location location) {
        UUID playerId = player.getUniqueId();
        CameraSession session = cameraSessions.get(playerId);
        if (session != null) {
            session.setCurrentLocation(location.clone());
            // 发送位置更新数据包给玩家
            sendPositionPacket(player, location);
        }
    }
    
    /**
     * 开始播放摄像机动画
     * @param player 玩家
     * @param timeline 时间轴
     * @param duration 总持续时间(毫秒)
     */
    public void playCameraAnimation(Player player, Timeline timeline, long duration) {
        UUID playerId = player.getUniqueId();
        CameraSession session = cameraSessions.get(playerId);
        if (session != null) {
            session.setAnimating(true);
            
            new BukkitRunnable() {
                private long startTime = System.currentTimeMillis();
                private long endTime = startTime + duration;
                
                @Override
                public void run() {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime >= endTime) {
                        // 动画结束
                        session.setAnimating(false);
                        cancel();
                        return;
                    }
                    
                    // 计算进度 (0.0 - 1.0)
                    float progress = (float) (currentTime - startTime) / (endTime - startTime);
                    
                    // 获取当前位置
                    Location currentLocation = timeline.getLocationAt(progress);
                    if (currentLocation != null) {
                        setCameraPosition(player, currentLocation);
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L); // 每tick更新一次
        }
    }
    
    /**
     * 发送位置数据包给玩家
     * @param player 玩家
     * @param location 位置
     */
    private void sendPositionPacket(Player player, Location location) {
        // 注意：实际实现需要根据Minecraft版本调整数据包结构
        // 这里提供一个概念性的实现
        
        /*
        // 创建位置更新数据包
        PacketContainer positionPacket = protocolManager.createPacket(PacketType.Play.Server.POSITION);
        
        // 设置位置参数
        positionPacket.getDoubles().write(0, location.getX());
        positionPacket.getDoubles().write(1, location.getY());
        positionPacket.getDoubles().write(2, location.getZ());
        
        // 设置视角参数
        positionPacket.getFloat().write(0, location.getYaw());
        positionPacket.getFloat().write(1, location.getPitch());
        
        // 设置标志位
        positionPacket.getBooleans().write(0, false); // 是否相对X
        positionPacket.getBooleans().write(1, false); // 是否相对Y
        positionPacket.getBooleans().write(2, false); // 是否相对Z
        
        // 发送数据包
        try {
            protocolManager.sendServerPacket(player, positionPacket);
        } catch (Exception e) {
            plugin.getLogger().severe("发送位置数据包失败: " + e.getMessage());
        }
        */
    }
    
    /**
     * 注册数据包监听器
     */
    private void registerPacketListeners() {
        // 拦截位置确认数据包，防止客户端位置更新影响摄像机效果
        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Client.POSITION) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                UUID playerId = player.getUniqueId();
                CameraSession session = cameraSessions.get(playerId);
                
                // 如果玩家正在摄像机模式中，忽略位置更新
                if (session != null && session.isAnimating()) {
                    event.setCancelled(true);
                }
            }
        });
        
        // 拦截位置和视角数据包
        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Client.POSITION_LOOK) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                UUID playerId = player.getUniqueId();
                CameraSession session = cameraSessions.get(playerId);
                
                // 如果玩家正在摄像机模式中，忽略位置和视角更新
                if (session != null && session.isAnimating()) {
                    event.setCancelled(true);
                }
            }
        });
        
        // 拦截视角数据包
        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Client.LOOK) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                UUID playerId = player.getUniqueId();
                CameraSession session = cameraSessions.get(playerId);
                
                // 如果玩家正在摄像机模式中，忽略视角更新
                if (session != null && session.isAnimating()) {
                    event.setCancelled(true);
                }
            }
        });
    }
    
    /**
     * 摄像机会话类
     */
    private static class CameraSession {
        private final Player player;
        private final Location originalLocation;
        private Location currentLocation;
        private boolean animating = false;
        
        public CameraSession(Player player, Location originalLocation) {
            this.player = player;
            this.originalLocation = originalLocation;
            this.currentLocation = originalLocation.clone();
        }
        
        public Player getPlayer() {
            return player;
        }
        
        public Location getOriginalLocation() {
            return originalLocation;
        }
        
        public Location getCurrentLocation() {
            return currentLocation;
        }
        
        public void setCurrentLocation(Location currentLocation) {
            this.currentLocation = currentLocation;
        }
        
        public boolean isAnimating() {
            return animating;
        }
        
        public void setAnimating(boolean animating) {
            this.animating = animating;
        }
    }
    
    /**
     * 时间轴类，用于管理摄像机动画关键帧
     */
    public static class Timeline {
        private final Map<Float, Location> keyframes = new HashMap<>();
        
        /**
         * 添加关键帧
         * @param progress 进度 (0.0 - 1.0)
         * @param location 位置
         */
        public void addKeyframe(float progress, Location location) {
            keyframes.put(progress, location.clone());
        }
        
        /**
         * 获取指定进度时的位置
         * @param progress 进度 (0.0 - 1.0)
         * @return 位置
         */
        public Location getLocationAt(float progress) {
            // 简单线性插值实现
            // 实际应用中可以使用更复杂的插值算法
            
            if (keyframes.isEmpty()) {
                return null;
            }
            
            if (keyframes.size() == 1) {
                return keyframes.values().iterator().next();
            }
            
            // 找到相邻的关键帧
            Float prevKey = null;
            Float nextKey = null;
            
            for (Float key : keyframes.keySet()) {
                if (key <= progress && (prevKey == null || key > prevKey)) {
                    prevKey = key;
                }
                if (key >= progress && (nextKey == null || key < nextKey)) {
                    nextKey = key;
                }
            }
            
            if (prevKey == null) {
                return keyframes.get(nextKey);
            }
            
            if (nextKey == null) {
                return keyframes.get(prevKey);
            }
            
            if (prevKey.equals(nextKey)) {
                return keyframes.get(prevKey);
            }
            
            // 线性插值计算位置
            Location prevLoc = keyframes.get(prevKey);
            Location nextLoc = keyframes.get(nextKey);
            
            float t = (progress - prevKey) / (nextKey - prevKey);
            
            double x = prevLoc.getX() + (nextLoc.getX() - prevLoc.getX()) * t;
            double y = prevLoc.getY() + (nextLoc.getY() - prevLoc.getY()) * t;
            double z = prevLoc.getZ() + (nextLoc.getZ() - prevLoc.getZ()) * t;
            float yaw = interpolateAngle(prevLoc.getYaw(), nextLoc.getYaw(), t);
            float pitch = interpolateAngle(prevLoc.getPitch(), nextLoc.getPitch(), t);
            
            return new Location(prevLoc.getWorld(), x, y, z, yaw, pitch);
        }
        
        /**
         * 角度插值（处理角度环绕问题）
         * @param start 起始角度
         * @param end 结束角度
         * @param t 插值参数
         * @return 插值后的角度
         */
        private float interpolateAngle(float start, float end, float t) {
            // 处理角度环绕问题
            float diff = end - start;
            if (diff > 180) {
                diff -= 360;
            } else if (diff < -180) {
                diff += 360;
            }
            return start + diff * t;
        }
    }
}