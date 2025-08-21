package cn.popcraft.model;

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
     * 是否循环播放
     * @return 是否循环播放
     */
    public boolean isLoop() {
        return loop;
    }

    /**
     * 设置是否循环播放
     * @param loop 是否循环播放
     */
    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    /**
     * 添加序列条目
     * @param presetName 预设名称
     * @param duration 持续时间（秒）
     */
    public void addEntry(String presetName, double duration) {
        entries.add(new SequenceEntry(presetName, duration));
    }

    /**
     * 移除序列条目
     * @param index 条目索引
     */
    public void removeEntry(int index) {
        if (index >= 0 && index < entries.size()) {
            entries.remove(index);
        }
    }

    /**
     * 清空序列条目
     */
    public void clearEntries() {
        entries.clear();
    }

    /**
     * 序列条目类，表示序列中的一个预设及其播放时间
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

    /**
     * 创建序列的副本
     * @return 序列副本
     */
    public CameraSequence clone() {
        CameraSequence clone = new CameraSequence(this.name);
        clone.setLoop(this.loop);
        for (SequenceEntry entry : this.entries) {
            clone.addEntry(entry.getPresetName(), entry.getDuration());
        }
        return clone;
    }

    /**
     * 获取序列的总持续时间
     * @return 总持续时间（秒）
     */
    public double getTotalDuration() {
        return entries.stream()
                .mapToDouble(SequenceEntry::getDuration)
                .sum();
    }

    /**
     * 获取指定索引的序列条目
     * @param index 条目索引
     * @return 序列条目，如果索引无效则返回null
     */
    public SequenceEntry getEntry(int index) {
        if (index >= 0 && index < entries.size()) {
            return entries.get(index);
        }
        return null;
    }

    /**
     * 获取序列条目数量
     * @return 条目数量
     */
    public int getEntryCount() {
        return entries.size();
    }
    
    /**
     * 根据预设名称移除序列条目
     * @param presetName 预设名称
     */
    public void removeEntriesByPresetName(String presetName) {
        entries.removeIf(entry -> entry.getPresetName().equals(presetName));
    }

    /**
     * 交换两个序列条目的位置
     * @param index1 第一个条目的索引
     * @param index2 第二个条目的索引
     * @return 是否交换成功
     */
    public boolean swapEntries(int index1, int index2) {
        if (index1 >= 0 && index1 < entries.size() && 
            index2 >= 0 && index2 < entries.size() && 
            index1 != index2) {
            SequenceEntry entry1 = entries.get(index1);
            entries.set(index1, entries.get(index2));
            entries.set(index2, entry1);
            return true;
        }
        return false;
    }
}