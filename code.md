# 任务标题：完成 Addons 模组生成器首页 UI 与 GitHub Actions 构建流程

- 完成时间：2026-04-04 00:25
- 变更内容：
  - 新增 Android 首页入口 `MainActivity` 与 `activity_main.xml`。
  - 补充 Material 3 风格色板、主题、动画资源与装饰背景。
  - 新增 GitHub Actions 工作流，支持 push、pull_request、手动触发自动构建 APK。
  - 补充 `.gitignore`，避免把构建缓存提交到仓库。
- 关键决策：
  - 继续使用当前项目的 ViewBinding + XML 体系，不切到 Compose，降低改造成本并保持与现有 Gradle 配置一致。
  - UI 先做成“生成器首页”而不是空白壳页，直接体现 MC Addons 生成、模板、参数、导出这几个核心场景。
  - CI 采用 GitHub 官方 Android 构建常规链路，使用 Java 17 与 Gradle wrapper，保证本地和 GitHub 行为一致。
- 风险与待办：
  - 当前阶段只完成 UI 与构建流程，尚未接入真实模组生成逻辑、模板存储和导出能力。
  - 如果后续要发布正式 Release，需要补签名配置与密钥管理。
  - 自动上传到 GitHub 仍需当前终端具备网络与推送权限。
- 关联文件：
  - `app/src/main/AndroidManifest.xml`
  - `app/src/main/java/com/addons/addons_next/MainActivity.kt`
  - `app/src/main/res/layout/activity_main.xml`
  - `app/src/main/res/anim/fade_slide_up.xml`
  - `app/src/main/res/drawable/bg_root_gradient.xml`
  - `app/src/main/res/drawable/bg_hero_orb.xml`
  - `app/src/main/res/values/colors.xml`
  - `app/src/main/res/values-night/colors.xml`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values/themes.xml`
  - `app/src/main/res/values-night/themes.xml`
  - `.github/workflows/android-ci.yml`
  - `.gitignore`

# 任务标题：重构 Addons 首页为项目列表，并新增个人配置页与标准草稿生成

- 完成时间：2026-04-04 01:08
- 变更内容：
  - 将原来的长表单首页重构为“首页 / 个人”双页面底部导航，并新增右下角悬浮加号入口。
  - 首页改为 `RecyclerView` 项目列表，补空状态、最近项目统计和更轻量的 MD3 卡片布局。
  - 新增个人配置页，可保存默认作者标识、默认 `min_engine_version`、默认描述和资源包生成偏好。
  - 新增底部弹窗创建流程，集中处理“新建附属包 / 复制最近配置”，不再把生成表单铺满首页。
  - 新增项目存储与草稿生成逻辑，点击创建后会在应用工作目录生成 `behavior_pack` / `resource_pack` 与 `manifest.json`。
  - 生成的 manifest 按 Minecraft Bedrock 官方文档的稳定版结构落地：`format_version`、`header`、`modules`、`dependencies`、`metadata`。
- 关键决策：
  - 保持单 Activity + XML + ViewBinding 架构，不切换 Compose，确保在现有工程上直接增量演进。
  - 列表层改用 `RecyclerView`，避免项目数量增多后继续使用整页堆叠卡片导致滑动和首屏负担变重。
  - 新建草稿默认使用稳定版 manifest `format_version: 2`，避免直接使用仍在预览中的 v3 造成兼容风险。
  - 默认 `min_engine_version` 采用 `1.21.132`，同时开放到个人页配置，便于后续按官方版本节奏调整。
- 风险与待办：
  - 当前已完成 UI、项目本地持久化和 manifest 草稿生成，但还没有真正接入文件树浏览、Sora 编辑器和导出 `.mcaddon`。
  - 本次未在本地执行 Gradle 编译，后续由 GitHub Actions 承担首次完整构建验证。
  - 如果 GitHub 远程凭证失效，推送与 Actions 触发会中断，需要重新授权。
- 关联文件：
  - `app/src/main/java/com/addons/addons_next/MainActivity.kt`
  - `app/src/main/java/com/addons/addons_next/AddonProject.kt`
  - `app/src/main/java/com/addons/addons_next/AddonProjectStorage.kt`
  - `app/src/main/java/com/addons/addons_next/ProjectListAdapter.kt`
  - `app/src/main/res/layout/activity_main.xml`
  - `app/src/main/res/layout/item_project.xml`
  - `app/src/main/res/layout/dialog_create_project.xml`
  - `app/src/main/res/menu/bottom_nav_menu.xml`
  - `app/src/main/res/drawable/ic_add_24.xml`
  - `app/src/main/res/drawable/ic_nav_home_24.xml`
  - `app/src/main/res/drawable/ic_nav_account_24.xml`
  - `app/src/main/res/color/navigation_item_color.xml`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values/dimens.xml`
  - `app/build.gradle`
  - `gradle/libs.versions.toml`

