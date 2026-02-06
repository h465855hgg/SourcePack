package com.sourcepack.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sourcepack.core.*
import com.sourcepack.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

sealed class UiState {
    data object Idle : UiState()
    data class Loading(val msg: String, val detail: String = "") : UiState()
    data class Success(val info: String) : UiState()
    data class Error(val err: String) : UiState()
}

class MainVM(app: Application) : AndroidViewModel(app) {
    private val prefs = PreferenceManager(app)
    
    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state = _state.asStateFlow()
    
    private val _cfg = MutableStateFlow(prefs.config)
    val cfg = _cfg.asStateFlow()
    
    private val _isDark = MutableStateFlow(prefs.isDarkTheme)
    val isDark = _isDark.asStateFlow()
    
    private val _uFiles = MutableStateFlow(prefs.getSet("u_files"))
    val uFiles = _uFiles.asStateFlow()
    private val _uExts = MutableStateFlow(prefs.getSet("u_exts"))
    val uExts = _uExts.asStateFlow()

    // 导出路径 (仅支持 SAF Uri)
    private val _exportDir = MutableStateFlow<Uri?>(
        prefs.exportUriStr?.let { Uri.parse(it) }
    )
    val exportDir = _exportDir.asStateFlow()

    private var currentJob: Job? = null

    init {
        prefs.initDefaultsIfNeeded()
        _uFiles.value = prefs.getSet("u_files")
        _uExts.value = prefs.getSet("u_exts")
    }

    fun saveCfg(c: PackerConfig) { prefs.config = c; _cfg.value = c }
    fun toggleTheme() {
        val newMode = !_isDark.value
        prefs.isDarkTheme = newMode
        _isDark.value = newMode
    }
    fun reset() { _state.value = UiState.Idle }
    fun cancelTask() {
        currentJob?.cancel()
        _state.value = UiState.Idle
    }
    fun addBlacklist(type: Int, items: List<String>) {
        val key = if(type == 0) "u_files" else "u_exts"
        val current = prefs.getSet(key).toMutableSet()
        items.forEach { item ->
            val cleanItem = if(type == 1 && !item.startsWith(".")) ".$item" else item
            current.add(cleanItem)
        }
        prefs.updateSet(key, current)
        refreshLists()
    }
    fun removeBlacklist(type: Int, items: List<String>) {
        val key = if(type == 0) "u_files" else "u_exts"
        val current = prefs.getSet(key).toMutableSet()
        current.removeAll(items.toSet())
        prefs.updateSet(key, current)
        refreshLists()
    }
    private fun refreshLists() {
        _uFiles.value = prefs.getSet("u_files")
        _uExts.value = prefs.getSet("u_exts")
    }

    // 设置导出路径 (仅支持 SAF Uri)
    fun setExportDirectory(uri: Uri?) {
        if (uri == null) {
            prefs.exportUriStr = null
            _exportDir.value = null
            return
        }
        try {
            // 申请持久化权限 (这样重启APP后还能记住位置)
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            getApplication<Application>().contentResolver.takePersistableUriPermission(uri, takeFlags)
            
            prefs.exportUriStr = uri.toString()
            _exportDir.value = uri
        } catch (e: Exception) {
            // 如果失败(很少见)，依然尝试保存
            prefs.exportUriStr = uri.toString()
            _exportDir.value = uri
        }
    }

    // 在指定目录下创建文件
    private fun createDestFile(fileName: String): Uri {
        val dirUri = _exportDir.value ?: throw IllegalStateException("未设置导出路径")
        
        // 使用 DocumentFile 操作 SAF
        val dir = DocumentFile.fromTreeUri(getApplication(), dirUri)
        if (dir == null || !dir.canWrite()) {
            throw IllegalStateException("导出目录无法写入，请重新选择")
        }
        
        // 如果文件已存在，先删除旧的
        dir.findFile(fileName)?.delete()
        
        val mimeType = "text/markdown"
        val newFile = dir.createFile(mimeType, fileName) 
            ?: throw IllegalStateException("无法创建文件，请检查权限")
        return newFile.uri
    }

    private val progressCb = object : SourcePacker.ProgressCallback {
        private var lastUpdate = 0L
        override fun onProgress(currentFile: String) {
            val now = System.currentTimeMillis()
            if (now - lastUpdate > 100) {
                lastUpdate = now
                val current = _state.value
                if (current is UiState.Loading) {
                    _state.value = current.copy(detail = currentFile)
                }
            }
        }
    }

    fun packDirectly(srcUri: Uri, destUri: Uri?, fileName: String? = null) {
        _state.value = UiState.Loading("正在处理...")
        currentJob = viewModelScope.launch {
            try {
                // 如果传入了 destUri (手动选择模式)，直接用；否则在默认目录下创建 (自动模式)
                val finalDest = destUri ?: createDestFile(fileName ?: "output.md")
                
                SourcePacker.packToStream(
                    getApplication(), srcUri, finalDest, 
                    _uFiles.value, _uExts.value, _cfg.value, progressCb
                )
                _state.value = UiState.Success("文件已保存至: ${finalDest.path}")
            } catch (e: CancellationException) {
                _state.value = UiState.Idle
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "未知错误")
            }
        }
    }

    fun packListDirectly(srcUris: List<Uri>, destUri: Uri?, fileName: String? = null) {
        _state.value = UiState.Loading("正在处理...")
        currentJob = viewModelScope.launch {
            try {
                val finalDest = destUri ?: createDestFile(fileName ?: "output.md")
                SourcePacker.packListToStream(
                    getApplication(), srcUris, finalDest, _cfg.value, progressCb
                )
                _state.value = UiState.Success("文件已保存至: ${finalDest.path}")
            } catch (e: CancellationException) {
                _state.value = UiState.Idle
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "未知错误")
            }
        }
    }
    
    fun runGit(url: String, destUri: Uri?, fileName: String? = null) {
        var cleanUrl = url.trim().removeSuffix("/")
        if (cleanUrl.endsWith(".git")) cleanUrl = cleanUrl.removeSuffix(".git")
        if (!cleanUrl.contains("github.com")) {
            _state.value = UiState.Error("无效的 GitHub 链接")
            return
        }
        val path = cleanUrl.substringAfter("github.com/")
        val finalPath = if (path.contains("/tree/")) path.substringBefore("/tree/") else path
        val zipUrl = "https://github.com/$finalPath/archive/HEAD.zip"

        _state.value = UiState.Loading("正在下载仓库...")
        currentJob = viewModelScope.launch {
            try {
                val finalDest = destUri ?: createDestFile(fileName ?: "repo_export.md")
                SourcePacker.packGitHubRepo(
                    zipUrl, finalDest, getApplication(),
                    _uFiles.value, _uExts.value, _cfg.value, progressCb
                )
                _state.value = UiState.Success("GitHub 仓库已导出")
            } catch (e: CancellationException) {
                _state.value = UiState.Idle
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = UiState.Error("错误: ${e.message}")
            }
        }
    }
}