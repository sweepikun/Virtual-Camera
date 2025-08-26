package cn.popcraft.model;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class CameraPreset {
    private final String name;
    private final List<Location> locations = new ArrayList<>();
    private final List<CommandAction> commands = new ArrayList<>();
    private final List<TextAction> texts = new ArrayList<>();
    private final List<SegmentInfo> segmentInfos = new ArrayList<>(); // 每个段落的信息
    private CameraType type = CameraType.NORMAL;

    public CameraPreset(String name) {
        this.name = name;
    }

    public enum CameraType {
        NORMAL,
        SPECTATOR,
        CINEMATIC
    }

    /**
     * 段落信息类，存储每个路径段落的过渡类型和持续时间
     */
    public static class SegmentInfo {
        private TransitionType transitionType;
        private long duration; // 毫秒

        public SegmentInfo(TransitionType transitionType, long duration) {
            this.transitionType = transitionType;
            this.duration = duration;
        }

        public TransitionType getTransitionType() {
            return transitionType;
        }

        public void setTransitionType(TransitionType transitionType) {
            this.transitionType = transitionType;
        }

        public long getDuration() {
            return duration;
        }

        public void setDuration(long duration) {
            this.duration = duration;
        }
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
        // 同步更新段落信息
        updateSegmentInfos();
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
        // 如果不是第一个点，添加默认段落信息
        if (locations.size() > 1) {
            segmentInfos.add(new SegmentInfo(TransitionType.SMOOTH, 3000)); // 默认3秒
        }
    }

    public void addLocations(List<Location> locations) {
        this.locations.addAll(locations);
        updateSegmentInfos();
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
        this.segmentInfos.clear();
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
    
    /**
     * 获取段落信息
     * @return 段落信息列表
     */
    public List<SegmentInfo> getSegmentInfos() {
        return new ArrayList<>(segmentInfos);
    }
    
    /**
     * 设置段落的过渡类型和持续时间
     * @param segmentIndex 段落索引
     * @param transitionType 过渡类型
     * @param duration 持续时间(毫秒)
     */
    public void setSegmentInfo(int segmentIndex, TransitionType transitionType, long duration) {
        if (segmentIndex >= 0 && segmentIndex < segmentInfos.size()) {
            segmentInfos.get(segmentIndex).setTransitionType(transitionType);
            segmentInfos.get(segmentIndex).setDuration(duration);
        }
    }
    
    /**
     * 获取指定段落的过渡类型
     * @param segmentIndex 段落索引
     * @return 过渡类型
     */
    public TransitionType getSegmentTransitionType(int segmentIndex) {
        if (segmentIndex >= 0 && segmentIndex < segmentInfos.size()) {
            return segmentInfos.get(segmentIndex).getTransitionType();
        }
        return TransitionType.SMOOTH; // 默认值
    }
    
    /**
     * 获取指定段落的持续时间
     * @param segmentIndex 段落索引
     * @return 持续时间(毫秒)
     */
    public long getSegmentDuration(int segmentIndex) {
        if (segmentIndex >= 0 && segmentIndex < segmentInfos.size()) {
            return segmentInfos.get(segmentIndex).getDuration();
        }
        return 3000; // 默认值3秒
    }
    
    /**
     * 更新段落信息，确保与位置点数量同步
     */
    private void updateSegmentInfos() {
        // 清除多余的段落信息
        while (segmentInfos.size() >= locations.size()) {
            segmentInfos.remove(segmentInfos.size() - 1);
        }
        
        // 添加缺少的段落信息
        while (segmentInfos.size() < locations.size() - 1) {
            segmentInfos.add(new SegmentInfo(TransitionType.SMOOTH, 3000));
        }
    }
}