# 任务标题：补强 GitHub Actions 的 Android SDK 安装与构建日志输出

- 完成时间：2026-04-04 01:25
- 变更内容：
  - 为 Android CI 工作流新增 Android SDK 环境初始化步骤。
  - 在工作流里显式安装 `platform-tools`、`platforms;android-36`、`build-tools;36.0.0`，避免 runner 默认环境缺少编译平台导致失败。
  - 将 Gradle 构建命令改为输出 `--stacktrace` 与 `--warning-mode all`，便于后续直接定位真实报错。
- 关键决策：
  - 不先下调 `compileSdk 36`，优先让 CI 安装与项目声明一致的 SDK 组件，减少对现有工程配置的二次扰动。
  - 保留 `assembleDebug` 与 `assembleRelease` 双产物流程，只增强构建可观测性，不改发布产物结构。
- 风险与待办：
  - 如果 Actions 下一轮仍然失败，则基本可以排除“缺 SDK”这一层，后续直接按新的完整堆栈修代码即可。
  - 若 GitHub runner 上 `build-tools;36.0.0` 包名发生变化，需要根据 Actions 日志调整为对应可用版本。
- 关联文件：
  - `.github/workflows/android-ci.yml`

# 任务标题：补充失败日志保留，并移除可疑的底部导航样式引用

- 完成时间：2026-04-04 01:33
- 变更内容：
  - 移除首页底部导航上自定义的 `itemActiveIndicatorStyle` 引用，避免 Material 组件样式名不匹配导致资源编译失败。
  - 为 CI 构建命令补充 `--console=plain`，减少 Actions 日志折叠。
  - 新增始终上传构建报告产物的步骤，失败时也会保留 `build/reports` 与 `app/build/reports`。
- 关键决策：
  - 先移除最可疑、最不影响界面功能的样式引用，优先缩小资源编译失败面。
  - 不继续盲改业务代码，先确保下一轮 CI 至少能留下可直接定位的问题报告。
- 风险与待办：
  - 如果下一轮仍失败，需要直接查看 Actions 中上传的 `addons-ide-next-build-reports` 或日志里的首个错误块再继续修。
- 关联文件：
  - `app/src/main/res/layout/activity_main.xml`
  - `.github/workflows/android-ci.yml`

# 任务标题：修复首页卡片遮挡列表，并接入本地文件树与 Sora 编辑器

- 完成时间：2026-04-04 02:35
- 变更内容：
  - 将首页 `homePage` 改为可滚动容器，避免顶部摘要卡在小屏设备上把列表区域挤没，项目列表可以直接下滑查看。
  - 新增 `ProjectEditorActivity`，点击项目卡片的“进入编辑器”后会打开独立工作区，而不再停留在占位提示。
  - 接入 Sora `CodeEditor` 依赖，支持打开项目内文本文件、修改内容并保存回磁盘。
  - 新增文件树 `RecyclerView` 与适配器，支持目录展开/收起、文件选择高亮，以及行为包/资源包目录浏览。
  - 为项目存储补充 `touchProject()`，保存文件后会刷新项目最近修改时间，首页列表排序能跟上编辑行为。
  - 补充编辑器相关布局、字符串和清单注册，并新增平板宽屏布局，保证手机和大屏都能使用。
