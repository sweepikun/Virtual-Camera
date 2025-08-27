package cn.popcraft.command;

import cn.popcraft.manager.CameraManager;
import cn.popcraft.model.CameraPreset;
import cn.popcraft.model.CameraSequence;
import cn.popcraft.model.TransitionType;
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
    private final List<String> MAIN_COMMANDS = Arrays.asList(
        "enter", "exit", "save", "load", "delete", "list", "play", "stop", "help", "create", "addpoint", "finish", "segment", "random"
    );
    
    private final List<String> RANDOM_SUBCOMMANDS = Arrays.asList(
        "start", "stop", "add", "remove", "list"
    );

    public CameraTabCompleter(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 补全主命令
            StringUtil.copyPartialMatches(args[0], MAIN_COMMANDS, completions);
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
                case "create":
                    // 对于save和create命令，提供一些建议的预设名称格式
                    if (sender.hasPermission("virtualcamera.preset.save") || 
                        sender.hasPermission("virtualcamera.preset.create")) {
                        List<String> suggestions = Arrays.asList(
                            "preset_" + (cameraManager.getAllPresets().size() + 1),
                            "camera_position_" + (cameraManager.getAllPresets().size() + 1),
                            "scene_" + (cameraManager.getAllPresets().size() + 1)
                        );
                        StringUtil.copyPartialMatches(args[1], suggestions, completions);
                    }
                    break;
                    
                case "segment":
                    // 补全预设名称
                    if (sender.hasPermission("virtualcamera.preset.edit")) {
                        Map<String, CameraPreset> presets = cameraManager.getAllPresets();
                        List<String> presetNames = new ArrayList<>(presets.keySet());
                        StringUtil.copyPartialMatches(args[1], presetNames, completions);
                    }
                    break;
                    
                case "random":
                    // 补全random子命令
                    if (sender.hasPermission("virtualcamera.random.start") ||
                        sender.hasPermission("virtualcamera.random.stop") ||
                        sender.hasPermission("virtualcamera.random.add") ||
                        sender.hasPermission("virtualcamera.random.remove") ||
                        sender.hasPermission("virtualcamera.random.list")) {
                        StringUtil.copyPartialMatches(args[1], RANDOM_SUBCOMMANDS, completions);
                    }
                    break;
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "segment":
                    // 补全段落索引（如果可能的话）
                    if (sender.hasPermission("virtualcamera.preset.edit")) {
                        String presetName = args[1];
                        CameraPreset preset = cameraManager.getAllPresets().get(presetName);
                        if (preset != null) {
                            // 添加段落索引建议
                            for (int i = 0; i < Math.max(1, preset.getLocations().size() - 1); i++) {
                                completions.add(String.valueOf(i));
                            }
                        } else {
                            // 如果预设不存在，添加一些通用数字
                            completions.addAll(Arrays.asList("0", "1", "2", "3", "4", "5"));
                        }
                        StringUtil.copyPartialMatches(args[2], completions, new ArrayList<>());
                        completions.clear();
                        StringUtil.copyPartialMatches(args[2], Arrays.asList("0", "1", "2", "3", "4", "5"), completions);
                    }
                    break;
                    
                case "random":
                    switch (args[1].toLowerCase()) {
                        case "add":
                        case "remove":
                            // 补全预设名称
                            if (sender.hasPermission("virtualcamera.random.add") ||
                                sender.hasPermission("virtualcamera.random.remove")) {
                                Map<String, CameraPreset> presets = cameraManager.getAllPresets();
                                List<String> presetNames = new ArrayList<>(presets.keySet());
                                StringUtil.copyPartialMatches(args[2], presetNames, completions);
                            }
                            break;
                    }
                    break;
            }
        } else if (args.length == 4) {
            switch (args[0].toLowerCase()) {
                case "segment":
                    // 补全过渡类型
                    if (sender.hasPermission("virtualcamera.preset.edit")) {
                        List<String> transitionTypes = new ArrayList<>();
                        for (TransitionType type : TransitionType.values()) {
                            transitionTypes.add(type.name());
                        }
                        StringUtil.copyPartialMatches(args[3], transitionTypes, completions);
                    }
                    break;
            }
        } else if (args.length == 5) {
            switch (args[0].toLowerCase()) {
                case "segment":
                    // 补全持续时间（秒）
                    if (sender.hasPermission("virtualcamera.preset.edit")) {
                        completions.addAll(Arrays.asList("1.0", "2.0", "3.0", "5.0", "10.0"));
                        // 保留原有逻辑，但使用新的completions列表
                        List<String> temp = new ArrayList<>();
                        StringUtil.copyPartialMatches(args[4], completions, temp);
                        completions.clear();
                        completions.addAll(temp);
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