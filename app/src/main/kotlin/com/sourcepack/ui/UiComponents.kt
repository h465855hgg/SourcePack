package com.sourcepack.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ... materialIcon 函数保持不变 ...
private fun materialIcon(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit): ImageVector {
    return ImageVector.Builder(name, 24.dp, 24.dp, 24.0f, 24.0f).apply {
        path(fill = SolidColor(Color.Black), pathBuilder = block)
    }.build()
}

object Ico {
    // 系统图标保持不变...
    val Settings = Icons.Default.Settings
    val ArrowBack = Icons.AutoMirrored.Filled.ArrowBack
    val ArrowRight = Icons.AutoMirrored.Filled.KeyboardArrowRight
    val Add = Icons.Default.Add
    val Delete = Icons.Default.Delete
    val Check = Icons.Default.Check
    val CheckCircle = Icons.Default.CheckCircle
    val Error = Icons.Default.Warning
    val Info = Icons.Default.Info

    // === 自定义图标 ===
    // Inventory2, Folder, File, CloudDownload, Sun, Moon 保持不变...
    val Inventory2 = materialIcon("Inv") {
        moveTo(20.0f, 2.0f); horizontalLineTo(4.0f); curveTo(2.9f, 2.0f, 2.0f, 2.9f, 2.0f, 4.0f); verticalLineToRelative(3.01f); curveTo(2.0f, 8.11f, 2.9f, 9.0f, 4.0f, 9.0f); horizontalLineToRelative(1.0f); verticalLineToRelative(11.0f); curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f); horizontalLineToRelative(10.0f); curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f); verticalLineTo(9.0f); horizontalLineToRelative(1.0f); curveToRelative(1.1f, 0.0f, 2.0f, -0.89f, 2.0f, -1.99f); verticalLineTo(4.0f); curveTo(22.0f, 2.9f, 21.1f, 2.0f, 20.0f, 2.0f); close(); moveTo(9.0f, 12.0f); horizontalLineToRelative(6.0f); verticalLineToRelative(2.0f); horizontalLineTo(9.0f); verticalLineTo(12.0f); close(); moveTo(20.0f, 7.0f); horizontalLineTo(4.0f); verticalLineTo(4.0f); horizontalLineToRelative(16.0f); verticalLineTo(7.0f); close()
    }
    val Folder = materialIcon("Folder") {
        moveTo(10.0f, 4.0f); horizontalLineTo(4.0f); curveTo(2.9f, 4.0f, 2.01f, 4.9f, 2.01f, 6.0f); lineTo(2.0f, 18.0f); curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f); horizontalLineToRelative(16.0f); curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f); verticalLineTo(8.0f); curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f); horizontalLineToRelative(-8.0f); lineTo(10.0f, 4.0f); close()
    }
    val File = materialIcon("File") {
        moveTo(14.0f, 2.0f); horizontalLineTo(6.0f); curveTo(4.9f, 2.0f, 4.01f, 2.9f, 4.01f, 4.0f); lineTo(4.0f, 20.0f); curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 1.99f, 2.0f); horizontalLineTo(18.0f); curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f); verticalLineTo(8.0f); lineTo(14.0f, 2.0f); close(); moveTo(16.0f, 18.0f); horizontalLineTo(8.0f); verticalLineToRelative(-2.0f); horizontalLineToRelative(8.0f); verticalLineTo(18.0f); close(); moveTo(16.0f, 14.0f); horizontalLineTo(8.0f); verticalLineToRelative(-2.0f); horizontalLineToRelative(8.0f); verticalLineTo(14.0f); close(); moveTo(13.0f, 9.0f); verticalLineTo(3.5f); lineTo(18.5f, 9.0f); horizontalLineTo(13.0f); close()
    }
    val CloudDownload = materialIcon("CloudDl") {
        moveTo(19.35f, 10.04f); curveTo(18.67f, 6.59f, 15.64f, 4.0f, 12.0f, 4.0f); curveTo(9.11f, 4.0f, 6.6f, 5.64f, 5.35f, 8.04f); curveTo(2.34f, 8.36f, 0.0f, 10.91f, 0.0f, 14.0f); curveToRelative(0.0f, 3.31f, 2.69f, 6.0f, 6.0f, 6.0f); horizontalLineToRelative(13.0f); curveToRelative(2.76f, 0.0f, 5.0f, -2.24f, 5.0f, -5.0f); curveToRelative(0.0f, -2.64f, -2.05f, -4.78f, -4.65f, -4.96f); close(); moveTo(17.0f, 13.0f); lineToRelative(-5.0f, 5.0f); lineToRelative(-5.0f, -5.0f); horizontalLineToRelative(3.0f); verticalLineTo(9.0f); horizontalLineToRelative(4.0f); verticalLineToRelative(4.0f); horizontalLineToRelative(3.0f); close()
    }
    val Sun = materialIcon("Sun") {
        moveTo(12.0f, 7.0f); curveToRelative(-2.76f, 0.0f, -5.0f, 2.24f, -5.0f, 5.0f); reflectiveCurveToRelative(2.24f, 5.0f, 5.0f, 5.0f); reflectiveCurveToRelative(5.0f, -2.24f, 5.0f, -5.0f); reflectiveCurveToRelative(-2.24f, -5.0f, -5.0f, -5.0f); close(); moveTo(12.0f, 2.0f); verticalLineToRelative(2.0f); moveTo(12.0f, 20.0f); verticalLineToRelative(2.0f); moveTo(4.22f, 4.22f); lineToRelative(1.42f, 1.42f); moveTo(18.36f, 18.36f); lineToRelative(1.42f, 1.42f); moveTo(2.0f, 12.0f); horizontalLineToRelative(2.0f); moveTo(20.0f, 12.0f); horizontalLineToRelative(2.0f); moveTo(4.22f, 19.78f); lineToRelative(1.42f, -1.42f); moveTo(18.36f, 5.64f); lineToRelative(1.42f, -1.42f)
    }
    val Moon = materialIcon("Moon") {
        moveTo(11.01f, 3.05f); curveTo(6.51f, 3.54f, 3.0f, 7.36f, 3.0f, 12.0f); curveToRelative(0.0f, 4.97f, 4.03f, 9.0f, 9.0f, 9.0f); curveToRelative(4.63f, 0.0f, 8.45f, -3.5f, 8.95f, -8.0f); curveToRelative(-5.25f, 0.0f, -9.5f, -4.25f, -9.5f, -9.5f); close()
    }

    // === 全选 (SelectAll) ===
    // 采用“积木法”绘制：把虚线看作一个个小长方形块，确保填充可见
    val SelectAll = materialIcon("SelAll") {
        // --- 外层大虚线框 (24x24画布, 边距3) ---
        // 上边两段
        moveTo(4f, 4f); horizontalLineToRelative(5f); verticalLineToRelative(2f); horizontalLineTo(4f); close() // 左上横
        moveTo(15f, 4f); horizontalLineToRelative(5f); verticalLineToRelative(2f); horizontalLineTo(15f); close() // 右上横
        // 下边两段
        moveTo(4f, 18f); horizontalLineToRelative(5f); verticalLineToRelative(2f); horizontalLineTo(4f); close() // 左下横
        moveTo(15f, 18f); horizontalLineToRelative(5f); verticalLineToRelative(2f); horizontalLineTo(15f); close() // 右下横
        // 左边中间两段 (竖条)
        moveTo(4f, 7f); horizontalLineToRelative(2f); verticalLineToRelative(4f); horizontalLineTo(4f); close()
        moveTo(4f, 13f); horizontalLineToRelative(2f); verticalLineToRelative(4f); horizontalLineTo(4f); close()
        // 右边中间两段 (竖条)
        moveTo(18f, 7f); horizontalLineToRelative(2f); verticalLineToRelative(4f); horizontalLineTo(18f); close()
        moveTo(18f, 13f); horizontalLineToRelative(2f); verticalLineToRelative(4f); horizontalLineTo(18f); close()

        // --- 内层小虚线框 (中心) ---
        // 四个角的小方块，组成一个小框
        moveTo(9f, 9f); horizontalLineToRelative(2f); verticalLineToRelative(2f); horizontalLineTo(9f); close()
        moveTo(13f, 9f); horizontalLineToRelative(2f); verticalLineToRelative(2f); horizontalLineTo(13f); close()
        moveTo(13f, 13f); horizontalLineToRelative(2f); verticalLineToRelative(2f); horizontalLineTo(13f); close()
        moveTo(9f, 13f); horizontalLineToRelative(2f); verticalLineToRelative(2f); horizontalLineTo(9f); close()
    }

    // === 取消全选 (UnselectAll) ===
    // 基础同上，增加一条粗斜线
    val UnselectAll = materialIcon("UnSel") {
        // 1. 复制 SelectAll 的所有内容
        // 上
        moveTo(4f, 4f); horizontalLineToRelative(5f); verticalLineToRelative(2f); horizontalLineTo(4f); close()
        moveTo(15f, 4f); horizontalLineToRelative(5f); verticalLineToRelative(2f); horizontalLineTo(15f); close()
        // 下
        moveTo(4f, 18f); horizontalLineToRelative(5f); verticalLineToRelative(2f); horizontalLineTo(4f); close()
        moveTo(15f, 18f); horizontalLineToRelative(5f); verticalLineToRelative(2f); horizontalLineTo(15f); close()
        // 左
        moveTo(4f, 7f); horizontalLineToRelative(2f); verticalLineToRelative(4f); horizontalLineTo(4f); close()
        moveTo(4f, 13f); horizontalLineToRelative(2f); verticalLineToRelative(4f); horizontalLineTo(4f); close()
        // 右
        moveTo(18f, 7f); horizontalLineToRelative(2f); verticalLineToRelative(4f); horizontalLineTo(18f); close()
        moveTo(18f, 13f); horizontalLineToRelative(2f); verticalLineToRelative(4f); horizontalLineTo(18f); close()
        // 内
        moveTo(9f, 9f); horizontalLineToRelative(2f); verticalLineToRelative(2f); horizontalLineTo(9f); close()
        moveTo(13f, 9f); horizontalLineToRelative(2f); verticalLineToRelative(2f); horizontalLineTo(13f); close()
        moveTo(13f, 13f); horizontalLineToRelative(2f); verticalLineToRelative(2f); horizontalLineTo(13f); close()
        moveTo(9f, 13f); horizontalLineToRelative(2f); verticalLineToRelative(2f); horizontalLineTo(9f); close()

        // 2. 增加实心斜线 (左下到右上)
        // 使用多边形来模拟一条有厚度的线
        moveTo(5f, 20f) // 起点：左下
        lineTo(20f, 5f) // 终点：右上
        lineTo(21f, 6f) // 稍微往右偏移一点，形成宽度
        lineTo(6f, 21f) // 回到左下偏移点
        close()
    }
}

@Composable
fun HomeBtn(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun ResultCard(success: Boolean, msg: String, onReset: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (success) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.padding(16.dp).fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                if (success) Ico.CheckCircle else Ico.Error,
                null,
                modifier = Modifier.size(48.dp),
                tint = if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(16.dp))
            Text(msg, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onReset) { Text("OK") }
        }
    }
}

@Composable
fun SettingHeader(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingLink(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, null) },
        trailingContent = { Icon(Ico.ArrowRight, null) }
    )
}

@Composable
fun SwitchItem(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) }
    )
}

@Composable
fun LoadingView(msg: String, detail: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(24.dp))
        Text(msg, style = MaterialTheme.typography.titleMedium)
        if (detail.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}