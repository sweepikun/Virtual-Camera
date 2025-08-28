package cn.popcraft;

import cn.popcraft.manager.CameraManager;
import cn.popcraft.manager.CameraPresetManager;
import cn.popcraft.manager.RandomSwitchController;
import cn.popcraft.manager.TimedSequenceController;
import cn.popcraft.command.CameraCommand;
import cn.popcraft.command.CameraTabCompleter;
import cn.popcraft.listener.CameraListener;
import cn.popcraft.session.SessionManager;
import cn.popcraft.util.ProtocolCameraController;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class VirtualCameraPlugin extends JavaPlugin implements VirtualCamera {
    private CameraPresetManager presetManager;
    private TimedSequenceController sequenceController;
    private RandomSwitchController randomController;
    private CameraManager cameraManager;
    private SessionManager sessionManager;
    private ProtocolCameraController protocolCameraController;
    
    @Override
    public void onEnable() {
        // 保存默认配置
        saveDefaultConfig();
        
        // 初始化管理器
        presetManager = new CameraPresetManager(this);
        sequenceController = new TimedSequenceController(this);
        randomController = new RandomSwitchController(this);
        sessionManager = new SessionManagerImpl(this);
        cameraManager = new CameraManager(this, sessionManager);
        protocolCameraController = new ProtocolCameraController(this);
        
        // 注册命令
        CameraCommand cameraCommand = new CameraCommand(this, sessionManager, cameraManager);
        getCommand("vcam").setExecutor(cameraCommand);
        getCommand("vcam").setTabCompleter(new CameraTabCompleter(cameraManager));
        
        // 注册事件
        getServer().getPluginManager().registerEvents(new CameraListener(this, sessionManager, cameraManager), this);
        
        getLogger().info("VirtualCamera 已启用!");
    }
    
    @Override
    public void onDisable() {
        // 清理所有活动会话
        cameraManager.cleanupAllSessions();
        sequenceController.cleanupAllSequences();
        randomController.cleanupAllRandomSwitches();
        
        getLogger().info("VirtualCamera 已禁用!");
    }
    
    // Getter 方法
    public CameraPresetManager getPresetManager() {
        return presetManager;
    }
    
    public TimedSequenceController getSequenceController() {
        return sequenceController;
    }
    
    public RandomSwitchController getRandomController() {
        return randomController;
    }
    
    public CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    public cn.popcraft.session.SessionManager getSessionManager() {
        return sessionManager;
    }

    @Override
    public Plugin getPlugin() {
        return this;
    }
    
    public ProtocolCameraController getProtocolCameraController() {
        return protocolCameraController;
    }
}