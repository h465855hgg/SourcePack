package com.sourcepack.data

import android.content.Context
import android.net.Uri

class PreferenceManager(context: Context) {
    private val p = context.getSharedPreferences("sp_cfg", Context.MODE_PRIVATE)

    private var isInitialized: Boolean
        get() = p.getBoolean("init_done", false)
        set(v) = p.edit().putBoolean("init_done", v).apply()

    fun initDefaultsIfNeeded() {
        val legacyFirst = p.getBoolean("first", true)
        if (!isInitialized && legacyFirst) {
            val defaultFiles = setOf("local.properties", ".DS_Store", "thumbs.db", "desktop.ini")
            val defaultExts = setOf(
                ".png", ".jpg", ".jpeg", ".webp", ".gif", ".ico", ".svg", ".bmp",
                ".mp4", ".mp3", ".wav", ".ogg",
                ".jar", ".dex", ".so", ".apk", ".aab",
                ".zip", ".7z", ".rar", ".tar", ".gz",
                ".db", ".sqlite", ".class", ".exe", ".dll", ".bin",
                ".pdf", ".doc", ".docx", ".ppt", ".pptx", ".xls", ".xlsx"
            )
            p.edit()
                .putStringSet("u_files", defaultFiles)
                .putStringSet("u_exts", defaultExts)
                .putBoolean("init_done", true)
                .remove("first")
                .apply()
        }
    }

    // 存储是否为深色模式
    var isDarkTheme: Boolean
        get() = p.getBoolean("is_dark_theme", false)
        set(v) = p.edit().putBoolean("is_dark_theme", v).apply()

    // --- 新增：自定义导出路径 URI ---
    var exportUriStr: String?
        get() = p.getString("export_uri", null)
        set(v) = p.edit().putString("export_uri", v).apply()

    var config: PackerConfig
        get() = PackerConfig(
            compress = p.getBoolean("c_zip", false),
            removeComments = p.getBoolean("c_nocom", false),
            ignoreGit = p.getBoolean("c_git", true),
            ignoreBuild = p.getBoolean("c_bld", true),
            ignoreGradle = p.getBoolean("c_grd", true),
            useGitIgnore = p.getBoolean("c_gign", true),
            format = Format.values().getOrElse(p.getInt("c_fmt", 0)) { Format.MARKDOWN },
            mode = Mode.values().getOrElse(p.getInt("c_mod", 0)) { Mode.FULL }
        )
        set(v) {
            p.edit()
                .putBoolean("c_zip", v.compress)
                .putBoolean("c_nocom", v.removeComments)
                .putBoolean("c_git", v.ignoreGit)
                .putBoolean("c_bld", v.ignoreBuild)
                .putBoolean("c_grd", v.ignoreGradle)
                .putBoolean("c_gign", v.useGitIgnore)
                .putInt("c_fmt", v.format.ordinal)
                .putInt("c_mod", v.mode.ordinal)
                .apply()
        }

    fun getSet(k: String): Set<String> = p.getStringSet(k, emptySet()) ?: emptySet()
    fun updateSet(k: String, newSet: Set<String>) = p.edit().putStringSet(k, newSet).apply()
}