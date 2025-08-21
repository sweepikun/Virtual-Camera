package cn.popcraft;

import cn.popcraft.session.CameraSession;
import org.bukkit.entity.Player;

public interface SessionManager {
    CameraSession getSession(Player player);
    void createSession(Player player);
    void removeSession(Player player);
    void cleanupAllSessions();
}