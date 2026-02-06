package com.sourcepack.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sourcepack.data.*
import com.sourcepack.viewmodel.MainVM
import com.sourcepack.Page

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoot(onBack: () -> Unit, onNav: (Page) -> Unit, vm: MainVM) {
    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("设置") }, 
                navigationIcon = { IconButton(onClick = onBack) { Icon(Ico.ArrowBack, null) } }
            ) 
        }
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
            
            SettingHeader("常规")
            SettingLink(Ico.Settings, "通用配置", "导出路径、格式、压缩、模式") { onNav(Page.CONFIG_GEN) }
            SettingLink(Ico.Delete, "黑名单管理", "管理忽略规则 (批量)") { onNav(Page.CONFIG_BL) }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GeneralSettings(vm: MainVM, back: () -> Unit) {
    val cfg by vm.cfg.collectAsStateWithLifecycle()
    val exportDir by vm.exportDir.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // 恢复标准系统目录选择器
    val dirPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.setExportDirectory(uri)
    }

    Scaffold(topBar = { TopAppBar(title = { Text("通用配置") }, navigationIcon = { IconButton(onClick = back) { Icon(Ico.ArrowBack, null) } }) }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
            
            SettingHeader("导出位置")
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("默认保存目录", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    
                    val pathText = if (exportDir != null) {
                        // 尝试解析显示名称
                        DocumentFile.fromTreeUri(context, exportDir!!)?.uri?.path ?: "已设置 (Content Uri)"
                    } else {
                        "未设置 (每次都会询问保存位置)"
                    }
                    
                    Text(pathText, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { dirPicker.launch(null) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("选择文件夹")
                        }
                        
                        if (exportDir != null) {
                            OutlinedButton(onClick = { vm.setExportDirectory(null) }) {
                                Text("清除")
                            }
                        }
                    }
                }
            }

            SettingHeader("过滤")
            SwitchItem("使用 .gitignore", cfg.useGitIgnore) { vm.saveCfg(cfg.copy(useGitIgnore = it)) }
            SwitchItem("忽略 .gradle 文件夹", cfg.ignoreGradle) { vm.saveCfg(cfg.copy(ignoreGradle = it)) }
            SwitchItem("忽略 .git 文件夹", cfg.ignoreGit) { vm.saveCfg(cfg.copy(ignoreGit = it)) }
            SwitchItem("忽略 build 文件夹", cfg.ignoreBuild) { vm.saveCfg(cfg.copy(ignoreBuild = it)) }
            
            SettingHeader("输出内容")
            SwitchItem("压缩内容 (去换行)", cfg.compress) { vm.saveCfg(cfg.copy(compress = it)) }
            SwitchItem("去除代码注释", cfg.removeComments) { vm.saveCfg(cfg.copy(removeComments = it)) }
            
            SettingHeader("输出格式")
            FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Format.values().forEach { f ->
                    FilterChip(
                        selected = cfg.format == f,
                        onClick = { vm.saveCfg(cfg.copy(format = f)) },
                        label = { Text(f.name) }
                    )
                }
            }

            SettingHeader("输出模式")
            FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Mode.values().forEach { m ->
                    FilterChip(
                        selected = cfg.mode == m,
                        onClick = { vm.saveCfg(cfg.copy(mode = m)) },
                        label = { 
                            Text(when(m) {
                                Mode.FULL -> "完整内容"
                                Mode.TREE -> "仅目录树"
                            })
                        }
                    )
                }
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlacklistSettings(vm: MainVM, back: () -> Unit) {
    val files by vm.uFiles.collectAsStateWithLifecycle()
    val exts by vm.uExts.collectAsStateWithLifecycle()
    var type by remember { mutableIntStateOf(0) } 
    val selectedItems = remember { mutableStateListOf<String>() }
    LaunchedEffect(type) { selectedItems.clear() }

    val currentList = (if(type==0) files else exts).toList().sorted()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { 
                    if (selectedItems.isEmpty()) Text("黑名单") 
                    else Text("${selectedItems.size} 已选择")
                },
                navigationIcon = { 
                    if (selectedItems.isEmpty()) IconButton(onClick = back) { Icon(Ico.ArrowBack, null) }
                    else IconButton(onClick = { selectedItems.clear() }) { Icon(Ico.UnselectAll, null) }
                },
                actions = {
                    if (selectedItems.isNotEmpty()) {
                        IconButton(onClick = {
                            vm.removeBlacklist(type, selectedItems.toList())
                            selectedItems.clear()
                        }) { Icon(Ico.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    } else {
                        IconButton(onClick = { 
                            selectedItems.clear()
                            selectedItems.addAll(currentList)
                        }) { Icon(Ico.SelectAll, null) }
                    }
                }
            ) 
        },
        floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }) { Icon(Ico.Add, null) } }
    ) { p ->
        Column(Modifier.padding(p)) {
            TabRow(selectedTabIndex = type) {
                Tab(selected = type==0, onClick = { type=0 }, text = { Text("文件名/文件夹") })
                Tab(selected = type==1, onClick = { type=1 }, text = { Text("后缀 (.ext)") })
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(currentList) { item ->
                    val isSel = item in selectedItems
                    ListItem(
                        modifier = Modifier.clickable { 
                            if(isSel) selectedItems.remove(item) else selectedItems.add(item) 
                        },
                        leadingContent = { Checkbox(checked = isSel, onCheckedChange = { if(it) selectedItems.add(item) else selectedItems.remove(item) }) },
                        headlineContent = { Text(item) }
                    )
                }
                if (currentList.isEmpty()) item { Text("空列表", Modifier.padding(32.dp), color = Color.Gray) }
            }
        }
    }
    
    if (showAdd) {
        var txt by remember { mutableStateOf(if(type==1) "." else "") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("添加规则") },
            text = { 
                Column {
                    OutlinedTextField(txt, { txt=it }, singleLine = true, label = { Text(if(type==1) ".ext" else "Name") })
                    if(type==1 && !txt.startsWith(".")) Text("必须以 . 开头", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { 
                Button(onClick = { 
                    if(txt.isNotEmpty()) { 
                        vm.addBlacklist(type, listOf(txt))
                        showAdd = false 
                    } 
                }) { Text("确定") } 
            }
        )
    }
}