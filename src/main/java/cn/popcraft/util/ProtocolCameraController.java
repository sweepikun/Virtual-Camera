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
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于ProtocolLib的摄像机控制器
 * 使用ArmorStand作为摄像机代理，实现"灵魂出窍"效果
 * 玩家可以看到自己的身体，同时视角跟随摄像机移动
 */
public class ProtocolCameraController {
    /** 一个服务器tick对应的毫秒数，动画进度按tick推进，避免用墙钟造成的抖动 */
    private static final long TICK_MS = 50L;

    private final Plugin plugin;
    private final ProtocolManager protocolManager;
    private final boolean available;

    private final Map<UUID, CameraSession> cameraSessions = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> animationTasks = new ConcurrentHashMap<>();
    private final AtomicInteger entityIdCounter = new AtomicInteger(Integer.MAX_VALUE - 10000);

    public ProtocolCameraController(Plugin plugin) {
        this.plugin = plugin;

        ProtocolManager manager = null;
        try {
            manager = ProtocolLibrary.getProtocolManager();
        } catch (Throwable t) {
            // ProtocolLib 未安装或版本不兼容：控制器降级为空实现，插件仍可加载
            manager = null;
        }
        this.protocolManager = manager;
        this.available = manager != null;

        if (available) {
            try {
                registerPacketListeners();
            } catch (Throwable t) {
                plugin.getLogger().warning("ProtocolLib 数据包监听注册失败，协议运镜可能受限: " + t.getMessage());
            }
        } else {
            plugin.getLogger().warning("未检测到 ProtocolLib，协议运镜不可用，将自动回退为传送运镜。");
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * 开始摄像机模式 - 创建不可见的ArmorStand并让玩家观察它
     */
    public void startCameraMode(Player player) {
        if (!available) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (cameraSessions.containsKey(playerId) || !player.isOnline()) {
            return;
        }

        Location originalLocation = player.getLocation().clone();
        int cameraEntityId = generateEntityId();
        UUID cameraEntityUUID = UUID.randomUUID();

        CameraSession session = new CameraSession(cameraEntityId, originalLocation);
        cameraSessions.put(playerId, session);

        try {
            spawnCameraEntity(player, cameraEntityId, cameraEntityUUID, player.getEyeLocation());
            sendCameraPacket(player, cameraEntityId);
        } catch (Exception e) {
            plugin.getLogger().severe("启动摄像机模式失败: " + e.getMessage());
            cameraSessions.remove(playerId);
            cancelAnimationTask(playerId);
        }
    }

    /**
     * 停止摄像机模式 - 恢复玩家视角并删除摄像机实体
     */
    public void stopCameraMode(Player player) {
        UUID playerId = player.getUniqueId();
        CameraSession session = cameraSessions.remove(playerId);
        cancelAnimationTask(playerId);

        if (session == null || !available || !player.isOnline()) {
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
        if (session == null || !available || !player.isOnline()) {
            return;
        }

        Location target = location.clone();
        Location last = session.getCurrentLocation();
        session.setCurrentLocation(target);

        // 位置和角度都没变时不再发包，避免无谓的网络流量和客户端的重复插值
        if (last != null && samePlacement(last, target)) {
            return;
        }
        teleportCameraEntity(player, session.getCameraEntityId(), target);
    }

    /**
     * 播放摄像机动画。
     * 进度按服务器tick推进（每个tick一个等距采样），而不是墙钟：TPS波动时只会整体变慢，
     * 不会出现"某一跳突然跨过大段距离"的卡顿。
     */
    public void playCameraAnimation(Player player, cn.popcraft.util.Timeline timeline, long duration) {
        UUID playerId = player.getUniqueId();
        CameraSession session = cameraSessions.get(playerId);
        if (session == null || !available || timeline == null || timeline.getKeyframeCount() == 0) {
            return;
        }
        if (duration <= 0) {
            duration = 1000L;
        }

        cancelAnimationTask(playerId);
        session.setAnimating(true);

        final int totalTicks = (int) Math.max(1L, Math.round(duration / (double) TICK_MS));
        final Location finalKeyframe = timeline.getKeyframe(timeline.getKeyframeCount() - 1);

        BukkitTask task = new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                CameraSession current = cameraSessions.get(playerId);
                if (current == null || !player.isOnline() || !current.isAnimating()) {
                    cancel();
                    return;
                }

                if (tick >= totalTicks) {
                    current.setAnimating(false);
                    if (finalKeyframe != null) {
                        Location end = finalKeyframe.clone();
                        if (end.getWorld() == null) {
                            end.setWorld(player.getWorld());
                        }
                        setCameraPosition(player, end);
                    }
                    cancel();
                    return;
                }

                Location currentLocation = timeline.getLocationAt(tick * TICK_MS);
                if (currentLocation != null) {
                    Location loc = currentLocation.clone();
                    if (loc.getWorld() == null) {
                        loc.setWorld(player.getWorld());
                    }
                    setCameraPosition(player, loc);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        animationTasks.put(playerId, task);
    }

    /**
     * 结束当前动画但保持摄像机模式
     */
    public void stopAnimation(Player player) {
        UUID playerId = player.getUniqueId();
        cancelAnimationTask(playerId);
        CameraSession session = cameraSessions.get(playerId);
        if (session != null) {
            session.setAnimating(false);
        }
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
        for (UUID playerId : new ConcurrentHashMap<>(cameraSessions).keySet()) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) {
                stopCameraMode(player);
            } else {
                cancelAnimationTask(playerId);
                cameraSessions.remove(playerId);
            }
        }
        animationTasks.clear();
    }

    /**
     * 生成唯一的实体ID
     */
    private int generateEntityId() {
        return entityIdCounter.decrementAndGet();
    }

    /**
     * 把度数换算成协议用的角度单位（协议以 256 为单位表示整圈）
     */
    private static int toProtocolAngle(float degrees) {
        return ((int) (degrees * 256.0F / 360.0F)) & 0xFF;
    }

    /**
     * 角度单位中的协议字节
     */
    private static byte toProtocolAngleByte(float degrees) {
        return (byte) toProtocolAngle(degrees);
    }

    private static boolean samePlacement(Location a, Location b) {
        return a.getX() == b.getX() && a.getY() == b.getY() && a.getZ() == b.getZ()
                && a.getYaw() == b.getYaw() && a.getPitch() == b.getPitch();
    }

    private void cancelAnimationTask(UUID playerId) {
        BukkitTask task = animationTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
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

            spawnPacket.getIntegers().write(1, toProtocolAngle(location.getYaw()));
            spawnPacket.getIntegers().write(2, toProtocolAngle(location.getPitch()));
            spawnPacket.getIntegers().write(3, 0);

            protocolManager.sendServerPacket(player, spawnPacket);

            WrappedDataWatcher watcher = new WrappedDataWatcher();

            // 0x20 = 隐身，0x10 = marker(无碰撞箱)，0x08 = 无底座，0x01 = 小型
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
        if (entityId == 0) {
            return;
        }
        try {
            PacketContainer teleportPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);

            teleportPacket.getIntegers().write(0, entityId);
            teleportPacket.getDoubles().write(0, location.getX());
            teleportPacket.getDoubles().write(1, location.getY());
            teleportPacket.getDoubles().write(2, location.getZ());

            teleportPacket.getBytes().write(0, toProtocolAngleByte(location.getYaw()));
            teleportPacket.getBytes().write(1, toProtocolAngleByte(location.getPitch()));

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
     * 这些监听在异步线程触发，所以所有状态访问必须是并发安全的
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
        private final int cameraEntityId;
        private Location currentLocation;
        private boolean animating = false;

        public CameraSession(int cameraEntityId, Location initialLocation) {
            this.cameraEntityId = cameraEntityId;
            this.currentLocation = initialLocation.clone();
        }

        public int getCameraEntityId() {
            return cameraEntityId;
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
