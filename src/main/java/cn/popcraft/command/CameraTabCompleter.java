package cn.popcraft.command;

import cn.popcraft.manager.CameraManager;
import cn.popcraft.model.CameraPreset;
import cn.popcraft.model.CameraSequence;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 相机命令Tab补全器
 */
public class CameraTabCompleter implements TabCompleter {
    private final CameraManager cameraManager;
    private final List<String> COMMANDS = Arrays.asList(
        "enter", "exit", "save", "load", "delete", "list", "play", "stop", "help"
    );

    public CameraTabCompleter(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 补全主命令
            StringUtil.copyPartialMatches(args[0], COMMANDS, completions);
        } else if (args.length == 2) {
            // 补全子命令参数
            switch (args[0].toLowerCase()) {
                case "load":
                case "delete":
                    // 补全预设名称
                    if (sender.hasPermission("virtualcamera.preset.load") || 
                        sender.hasPermission("virtualcamera.preset.delete")) {
                        Map<String, CameraPreset> presets = cameraManager.getAllPresets();
                        List<String> presetNames = new ArrayList<>(presets.keySet());
                        StringUtil.copyPartialMatches(args[1], presetNames, completions);
                    }
                    break;

                case "play":
                    // 补全序列名称
                    if (sender.hasPermission("virtualcamera.sequence.play")) {
                        Map<String, CameraSequence> sequences = cameraManager.getAllSequences();
                        List<String> sequenceNames = new ArrayList<>(sequences.keySet());
                        StringUtil.copyPartialMatches(args[1], sequenceNames, completions);
                    }
                    break;

                case "save":
                    // 对于save命令，提供一些建议的预设名称格式
                    if (sender.hasPermission("virtualcamera.preset.save")) {
                        List<String> suggestions = Arrays.asList(
                            "preset_" + (cameraManager.getAllPresets().size() + 1),
                            "camera_position_" + (cameraManager.getAllPresets().size() + 1),
                            "scene_" + (cameraManager.getAllPresets().size() + 1)
                        );
                        StringUtil.copyPartialMatches(args[1], suggestions, completions);
                    }
                    break;
            }
        }

        // 按字母顺序排序补全结果
        Collections.sort(completions);
        return completions;
    }

    /**
     * 过滤已存在的名称，避免重复
     * @param suggestions 建议列表
     * @param existingNames 已存在的名称
     * @return 过滤后的建议列表
     */
    private List<String> filterExistingNames(List<String> suggestions, List<String> existingNames) {
        return suggestions.stream()
                .filter(name -> !existingNames.contains(name))
                .collect(Collectors.toList());
    }
}