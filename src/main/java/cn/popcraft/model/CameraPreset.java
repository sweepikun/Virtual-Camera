package cn.popcraft.model;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class CameraPreset {
    private final String name;
    private final List<Location> locations = new ArrayList<>();
    private final List<CommandAction> commands = new ArrayList<>();
    private final List<TextAction> texts = new ArrayList<>();
    private CameraType type = CameraType.NORMAL;

    public CameraPreset(String name) {
        this.name = name;
    }

    public enum CameraType {
        NORMAL,
        SPECTATOR,
        CINEMATIC
    }

    public static class CommandAction {
        private final String command;
        private final long delay; // 毫秒

        public CommandAction(String command, long delay) {
            this.command = command;
            this.delay = delay;
        }

        public String getCommand() {
            return command;
        }

        public long getDelay() {
            return delay;
        }
    }

    public static class TextAction {
        private final String text;
        private final long delay; // 毫秒
        private long duration; // 文本显示持续时间(毫秒)

        public TextAction(String text, long delay) {
            this.text = text;
            this.delay = delay;
            this.duration = 3000; // 默认持续3秒
        }

        public TextAction(String text, long delay, long duration) {
            this.text = text;
            this.delay = delay;
            this.duration = duration;
        }

        public String getText() {
            return text;
        }

        public long getDelay() {
            return delay;
        }

        public long getDuration() {
            return duration;
        }

        public void setDuration(long duration) {
            this.duration = duration;
        }
    }

    public String getName() {
        return name;
    }

    public List<Location> getLocations() {
        return new ArrayList<>(locations);
    }

    public void setLocations(List<Location> locations) {
        this.locations.clear();
        this.locations.addAll(locations);
    }

    public List<CommandAction> getCommands() {
        return new ArrayList<>(commands);
    }

    public void addCommand(String command, long delay) {
        this.commands.add(new CommandAction(command, delay));
    }

    public List<TextAction> getTexts() {
        return new ArrayList<>(texts);
    }

    public void addText(String text, long delay) {
        this.texts.add(new TextAction(text, delay));
    }

    public void addText(String text, long delay, long duration) {
        this.texts.add(new TextAction(text, delay, duration));
    }

    public CameraType getType() {
        return type;
    }

    public void setType(CameraType type) {
        this.type = type;
    }

    /**
     * 验证预设是否有效
     */
    public boolean isValid() {
        return !locations.isEmpty();
    }

    /**
     * 添加多个位置点
     */
    public void addLocation(Location location) {
        this.locations.add(location);
    }

    public void addLocations(List<Location> locations) {
        this.locations.addAll(locations);
    }

    /**
     * 添加多个命令
     */
    public void addCommands(List<CommandAction> commands) {
        this.commands.addAll(commands);
    }

    /**
     * 添加多个文本
     */
    public void addTexts(List<TextAction> texts) {
        this.texts.addAll(texts);
    }

    /**
     * 清除所有位置点
     */
    public void clearLocations() {
        this.locations.clear();
    }

    /**
     * 清除所有命令
     */
    public void clearCommands() {
        this.commands.clear();
    }

    /**
     * 清除所有文本
     */
    public void clearTexts() {
        this.texts.clear();
    }

    /**
     * 获取第一个位置点
     */
    public Location getFirstLocation() {
        return locations.isEmpty() ? null : locations.get(0);
    }

    /**
     * 获取最后一个位置点
     */
    public Location getLastLocation() {
        return locations.isEmpty() ? null : locations.get(locations.size() - 1);
    }

    /**
     * 获取位置点数量
     */
    public int getLocationCount() {
        return locations.size();
    }

    /**
     * 获取命令数量
     */
    public int getCommandCount() {
        return commands.size();
    }

    /**
     * 获取文本数量
     */
    public int getTextCount() {
        return texts.size();
    }
}