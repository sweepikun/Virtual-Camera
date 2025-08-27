# Virtual-Camera

Virtual-Camera 是一个功能强大的 Minecraft 插件，为服务器管理员和内容创作者提供专业级的虚拟相机系统。通过该插件，您可以创建电影级的场景体验，实现流畅的运镜效果和丰富的视觉展示。

## 功能特点

- **多相机预设管理**：创建、保存和管理多个相机位置预设
- **平滑运镜效果**：支持在多个关键帧之间实现平滑过渡动画
- **多种过渡效果**：支持线性、缓入缓出、弹性、弹跳等多种过渡效果
- **自定义路径动画**：配置复杂的相机移动路径
- **文本显示系统**：在运镜过程中显示自定义文本（动作栏、标题等）
- **命令调度**：在指定时间点自动执行命令
- **序列播放**：创建和播放相机序列，支持循环播放
- **随机切换**：随机切换不同的相机预设
- **权限控制**：完整的权限系统，确保安全使用
- **预设分文件存储**：每个预设保存为单独的.yml文件，便于管理和分享

## 安装方法

1. 下载插件 JAR 文件
2. 将 JAR 文件放入服务器的 `plugins` 文件夹
3. 启动或重启服务器
4. 插件会自动生成配置文件 `config.yml` 和 `presets` 文件夹

## 使用方法

### 基本命令

- `/vcam enter` - 进入相机模式
- `/vcam exit` - 退出相机模式
- `/vcam save <名称>` - 保存当前相机位置为预设
- `/vcam load <名称>` - 加载并播放预设
- `/vcam delete <名称>` - 删除预设
- `/vcam list` - 列出所有预设和序列
- `/vcam play <序列>` - 播放相机序列
- `/vcam stop` - 停止序列播放
- `/vcam create <名称>` - 创建多点相机预设
- `/vcam addpoint [运行时间]` - 添加当前位置(包括角度)为路径点
- `/vcam finish` - 完成预设创建
- `/vcam segment <索引> <类型> <时间> <预设>` - 设置段落过渡效果
- `/vcam random start <间隔秒>` - 开始随机切换预设
- `/vcam random stop` - 停止随机切换预设
- `/vcam random add <预设名>` - 添加预设到随机切换池
- `/vcam random remove <预设名>` - 从随机切换池移除预设
- `/vcam random list` - 列出随机切换池中的预设
- `/vcam help` - 显示帮助信息

也可以使用别名命令：
- `/camera` - 与 `/vcam` 功能相同
- `/cam` - 与 `/vcam` 功能相同

### 权限节点

- `virtualcamera.use` - 基本使用权限
- `virtualcamera.preset.save` - 保存预设权限
- `virtualcamera.preset.load` - 加载预设权限
- `virtualcamera.preset.delete` - 删除预设权限
- `virtualcamera.preset.list` - 列出预设权限
- `virtualcamera.preset.create` - 创建预设权限
- `virtualcamera.preset.edit` - 编辑预设权限
- `virtualcamera.sequence.play` - 播放序列权限
- `virtualcamera.sequence.stop` - 停止序列权限
- `virtualcamera.random.start` - 开始随机切换权限
- `virtualcamera.random.stop` - 停止随机切换权限
- `virtualcamera.random.add` - 添加到随机池权限
- `virtualcamera.random.remove` - 从随机池移除权限
- `virtualcamera.random.list` - 查看随机池权限

## 配置说明

插件的配置文件位于 `plugins/Virtual-Camera/config.yml`：

每个预设现在保存在独立的文件中，位于 `plugins/Virtual-Camera/presets/` 文件夹下，文件名与预设名称相同，扩展名为 `.yml`。

示例预设文件 `plugins/Virtual-Camera/presets/demo_path.yml`：

```yaml
# 全局设置
settings:
  # 默认相机类型 (NORMAL/SPECTATOR/CINEMATIC)
  default_camera_type: NORMAL
  
  # 默认过渡效果
  default_transition:
    # 过渡类型 (TELEPORT/SMOOTH/FADE)
    type: SMOOTH
    # 过渡时间(毫秒)
    duration: 1000
  
  # 文本显示设置
  text_display:
    # 默认文本持续时间(毫秒)
    default_duration: 3000
    # 文本颜色代码
    default_color: "&f"
    # 文本位置 (ACTION_BAR/TITLE/SUBTITLE/CHAT)
    position: ACTION_BAR
  
  # 权限设置
  permissions:
    # 是否检查权限
    check_permissions: true
    # 是否在没有权限时发送消息
    send_no_permission_message: true

# 虚拟相机预设配置
presets:
  # 示例预设1 - 简单路径
  demo_path:
    type: NORMAL  # 相机类型(NORMAL/SPECTATOR/CINEMATIC)
    locations:
      - {x: 10, y: 64, z: 20, yaw: 0, pitch: 0}
      - {x: 15, y: 65, z: 25, yaw: 90, pitch: -10}
      - {x: 20, y: 66, z: 30, yaw: 180, pitch: -20}
    
    # 命令调度(可选)
    commands:
      - {command: "say 相机到达第一个点", delay: 0}
      - {command: "effect give @a minecraft:glowing 10 1", delay: 1000}
    
    # 文本显示(可选)
    texts:
      - {text: "&a正在移动中...", delay: 500, duration: 2000}
      - {text: "&b即将到达终点", delay: 2000, duration: 3000}

  # 示例预设2 - 简单单点
  demo_single:
    type: SPECTATOR
    locations:
      - {x: 100, y: 80, z: 100, yaw: 45, pitch: -30}

# 相机序列配置
sequences:
  # 示例序列1 - 简单演示
  demo_sequence:
    # 是否循环播放
    loop: false
    # 序列条目
    entries:
      - {preset: "demo_path", duration: 5.0}
      - {preset: "demo_single", duration: 3.0}
      - {preset: "demo_path", duration: 4.0}
  
  # 示例序列2 - 循环演示
  loop_sequence:
    # 是否循环播放
    loop: true
    # 序列条目
    entries:
      - {preset: "demo_single", duration: 2.0}
      - {preset: "demo_path", duration: 3.0}
```

