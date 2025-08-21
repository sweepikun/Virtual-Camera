package cn.popcraft;

import org.bukkit.plugin.Plugin;
import cn.popcraft.manager.CameraManager;
import cn.popcraft.manager.CameraPresetManager;
import cn.popcraft.manager.RandomSwitchController;
import cn.popcraft.manager.TimedSequenceController;
import cn.popcraft.session.SessionManager;

public interface VirtualCamera {
    CameraPresetManager getPresetManager();
    TimedSequenceController getSequenceController();
    RandomSwitchController getRandomController();
    CameraManager getCameraManager();
    SessionManager getSessionManager();
    Plugin getPlugin();
}