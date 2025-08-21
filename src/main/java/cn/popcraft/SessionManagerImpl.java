package cn.popcraft;

import cn.popcraft.session.CameraSession;
import cn.popcraft.session.SessionManager;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class SessionManagerImpl implements SessionManager {
    private final VirtualCameraPlugin plugin;
    private final Map<UUID, CameraSession> sessions = new HashMap<>();

    public SessionManagerImpl(VirtualCameraPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CameraSession getSession(Player player) {
        UUID playerId = player.getUniqueId();
        if (!sessions.containsKey(playerId)) {
            sessions.put(playerId, new CameraSession(player));
        }
        return sessions.get(playerId);
    }

    @Override
    public boolean hasSession(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    @Override
    public void removeSession(Player player) {
        UUID playerId = player.getUniqueId();
        if (sessions.containsKey(playerId)) {
            CameraSession session = sessions.get(playerId);
            // 确保玩家退出相机模式
            if (session.isInCameraMode()) {
                session.exitCameraMode();
            }
            sessions.remove(playerId);
        }
    }

    @Override
    public void clearAllSessions() {
        // 确保所有玩家退出相机模式
        for (CameraSession session : sessions.values()) {
            if (session.isInCameraMode()) {
                session.exitCameraMode();
            }
        }
        sessions.clear();
    }

    @Override
    public Map<UUID, CameraSession> getAllSessions() {
        return new HashMap<>(sessions);
    }

    @Override
    public int getSessionCount() {
        return sessions.size();
    }

    @Override
    public int getActiveCameraCount() {
        int count = 0;
        for (CameraSession session : sessions.values()) {
            if (session.isInCameraMode()) {
                count++;
            }
        }
        return count;
    }
}