### 配置项说明

#### 全局设置
- `default_camera_type`: 默认相机类型
  - `NORMAL`: 普通模式
  - `SPECTATOR`: 旁观者模式
  - `CINEMATIC`: 电影模式
- `default_transition`: 默认过渡效果设置
- `text_display`: 文本显示相关设置
- `permissions`: 权限检查设置

#### 预设配置
- 预设现在保存在 `plugins/Virtual-Camera/presets/` 文件夹中
- 每个 `.yml` 文件包含一个预设的完整配置
- 文件名（不含扩展名）即为预设名称
- 每个预设包含:
  - `type`: 相机类型
  - `locations`: 位置点列表（支持多个关键帧）
  - `commands`: 命令调度列表（可选）
  - `texts`: 文本显示列表（可选）

#### 序列配置
- `sequences`: 所有序列的根节点
- 每个序列包含:
  - `loop`: 是否循环播放
  - `entries`: 序列条目列表，每个条目指定预设名称和持续时间

## 使用示例

### 创建简单的路径动画

1. 进入相机模式: `/vcam enter`
2. 移动到起始位置并保存: `/vcam save start`
3. 移动到结束位置并保存: `/vcam save end`
4. 编辑 `plugins/Virtual-Camera/presets/start.yml` 文件，将两个位置添加到同一个预设中形成路径
5. 播放预设查看运镜效果: `/vcam load start`

### 创建多点相机预设（新功能）

1. 开始创建预设: `/vcam create my_preset`
2. 移动到起始位置（包括调整视角角度）并添加第一个点: `/vcam addpoint`
3. 移动到第二个位置并添加点（指定到下一点的运行时间）: `/vcam addpoint 2.5`
4. 继续添加更多点（每个点都会记录位置和视角角度）...
5. 完成预设创建: `/vcam finish`

预设将自动保存为 `plugins/Virtual-Camera/presets/my_preset.yml` 文件。

### 设置段落过渡效果

创建预设后，可以为每个段落设置不同的过渡效果和持续时间：

```
/vcam segment 0 ease_in_out 3.0 my_preset
/vcam segment 1 bounce 2.5 my_preset
```

这将为预设 `my_preset` 的第一个段落设置缓入缓出效果，持续3秒；为第二个段落设置弹跳效果，持续2.5秒。

### 创建文本显示效果

在配置文件的预设中添加texts部分:

```yaml
texts:
  - {text: "&a欢迎观看", delay: 0, duration: 3000}
  - {text: "&b精彩即将开始", delay: 3000, duration: 2000}
```

### 创建自动命令执行

在配置文件的预设中添加commands部分:

```yaml
commands:
  - {command: "say 相机启动", delay: 0}
  - {command: "effect give @a minecraft:glowing 10 1", delay: 1000}
```

## 构建项目

### 使用 Gradle 构建

```bash
./gradlew build
```

生成的 JAR 文件位于 `build/libs/` 目录中。

### 使用 Maven 构建

```bash
mvn package
```

生成的 JAR 文件位于 `target/` 目录中。

## 技术架构

### 核心组件

- **CameraManager**: 相机管理器，处理核心逻辑
- **SessionManager**: 会话管理器，管理玩家状态
- **CameraSession**: 相机会话，保存单个玩家的相机状态
- **CameraPreset**: 相机预设，保存相机位置和相关配置
- **CameraSequence**: 相机序列，管理预设播放顺序
- **PathInterpolator**: 路径插值器，实现平滑过渡算法
- **Timeline**: 时间轴管理器，统一管理时间相关操作

### 设计模式

- **命令模式**: 处理 `/vcam` 命令的执行
- **观察者模式**: 使用监听器响应游戏事件
- **管理器模式**: 通过管理器类集中处理功能
- **策略模式**: 支持不同的相机类型和过渡效果

## 故障排除

### 常见问题

1. **插件无法加载**: 检查服务器版本是否兼容
2. **权限问题**: 确保为用户分配了适当的权限节点
3. **配置不生效**: 检查配置文件格式是否正确，重启服务器使配置生效
4. **预设无法加载**: 检查 `presets` 文件夹中的 `.yml` 文件格式是否正确

### 日志查看

插件会在服务器日志中输出相关信息，可以通过日志排查问题。

## 贡献

欢迎提交 Issue 和 Pull Request 来改进这个插件。

## 许可证

[请在此处添加许可证信息]