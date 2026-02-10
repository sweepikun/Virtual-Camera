package cn.popcraft.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于ProtocolLib的摄像机控制器
 * 使用ArmorStand作为摄像机代理，实现"灵魂出窍"效果
 * 玩家可以看到自己的身体，同时视角跟随摄像机移动
 */
public class ProtocolCameraController {
    private final Plugin plugin;
    private final ProtocolManager protocolManager;
    private final Map<UUID, CameraSession> cameraSessions = new HashMap<>();
    private final AtomicInteger entityIdCounter = new AtomicInteger(Integer.MAX_VALUE - 10000);
    
    public ProtocolCameraController(Plugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        registerPacketListeners();
    }
    
    /**
     * 开始摄像机模式 - 创建不可见的ArmorStand并让玩家观察它
     */
    public void startCameraMode(Player player) {
        UUID playerId = player.getUniqueId();
        if (cameraSessions.containsKey(playerId)) {
            return;
        }
        
        Location originalLocation = player.getLocation().clone();
        int cameraEntityId = generateEntityId();
        UUID cameraEntityUUID = UUID.randomUUID();
        
        CameraSession session = new CameraSession(player, originalLocation, cameraEntityId, cameraEntityUUID);
        cameraSessions.put(playerId, session);
        
        Location eyeLocation = player.getEyeLocation();
        
        try {
            spawnCameraEntity(player, cameraEntityId, cameraEntityUUID, eyeLocation);
            sendCameraPacket(player, cameraEntityId);
        } catch (Exception e) {
            plugin.getLogger().severe("启动摄像机模式失败: " + e.getMessage());
            cameraSessions.remove(playerId);
        }
    }
    
    /**
     * 停止摄像机模式 - 恢复玩家视角并删除摄像机实体
     */
    public void stopCameraMode(Player player) {
        UUID playerId = player.getUniqueId();
        CameraSession session = cameraSessions.remove(playerId);
        if (session == null) {
            return;
        }
        
        try {
            sendCameraPacket(player, player.getEntityId());
            destroyCameraEntity(player, session.getCameraEntityId());
        } catch (Exception e) {
            plugin.getLogger().severe("停止摄像机模式失败: " + e.getMessage());
        }
    }
    
    /**
     * 设置摄像机位置和视角
     */
    public void setCameraPosition(Player player, Location location) {
        UUID playerId = player.getUniqueId();
        CameraSession session = cameraSessions.get(playerId);
        if (session == null) {
            return;
        }
        
        session.setCurrentLocation(location.clone());
        teleportCameraEntity(player, session.getCameraEntityId(), location);
    }
    
    /**
     * 播放摄像机动画
     */
    public void playCameraAnimation(Player player, cn.popcraft.util.Timeline timeline, long duration) {
        UUID playerId = player.getUniqueId();
        CameraSession session = cameraSessions.get(playerId);
        if (session == null) {
            return;
        }
        
        session.setAnimating(true);
        
        new BukkitRunnable() {
            private long startTime = System.currentTimeMillis();
            private long endTime = startTime + duration;
            
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                if (currentTime >= endTime || !cameraSessions.containsKey(playerId)) {
                    session.setAnimating(false);
                    cancel();
                    return;
                }
                
                long elapsed = currentTime - startTime;
                Location currentLocation = timeline.getLocationAt(elapsed);
                if (currentLocation != null) {
                    if (currentLocation.getWorld() == null) {
                        currentLocation.setWorld(player.getWorld());
                    }
                    setCameraPosition(player, currentLocation);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    /**
     * 检查玩家是否在摄像机模式中
     */
    public boolean isInCameraMode(Player player) {
        CameraSession session = cameraSessions.get(player.getUniqueId());
        return session != null;
    }
    
    /**
     * 检查玩家是否正在播放动画
     */
    public boolean isAnimating(Player player) {
        CameraSession session = cameraSessions.get(player.getUniqueId());
        return session != null && session.isAnimating();
    }
    
    /**
     * 清理所有摄像机会话
     */
    public void cleanup() {
        for (UUID playerId : new HashMap<>(cameraSessions).keySet()) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) {
                stopCameraMode(player);
            } else {
                cameraSessions.remove(playerId);
            }
        }
    }
    
    /**
     * 生成唯一的实体ID
     */
    private int generateEntityId() {
        return entityIdCounter.decrementAndGet();
    }
    
    /**
     * 生成摄像机实体(ArmorStand)
     */
    private void spawnCameraEntity(Player player, int entityId, UUID entityUUID, Location location) {
        try {
            PacketContainer spawnPacket = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
            
            spawnPacket.getIntegers().write(0, entityId);
            spawnPacket.getUUIDs().write(0, entityUUID);
            spawnPacket.getEntityTypeModifier().write(0, EntityType.ARMOR_STAND);
            
            spawnPacket.getDoubles().write(0, location.getX());
            spawnPacket.getDoubles().write(1, location.getY());
            spawnPacket.getDoubles().write(2, location.getZ());
            
            int yaw = (int) (location.getYaw() * 256.0F / 360.0F);
            int pitch = (int) (location.getPitch() * 256.0F / 360.0F);
            spawnPacket.getIntegers().write(1, yaw);
            spawnPacket.getIntegers().write(2, pitch);
            spawnPacket.getIntegers().write(3, 0);
            
            protocolManager.sendServerPacket(player, spawnPacket);
            
            WrappedDataWatcher watcher = new WrappedDataWatcher();
            
            byte flags = 0x20;
            watcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, WrappedDataWatcher.Registry.get(Byte.class)), flags);
            
            byte armorStandFlags = 0x10 | 0x08 | 0x01;
            watcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(15, WrappedDataWatcher.Registry.get(Byte.class)), armorStandFlags);
            
