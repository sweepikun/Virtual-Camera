package cn.popcraft.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 摄像机预设位置
 */
public class CameraPreset {
    private final String name;
    private List<Location> locations = new ArrayList<>();
    private CameraType type = CameraType.NORMAL;
    private List<CommandAction> commands = new ArrayList<>();
    private List<TextAction> texts = new ArrayList<>();

    public static class CommandAction {
        private String command;
        private long delay; // 毫秒

        public CommandAction(String command, long delay) {
            this.command = command;
            this.delay = delay;
        }

        // getters and setters
    }

    public static class TextAction {
        private String text;
        private long delay; // 毫秒
        private long duration; // 毫秒

        public TextAction(String text, long delay, long duration) {
            this.text = text;
            this.delay = delay;
            this.duration = duration;
        }

        // getters and setters
    }

    public CameraPreset(String name) {
        this.name = name;
    }

    public CameraPreset(String name, Location location) {
        this.name = name;
        this.locations.add(location);
    }

    public String getName() {
        return name;
    }

    public List<Location> getLocations() {
        return locations;
    }

    public void addLocation(Location location) {
        this.locations.add(location);
    }

    public void removeLocation(int index) {
        this.locations.remove(index);
    }

    public Location getFirstLocation() {
        return locations.isEmpty() ? null : locations.get(0);
    }

    public List<CommandAction> getCommands() {
        return commands;
    }

    public void addCommand(String command, long delay) {
        this.commands.add(new CommandAction(command, delay));
    }

    public List<TextAction> getTexts() {
        return texts;
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

    @Override
    public String toString() {
        return String.format("CameraPreset{name='%s', locations=%s, commands=%s, texts=%s, type=%s}",
                name, locations.size(), commands.size(), texts.size(), type);
    }

    // CommandAction getters/setters
    public static class CommandAction {
        private String command;
        private long delay;

        public CommandAction(String command, long delay) {
            this.command = command;
            this.delay = delay;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public long getDelay() {
            return delay;
        }

        public void setDelay(long delay) {
            this.delay = delay;
        }
    }

    // TextAction getters/setters
    public static class TextAction {
        private String text;
        private long delay;
        private long duration;

        public TextAction(String text, long delay, long duration) {
            this.text = text;
            this.delay = delay;
            this.duration = duration;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public long getDelay() {
            return delay;
        }

        public void setDelay(long delay) {
            this.delay = delay;
        }

        public long getDuration() {
            return duration;
        }

        public void setDuration(long duration) {
            this.duration = duration;
        }
    }
}