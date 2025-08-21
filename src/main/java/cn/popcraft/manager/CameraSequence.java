package cn.popcraft.manager;

import java.util.ArrayList;
import java.util.List;

/**
 * 相机序列类，表示一系列相机预设的播放顺序
 */
public class CameraSequence {
    private final String name;
    private final List<SequenceEntry> entries;
    private boolean loop;

    /**
     * 创建一个新的相机序列
     * @param name 序列名称
     */
    public CameraSequence(String name) {
        this.name = name;
        this.entries = new ArrayList<>();
        this.loop = false;
    }

    /**
     * 获取序列名称
     * @return 序列名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取序列条目列表
     * @return 序列条目列表
     */
    public List<SequenceEntry> getEntries() {
        return entries;
    }

    /**
     * 添加一个序列条目
     * @param presetName 预设名称
     * @param duration 持续时间（秒）
     */
    public void addEntry(String presetName, double duration) {
        entries.add(new SequenceEntry(presetName, duration));
    }

    /**
     * 检查序列是否为循环模式
     * @return 是否为循环模式
     */
    public boolean isLoop() {
        return loop;
    }

    /**
     * 设置序列是否为循环模式
     * @param loop 是否为循环模式
     */
    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    /**
     * 序列条目类，表示序列中的一个预设及其持续时间
     */
    public static class SequenceEntry {
        private final String presetName;
        private final double duration;

        /**
         * 创建一个新的序列条目
         * @param presetName 预设名称
         * @param duration 持续时间（秒）
         */
        public SequenceEntry(String presetName, double duration) {
            this.presetName = presetName;
            this.duration = duration;
        }

        /**
         * 获取预设名称
         * @return 预设名称
         */
        public String getPresetName() {
            return presetName;
        }

        /**
         * 获取持续时间
         * @return 持续时间（秒）
         */
        public double getDuration() {
            return duration;
        }
    }
}