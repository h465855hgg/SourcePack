package com.sourcepack.data

data class PackerConfig(
    val compress: Boolean = false,
    val removeComments: Boolean = false,
    val ignoreGit: Boolean = true,
    val ignoreBuild: Boolean = true,
    val ignoreGradle: Boolean = true,
    val useGitIgnore: Boolean = true,
    val format: Format = Format.MARKDOWN,
    val mode: Mode = Mode.FULL
)

enum class Format { MARKDOWN, XML, TEXT }
enum class Mode { FULL, TREE }
// AppTheme enum removed