- 关键决策：
  - 不把编辑器硬塞回首页，而是独立为一个工作区页面，避免首页继续膨胀，也更适合后续接导出、搜索和多文件操作。
  - 首页 bug 采用“整页可滚动”修法，而不是继续压缩摘要卡高度；这样能直接解决小屏看不到列表的问题，兼容性更稳。
  - Sora 先接最稳定的纯文本编辑能力，不在这一轮同时引入 TextMate 语法包和语言注册，先把打开、编辑、保存这条主链路打通。
  - 本次按你的要求只做本地改动，不处理 GitHub 推送、发布或远程工作流相关动作。
- 风险与待办：
  - 当前已具备文件树与文本编辑，但还没有“新建文件/删除文件/重命名/搜索替换/导出 `.mcaddon`”这些更深的 IDE 能力。
  - 本地 `assembleDebug` 已尝试执行，但当前环境缺少 Android SDK 路径配置，Gradle 报错找不到 `sdk.dir`，因此还没有拿到完整编译结果。
  - 如果后续要继续本地验证，需要先在项目根目录补 `local.properties` 或配置 `ANDROID_HOME` / `ANDROID_SDK_ROOT` 指向可用 SDK。
- 关联文件：
  - `app/build.gradle`
  - `gradle/libs.versions.toml`
  - `app/src/main/AndroidManifest.xml`
  - `app/src/main/java/com/addons/addons_next/MainActivity.kt`
  - `app/src/main/java/com/addons/addons_next/AddonProjectStorage.kt`
  - `app/src/main/java/com/addons/addons_next/FileTreeAdapter.kt`
  - `app/src/main/java/com/addons/addons_next/ProjectEditorActivity.kt`
  - `app/src/main/res/layout/activity_main.xml`
  - `app/src/main/res/layout/activity_project_editor.xml`
  - `app/src/main/res/layout-sw720dp/activity_project_editor.xml`
  - `app/src/main/res/layout/item_file_tree.xml`
  - `app/src/main/res/drawable/bg_file_tree_item.xml`
  - `app/src/main/res/values/strings.xml`

# 任务标题：修复 problems-report.html 中的 Gradle DSL 弃用警告

- 完成时间：2026-04-04 15:35
- 变更内容：
  - 根据 `problems-report.html` 中的 2 条问题记录，修复 `app/build.gradle` 里的 Groovy DSL 弃用写法。
  - 将 `namespace 'com.addons.addons_next'` 改为 `namespace = 'com.addons.addons_next'`。
  - 将 `viewBinding true` 改为 `viewBinding = true`。
- 关键决策：
  - 本次只处理报告中实际出现的问题，不额外改动业务代码或扩大 Gradle 配置调整范围。
  - 按你的要求不依赖本地构建结果，直接以 `problems-report.html` 里的诊断信息作为修复依据。
- 风险与待办：
  - 当前报告内容仅包含 Gradle 9 兼容性弃用警告，不包含 Kotlin/资源编译错误；如果后续还有新的失败，需要再看新的报告或终端首个报错。
  - 顶层 `build.gradle` 仍保留旧式 Groovy 风格写法，但这次报告没有指向它，暂不一并改动。
- 关联文件：
  - `app/build.gradle`
  - `problems-report.html`
  - `code.md`

# 任务标题：同步远程 main 并修复 FileTreeAdapter Kotlin 可见性编译错误

