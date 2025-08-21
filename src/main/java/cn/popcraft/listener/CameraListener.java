package cn.popcraft.listener;

import cn.popcraft.VirtualCamera;
import cn.popcraft.manager.CameraManager;
import cn.popcraft.model.Camera;
import cn.popcraft.session.CameraSession;
import cn.popcraft.session.SessionManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * 相机事件监听器
 */
public class CameraListener implements Listener {
    private final VirtualCamera plugin;
    private final SessionManager sessionManager;
    private final CameraManager cameraManager;

    public CameraListener(VirtualCamera plugin, SessionManager sessionManager, CameraManager cameraManager) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
        this.cameraManager = cameraManager;
    }

    /**
     * 处理玩家移动事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // 检查玩家是否在相机模式中
        if (!sessionManager.hasSession(player)) {
            return;
        }
        
        CameraSession session = sessionManager.getSession(player);
        if (!session.isInCameraMode()) {
            return;
        }
        
        // 如果这是插件触发的移动，忽略它
        if (session.isIgnoreNextMove()) {
            session.setIgnoreNextMove(false);
            return;
        }
        
        // 获取移动前后的位置
        Location from = event.getFrom();
        Location to = event.getTo();
        
        // 如果只是视角变化（没有位置变化）
        if (to != null && from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {
            // 更新相机的视角
            Camera camera = session.getActiveCamera();
            camera.setYaw(to.getYaw());
            camera.setPitch(to.getPitch());
        } else {
            // 如果是位置变化，更新相机位置
            if (to != null) {
                Camera camera = session.getActiveCamera();
                camera.setLocation(to.clone());
                camera.setYaw(to.getYaw());
                camera.setPitch(to.getPitch());
            }
        }
    }

    /**
     * 处理玩家传送事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        
        // 检查玩家是否在相机模式中
        if (!sessionManager.hasSession(player)) {
            return;
        }
        
        CameraSession session = sessionManager.getSession(player);
        if (!session.isInCameraMode()) {
            return;
        }
        
        // 如果这是插件触发的传送，忽略它
        if (session.isIgnoreNextMove()) {
            session.setIgnoreNextMove(false);
            return;
        }
        
        // 更新相机位置
        Location to = event.getTo();
        if (to != null) {
            Camera camera = session.getActiveCamera();
            camera.setLocation(to.clone());
            camera.setYaw(to.getYaw());
            camera.setPitch(to.getPitch());
        }
    }

    /**
     * 处理玩家退出事件
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // 如果玩家在相机模式中，确保他们退出相机模式
        if (sessionManager.hasSession(player)) {
            CameraSession session = sessionManager.getSession(player);
            if (session.isInCameraMode()) {
                cameraManager.exitCameraMode(player);
            }
            
            // 移除会话
            sessionManager.removeSession(player);
        }
    }

    /**
     * 处理玩家加入事件
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 检查玩家是否有未正确清理的相机会话
        if (sessionManager.hasSession(player)) {
            CameraSession session = sessionManager.getSession(player);
            if (session.isInCameraMode()) {
                // 确保玩家退出相机模式
                cameraManager.exitCameraMode(player);
            }
        }
    }
}