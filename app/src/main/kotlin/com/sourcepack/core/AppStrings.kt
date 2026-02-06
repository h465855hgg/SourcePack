package com.sourcepack.core

import com.sourcepack.BuildConfig

object Str {
    // 移除了双语支持函数 get(cn, en) 和所有关于页面的文案
    const val APP_VERSION = "v${BuildConfig.VERSION_NAME}"
}