            PacketContainer metadataPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            metadataPacket.getIntegers().write(0, entityId);
            metadataPacket.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
            
            protocolManager.sendServerPacket(player, metadataPacket);
            
        } catch (Exception e) {
            plugin.getLogger().severe("生成摄像机实体失败: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    /**
     * 传送摄像机实体到指定位置
     */
    private void teleportCameraEntity(Player player, int entityId, Location location) {
        try {
            PacketContainer teleportPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
            
            teleportPacket.getIntegers().write(0, entityId);
            teleportPacket.getDoubles().write(0, location.getX());
            teleportPacket.getDoubles().write(1, location.getY());
            teleportPacket.getDoubles().write(2, location.getZ());
            
            byte yaw = (byte) (location.getYaw() * 256.0F / 360.0F);
            byte pitch = (byte) (location.getPitch() * 256.0F / 360.0F);
            teleportPacket.getBytes().write(0, yaw);
            teleportPacket.getBytes().write(1, pitch);
            
            teleportPacket.getBooleans().write(0, false);
            
            protocolManager.sendServerPacket(player, teleportPacket);
            
        } catch (Exception e) {
            plugin.getLogger().severe("传送摄像机实体失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送Camera数据包切换玩家视角
     */
    private void sendCameraPacket(Player player, int targetEntityId) {
        try {
            PacketContainer cameraPacket = protocolManager.createPacket(PacketType.Play.Server.CAMERA);
            cameraPacket.getIntegers().write(0, targetEntityId);
            protocolManager.sendServerPacket(player, cameraPacket);
        } catch (Exception e) {
            plugin.getLogger().severe("发送Camera数据包失败: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    /**
     * 销毁摄像机实体
     */
    private void destroyCameraEntity(Player player, int entityId) {
        try {
            PacketContainer destroyPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            destroyPacket.getIntLists().write(0, java.util.Collections.singletonList(entityId));
            protocolManager.sendServerPacket(player, destroyPacket);
        } catch (Exception e) {
            plugin.getLogger().severe("销毁摄像机实体失败: " + e.getMessage());
        }
    }
    
    /**
     * 注册数据包监听器 - 防止动画期间客户端移动干扰
     */
    private void registerPacketListeners() {
        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Client.POSITION) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                CameraSession session = cameraSessions.get(player.getUniqueId());
                if (session != null && session.isAnimating()) {
                    event.setCancelled(true);
                }
            }
        });
        
        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Client.POSITION_LOOK) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                CameraSession session = cameraSessions.get(player.getUniqueId());
                if (session != null && session.isAnimating()) {
                    event.setCancelled(true);
                }
            }
        });
        
        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Client.LOOK) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                CameraSession session = cameraSessions.get(player.getUniqueId());
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
        private final int cameraEntityId;
        private final UUID cameraEntityUUID;
        private Location currentLocation;
        private boolean animating = false;
        
        public CameraSession(Player player, Location originalLocation, int cameraEntityId, UUID cameraEntityUUID) {
            this.player = player;
            this.originalLocation = originalLocation;
            this.cameraEntityId = cameraEntityId;
            this.cameraEntityUUID = cameraEntityUUID;
            this.currentLocation = originalLocation.clone();
        }
        
        public int getCameraEntityId() {
            return cameraEntityId;
        }
        
        public UUID getCameraEntityUUID() {
            return cameraEntityUUID;
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
}
