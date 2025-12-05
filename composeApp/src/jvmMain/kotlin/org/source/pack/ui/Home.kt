package org.source.pack.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.*
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.source.pack.core.utils.SourcePacker
import java.awt.FileDialog
import java.io.File
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileSystemView // 引入 FileSystemView

@Composable
fun Home() {
    // --- 状态管理 ---
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // 输入/输出状态
    var sourceType by remember { mutableStateOf(SourceType.Local) }
    var inputPath by remember { mutableStateOf("") }
    var outputPath by remember { mutableStateOf("") }

    // 配置状态
    var selectedFormat by remember { mutableStateOf(SourcePacker.Format.MARKDOWN) }
    var selectedMode by remember { mutableStateOf(SourcePacker.Mode.FULL) }

    var isCompress by remember { mutableStateOf(false) }
    var ignoreGit by remember { mutableStateOf(true) }
    var ignoreBuild by remember { mutableStateOf(true) }
    var ignoreGradle by remember { mutableStateOf(true) }

    var userIgnoreFiles by remember { mutableStateOf("") }
    var userIgnoreExts by remember { mutableStateOf("log, tmp") }

    // UI 展开状态
    var isSourceExpanded by remember { mutableStateOf(true) }
    var isOutputExpanded by remember { mutableStateOf(true) }
    var isAdvancedOptionsExpanded by remember { mutableStateOf(false) }

    // 运行时状态
    var isProcessing by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<Status?>(null) }

    // --- 逻辑联动 ---
    LaunchedEffect(inputPath, selectedFormat, sourceType) {
        if (inputPath.isNotBlank()) {
            val name = if (sourceType == SourceType.Local) {
                File(inputPath).name
            } else {
                inputPath.trim().removeSuffix("/").removeSuffix(".git")
                    .substringAfterLast("/").takeIf { !it.contains("://") } ?: ""
            }

            if (name.isNotBlank()) {
                val ext = if (selectedFormat == SourcePacker.Format.XML) ".xml" else ".md"
                val newFileName = "$name$ext"

                val currentOutFile = if (outputPath.isNotBlank()) File(outputPath) else null
                val keepCurrentDir = currentOutFile?.parentFile?.exists() == true

                val parentDir = if (keepCurrentDir) {
                    currentOutFile.parent
                } else if (sourceType == SourceType.Local && File(inputPath).exists()) {
                    File(inputPath).parent
                } else {
                    null
                }

                outputPath = if (parentDir != null) {
                    File(parentDir, newFileName).absolutePath
                } else {
                    newFileName
                }
            }
        }
    }

    // --- 界面布局 ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "SourcePack",
                style = FluentTheme.typography.titleLarge
            )
            Text(
                text = "将项目源码打包成单个文件，便于分析或提交给 AI 模型。",
                style = FluentTheme.typography.body,
                color = FluentTheme.colors.text.text.secondary
            )
        }

        // 1. 输入源设置
        Expander(
            expanded = isSourceExpanded,
            onExpandedChanged = { isSourceExpanded = it },
            heading = { Text("输入源") },
            icon = { Icon(Icons.Regular.FolderOpen, null) },
            caption = { Text("选择本地项目或 GitHub 仓库") },
            expandContent = {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        RadioButton(
                            selected = sourceType == SourceType.Local,
                            onClick = { sourceType = SourceType.Local },
                            label = "本地文件夹"
                        )
                        RadioButton(
                            selected = sourceType == SourceType.GitHub,
                            onClick = { sourceType = SourceType.GitHub },
                            label = "GitHub 仓库"
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = inputPath,
                            onValueChange = { inputPath = it },
                            placeholder = { Text(if (sourceType == SourceType.Local) "例如: C:\\Projects\\MySource" else "例如: https://github.com/user/repo") },
                            modifier = Modifier.weight(1f),
                            header = { Text("项目路径 / URL") },
                            trailing = null
                        )

                        if (sourceType == SourceType.Local) {
                            Button(
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        // 【关键修改】使用 FileSystemView 获取真正的桌面路径
                                        // 这可以解决 OneDrive 同步导致桌面路径变更的问题
                                        val fsView = FileSystemView.getFileSystemView()
                                        val desktop = fsView.homeDirectory // 在 Windows 上这指向桌面

                                        val currentFile = if (inputPath.isNotBlank()) File(inputPath) else null

                                        // 优先级：当前已选路径 > 系统获取的桌面 > 用户主目录
                                        val initialDir = when {
                                            currentFile?.exists() == true -> currentFile
                                            desktop.exists() -> desktop
                                            else -> File(System.getProperty("user.home"))
                                        }

                                        val file = FilePickerUtil.chooseDirectory(initialDir)
                                        if (file != null) {
                                            inputPath = file.absolutePath
                                        }
                                    }
                                },
                                modifier = Modifier.align(Alignment.Bottom)
                            ) {
                                Icon(Icons.Regular.FolderOpen, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("浏览")
                            }
                        }
                    }
                }
            }
        )

        // 2. 输出设置
        Expander(
            expanded = isOutputExpanded,
            onExpandedChanged = { isOutputExpanded = it },
            heading = { Text("输出目标") },
            icon = { Icon(Icons.Regular.Save, null) },
            caption = { Text("设置生成文件的保存位置") },
            expandContent = {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = outputPath,
                            onValueChange = { outputPath = it },
                            placeholder = { Text("例如: D:\\Output\\Source.md") },
                            modifier = Modifier.weight(1f),
                            header = { Text("保存路径") },
                            trailing = null
                        )
                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    val name = if (inputPath.isNotBlank()) {
                                        if (sourceType == SourceType.Local) File(inputPath).name
                                        else inputPath.trim().removeSuffix("/").removeSuffix(".git").substringAfterLast("/").takeIf { !it.contains("://") } ?: ""
                                    } else ""

                                    val safeName = if (name.isBlank()) "SourcePack_Output" else name
                                    val defaultName = safeName + if(selectedFormat == SourcePacker.Format.XML) ".xml" else ".md"

                                    // 保存弹窗也默认尝试定位到桌面（如果没有其他更好的位置）
                                    val fsView = FileSystemView.getFileSystemView()
                                    val desktop = fsView.homeDirectory

                                    // 优先级：当前输入源的父目录 > 桌面 > 用户主目录
                                    val initialDir = if (inputPath.isNotBlank() && sourceType == SourceType.Local) {
                                        File(inputPath).parentFile?.takeIf { it.exists() }
                                    } else {
                                        desktop
                                    }

                                    val file = FilePickerUtil.saveFile(defaultName, initialDir)
                                    if (file != null) {
                                        outputPath = file.absolutePath
                                    }
                                }
                            },
                            modifier = Modifier.align(Alignment.Bottom)
                        ) {
                            Icon(Icons.Regular.Save, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("选择")
                        }
                    }
                }
            }
        )

        // 3. 高级配置
        Expander(
            expanded = isAdvancedOptionsExpanded,
            onExpandedChanged = { isAdvancedOptionsExpanded = it },
            heading = { Text("高级配置") },
            icon = { Icon(Icons.Regular.Settings, null) },
            caption = { Text("格式、过滤规则与压缩选项") },
            expandContent = {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("输出格式", style = FluentTheme.typography.bodyStrong)
                            MenuFlyoutContainer(
                                flyout = {
                                    SourcePacker.Format.entries.forEach { fmt ->
                                        MenuFlyoutItem(
                                            text = { Text(fmt.name) },
                                            selected = selectedFormat == fmt,
                                            onSelectedChanged = {
                                                selectedFormat = fmt
                                                isFlyoutVisible = false
                                            },
                                            selectionType = ListItemSelectionType.Radio
                                        )
                                    }
                                },
                                content = {
                                    DropDownButton(
                                        onClick = { isFlyoutVisible = true },
                                        content = { Text(selectedFormat.name) }
                                    )
                                }
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("输出内容模式", style = FluentTheme.typography.bodyStrong)
                            MenuFlyoutContainer(
                                flyout = {
                                    SourcePacker.Mode.entries.forEach { m ->
                                        MenuFlyoutItem(
                                            text = { Text(m.name) },
                                            selected = selectedMode == m,
                                            onSelectedChanged = {
                                                selectedMode = m
                                                isFlyoutVisible = false
                                            },
                                            selectionType = ListItemSelectionType.Radio
                                        )
                                    }
                                },
                                content = {
                                    DropDownButton(
                                        onClick = { isFlyoutVisible = true },
                                        content = { Text(selectedMode.name) }
                                    )
                                }
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("过滤与优化", style = FluentTheme.typography.bodyStrong)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            CheckBox(checked = isCompress, onCheckStateChange = { isCompress = it }, label = "压缩内容")
                            CheckBox(checked = ignoreGit, onCheckStateChange = { ignoreGit = it }, label = "忽略 .git")
                            CheckBox(checked = ignoreBuild, onCheckStateChange = { ignoreBuild = it }, label = "忽略 build")
                            CheckBox(checked = ignoreGradle, onCheckStateChange = { ignoreGradle = it }, label = "忽略 Gradle")
                        }
                    }

                    TextField(
                        value = userIgnoreFiles,
                        onValueChange = { userIgnoreFiles = it },
                        header = { Text("忽略文件名 (逗号分隔)") },
                        placeholder = { Text("例如: secret.key, local.properties") },
                        modifier = Modifier.fillMaxWidth(),
                        trailing = null
                    )
                    TextField(
                        value = userIgnoreExts,
                        onValueChange = { userIgnoreExts = it },
                        header = { Text("忽略后缀名 (逗号分隔)") },
                        placeholder = { Text("例如: log, tmp, cache") },
                        modifier = Modifier.fillMaxWidth(),
                        trailing = null
                    )
                }
            }
        )

        // 4. 执行区域
        Spacer(Modifier.height(8.dp))

        statusMessage?.let { status ->
            InfoBar(
                title = { Text(status.title) },
                message = { Text(status.message) },
                severity = status.severity,
                closeAction = { InfoBarDefaults.CloseActionButton(onClick = { statusMessage = null }) }
            )
        }

        if (isProcessing) {
            ProgressBar(modifier = Modifier.fillMaxWidth())
            Text(
                text = progressText,
                style = FluentTheme.typography.caption,
                color = FluentTheme.colors.text.text.secondary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            AccentButton(
                onClick = {
                    if (inputPath.isBlank() || outputPath.isBlank()) {
                        statusMessage = Status("错误", "请填写完整的输入和输出路径", InfoBarSeverity.Critical)
                        return@AccentButton
                    }
                    isProcessing = true
                    statusMessage = null
                    scope.launch(Dispatchers.IO) {
                        try {
                            val config = SourcePacker.Config(
                                compress = isCompress,
                                ignoreGit = ignoreGit,
                                ignoreBuild = ignoreBuild,
                                ignoreGradle = ignoreGradle,
                                format = selectedFormat,
                                mode = selectedMode,
                                userIgnoreFiles = userIgnoreFiles.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
                                userIgnoreExts = userIgnoreExts.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                            )
                            val destFile = File(outputPath)

                            if (sourceType == SourceType.Local) {
                                val sourceFile = File(inputPath)
                                SourcePacker().packLocal(sourceFile, destFile, config) { progressText = "正在处理: $it" }
                            } else {
                                SourcePacker().packGitHub(inputPath, destFile, config) { progressText = it }
                            }
                            statusMessage = Status("成功", "打包完成！文件已保存至: $outputPath", InfoBarSeverity.Success)
                        } catch (e: Exception) {
                            statusMessage = Status("失败", e.message ?: "未知错误", InfoBarSeverity.Critical)
                            e.printStackTrace()
                        } finally {
                            isProcessing = false
                            progressText = ""
                        }
                    }
                },
                disabled = isProcessing,
                modifier = Modifier.defaultMinSize(minWidth = 120.dp)
            ) {
                if (isProcessing) {
                    ProgressRing(size = ProgressRingSize.Small, color = FluentTheme.colors.text.onAccent.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("处理中...")
                } else {
                    Icon(Icons.Regular.Play, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("开始打包")
                }
            }
        }
    }
}

// --- 辅助数据类 ---
enum class SourceType { Local, GitHub }
data class Status(
    val title: String,
    val message: String,
    val severity: InfoBarSeverity
)

// --- 文件选择工具类 (AWT/Swing) ---
object FilePickerUtil {
    fun chooseDirectory(initialDirectory: File? = null): File? {
        if (System.getProperty("os.name").lowercase().contains("mac")) {
            val dialog = FileDialog(null as java.awt.Frame?, "选择文件夹", FileDialog.LOAD)
            System.setProperty("apple.awt.fileDialogForDirectories", "true")
            if (initialDirectory != null && initialDirectory.exists()) {
                dialog.directory = initialDirectory.absolutePath
            }
            dialog.isVisible = true
            System.setProperty("apple.awt.fileDialogForDirectories", "false")
            return if (dialog.directory != null && dialog.file != null) {
                File(dialog.directory, dialog.file)
            } else null
        }
        return chooseDirectorySwing(initialDirectory)
    }

    private fun chooseDirectorySwing(initialDirectory: File? = null): File? {
        val openChooser = {
            val chooser = javax.swing.JFileChooser()
            chooser.fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
            try {
                // 使用传入的 initialDirectory，如果为 null，则回退到用户主目录
                // 注意：这里移除了 FileSystemView 逻辑，因为 Home.kt 已经传进来了
                chooser.currentDirectory = initialDirectory?.takeIf { it.exists() } ?: File(System.getProperty("user.home"))
            } catch (_: Exception) {}

            val returnVal = chooser.showOpenDialog(null)
            if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION) {
                chooser.selectedFile
            } else {
                null
            }
        }
        return if (SwingUtilities.isEventDispatchThread()) {
            openChooser()
        } else {
            var result: File? = null
            SwingUtilities.invokeAndWait { result = openChooser() }
            result
        }
    }

    fun saveFile(defaultName: String, initialDirectory: File? = null): File? {
        val dialog = FileDialog(null as java.awt.Frame?, "保存文件", FileDialog.SAVE)
        dialog.file = defaultName
        if (initialDirectory != null && initialDirectory.exists()) {
            dialog.directory = initialDirectory.absolutePath
        }
        dialog.isVisible = true
        return if (dialog.directory != null && dialog.file != null) {
            File(dialog.directory, dialog.file)
        } else null
    }
}