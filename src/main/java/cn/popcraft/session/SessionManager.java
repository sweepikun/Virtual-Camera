package cn.popcraft.session;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * 会话管理器接口，管理所有玩家的相机会话
 */
public interface SessionManager {
    /**
     * 获取玩家的相机会话，如果不存在则创建
     * @param player 玩家
     * @return 相机会话
     */
    CameraSession getSession(Player player);

    /**
     * 检查玩家是否有相机会话
     * @param player 玩家
     * @return 是否有相机会话
     */
    boolean hasSession(Player player);

    /**
     * 移除玩家的相机会话
     * @param player 玩家
     */
    void removeSession(Player player);

    /**
     * 清除所有相机会话
     */
    void clearAllSessions();

    /**
     * 获取所有相机会话
     * @return 所有相机会话
     */
    Map<UUID, CameraSession> getAllSessions();

    /**
     * 获取当前会话数量
     * @return 会话数量
     */
    int getSessionCount();

    /**
     * 获取处于相机模式的玩家数量
     * @return 处于相机模式的玩家数量
     */
    int getActiveCameraCount();
}