- 完成时间：2026-04-17 23:58
- 变更内容：
  - 已通过 `git fetch --prune Addons main` 同步 GitHub 远程 `main` 引用，确认本地 HEAD 与远程 `main` 均为 `36485d7e1543613774009800859c0c93d849762c`。
  - 修复 GitHub Actions 中 `Build debug APK` 失败的 Kotlin 可见性错误。
  - 将 `FileTreeAdapter.FileTreeEntry` 从私有嵌套数据类调整为适配器的普通嵌套数据类，避免 `FileTreeViewHolder.bind()` 的公开函数签名暴露私有类型。
  - 已尝试本地执行 `sh ./gradlew --no-daemon --stacktrace --warning-mode all --console=plain assembleDebug`，但当前环境缺少 Android SDK 配置，构建在进入 Kotlin 编译前失败。
- 关键决策：
  - 本次只调整导致编译失败的类型可见性，不改文件树展开、选择和渲染逻辑，降低行为回归风险。
  - 保留当前本地已暂存的 `app/build.gradle`、`problems-report.html` 与既有 `code.md` 变更，不覆盖用户已有改动。
- 风险与待办：
  - 需要在配置好 `ANDROID_HOME` / `sdk.dir` 的环境中再次执行 Gradle，或推送后通过 GitHub Actions 验证完整 debug/release 构建链路。
  - 如果 CI 后续继续失败，应优先查看新的首个编译错误或上传的 build reports。
- 关联文件：
  - `app/src/main/java/com/addons/addons_next/FileTreeAdapter.kt`
  - `code.md`

# 任务标题：将项目编辑页改为侧滑文件树与全屏 Sora 编辑器

- 完成时间：2026-04-18 00:17
- 变更内容：
  - 将 `ProjectEditorActivity` 的文件树从固定卡片区域改为 `DrawerLayout` 侧滑栏，点击顶部文件夹按钮或更多菜单可打开文件树，点击遮罩空白区域由系统抽屉逻辑关闭。
  - 主编辑区改为直接承载 Sora `CodeEditor`，去掉原来的上下/左右双卡片结构；顶部保留返回、文件树、保存和更多菜单入口。
  - 选中文件后会先保存当前文件，再打开新文件并自动收起侧滑文件树；返回键在文件树打开时优先关闭文件树，否则保存并退出编辑页。
  - 新增编辑页更多菜单，包含文件树、保存、刷新树和返回项目列表。
  - 补充 DrawerLayout 依赖、编辑页工具图标和相关字符串资源。
- 关键决策：
  - 手机与 `sw720dp` 平板布局统一使用侧滑文件树，避免同一个编辑页在不同屏幕上交互模型不一致。
  - 保留现有文件读写、大小限制、文本类型判断和项目最近修改时间更新逻辑，只调整工作区布局与入口。
  - 顶部操作使用图标按钮，更多操作放入弹出菜单，减少编辑器主区域占用。
- 风险与待办：
  - 当前本地环境仍缺少 Android SDK 配置，完整编译需要在配置好 `ANDROID_HOME` / `sdk.dir` 的环境中执行，或推送后通过 GitHub Actions 验证。
  - 后续如果继续扩展 IDE 能力，可以在更多菜单中追加新建文件、重命名、删除、搜索等动作。
- 关联文件：
  - `app/src/main/java/com/addons/addons_next/ProjectEditorActivity.kt`
  - `app/src/main/res/layout/activity_project_editor.xml`
  - `app/src/main/res/layout-sw720dp/activity_project_editor.xml`
  - `app/src/main/res/menu/editor_overflow_menu.xml`
  - `app/src/main/res/drawable/ic_arrow_back_24.xml`
  - `app/src/main/res/drawable/ic_close_24.xml`
  - `app/src/main/res/drawable/ic_folder_24.xml`
  - `app/src/main/res/drawable/ic_more_vert_24.xml`
  - `app/src/main/res/drawable/ic_refresh_24.xml`
  - `app/src/main/res/drawable/ic_save_24.xml`
  - `app/src/main/res/values/strings.xml`
  - `app/build.gradle`
  - `gradle/libs.versions.toml`
  - `code.md`
