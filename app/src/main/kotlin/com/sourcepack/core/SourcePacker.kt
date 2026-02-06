package com.sourcepack.core

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.sourcepack.data.*
import kotlinx.coroutines.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object SourcePacker {
    private val FORCE_IGNORE_DIRS = setOf(".git", ".svn", ".idea", ".vscode", ".gradle", "build", "target", "node_modules", "captures")
    
    private val BINARY_EXTS = setOf(
        ".zip", ".7z", ".rar", ".tar", ".gz", ".apk", ".jar", ".png", ".jpg", ".jpeg", ".webp", ".gif", ".ico", ".svg",
        ".so", ".dll", ".exe", ".class", ".dex", ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
        ".mp3", ".mp4", ".wav", ".ogg", ".db", ".sqlite", ".ttf", ".woff", ".eot", ".psd", ".ai"
    )
    
    private const val MAX_FILE_SIZE = 1024 * 1024L 
    private const val BUFFER_SIZE = 16 * 1024 

    interface ProgressCallback {
        fun onProgress(currentFile: String)
    }

    suspend fun packToStream(
        ctx: Context,
        root: FastFile, 
        destUri: Uri,
        userFiles: Set<String>,
        userExts: Set<String>,
        cfg: PackerConfig,
        cb: ProgressCallback
    ) = withContext(Dispatchers.IO) {
        val outputStream = ctx.contentResolver.openOutputStream(destUri, "w") ?: throw IOException("Cannot open dest URI")
        // 使用 BufferWriter 减少 IO 次数
        val writer = BufferedWriter(OutputStreamWriter(outputStream), BUFFER_SIZE)

        try {
            val projectName = root.name
            val destName = DocumentFile.fromSingleUri(ctx, destUri)?.name ?: "unknown_output_file"

            val skipDirs = FORCE_IGNORE_DIRS.toMutableSet().apply {
                if (cfg.ignoreGradle) add(".gradle")
                if (cfg.ignoreBuild) add("build")
                if (cfg.ignoreGit) add(".git")
            }
            val binExts = userExts.map { if (it.startsWith(".")) it else ".$it" }.toSet()

            writeHeader(writer, projectName, cfg)

            if (cfg.format != Format.XML) {
                cb.onProgress("Generating Tree...")
                writer.write("## Project Structure\n\n")
                writer.write("```text\n")
                val treeBuilder = StringBuilder()
                generateTreeString(root, "", treeBuilder, skipDirs, destName)
                writer.write(treeBuilder.toString())
                writer.write("```\n\n")
            }

            if (cfg.mode == Mode.FULL || cfg.format == Format.XML) {
                if (cfg.format != Format.XML) {
                    writer.write("## File Contents\n\n")
                }
                processNode(ctx, root, "", writer, skipDirs, userFiles, binExts, cfg, cb, destName)
            }

            writeFooter(writer, cfg)
        } finally {
            try {
                writer.flush()
                writer.close()
                outputStream.close()
            } catch (_: Exception) {}
        }
    }

    // ... packGitHubRepo 保持不变 ...
    suspend fun packGitHubRepo(
        urlStr: String,
        destUri: Uri,
        ctx: Context,
        userFiles: Set<String>,
        userExts: Set<String>,
        cfg: PackerConfig,
        cb: ProgressCallback
    ) = withContext(Dispatchers.IO) {
        val tempFile = File(ctx.cacheDir, "gh_temp_${System.currentTimeMillis()}.zip")
        try {
            cb.onProgress("Downloading...")
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 60000
            conn.instanceFollowRedirects = true
            conn.inputStream.use { input -> FileOutputStream(tempFile).use { output -> input.copyTo(output) } }
            
            cb.onProgress("Analyzing Structure...")
            val zipFile = ZipFile(tempFile)
            val rootNode = buildZipVFS(zipFile, urlStr.substringAfterLast("/").substringBefore("."))
            
            packToStream(ctx, rootNode, destUri, userFiles, userExts, cfg, cb)
            zipFile.close()
        } finally {
            tempFile.delete()
        }
    }

    private fun generateTreeString(
        node: FastFile,
        prefix: String,
        sb: StringBuilder,
        skipDirs: Set<String>,
        ignoreFile: String
    ) {
        if (prefix.isEmpty()) sb.append("📦 ${node.name}\n")

        if (node.isDirectory) {
            // listFiles 可能是耗时操作，但在 IO 线程中是安全的
            val children = node.listFiles()
                .sortedWith(compareBy({ !it.isDirectory }, { it.name }))
            
            for (child in children) {
                val name = child.name
                if (!child.isDirectory && name == ignoreFile) continue
                if (child.isDirectory && name in skipDirs) continue
                
                val icon = if (child.isDirectory) " 📂 " else " 📄 "
                sb.append(prefix).append(icon).append(name).append("\n")
                
                if (child.isDirectory) {
                    generateTreeString(child, "$prefix  ", sb, skipDirs, ignoreFile)
                }
            }
        }
    }

    private suspend fun processNode(
        ctx: Context,
        node: FastFile,
        relativePath: String,
        writer: BufferedWriter,
        skipDirs: Set<String>,
        userFiles: Set<String>,
        binExts: Set<String>,
        cfg: PackerConfig,
        cb: ProgressCallback,
        ignoreFile: String
    ) {
        // 频繁检查协程取消状态，保证 UI 响应
        currentCoroutineContext().ensureActive()

        if (node.isDirectory) {
            if (cfg.format == Format.XML && relativePath.isNotEmpty()) {
                writer.write("  <dir name=\"${node.name}\">\n")
            }

            val children = node.listFiles()
                .sortedWith(compareBy({ !it.isDirectory }, { it.name }))
            
            for (child in children) {
                val name = child.name
                if (!child.isDirectory && name == ignoreFile) continue
                val childPath = if (relativePath.isEmpty()) name else "$relativePath/$name"

                if (child.isDirectory) {
                    if (name in skipDirs) continue
                } else {
                    if (name in userFiles) continue
                    if (binExts.any { name.endsWith(it, ignoreCase = true) }) continue
                }
                processNode(ctx, child, childPath, writer, skipDirs, userFiles, binExts, cfg, cb, ignoreFile)
            }
            if (cfg.format == Format.XML && relativePath.isNotEmpty()) {
                writer.write("  </dir>\n")
            }
        } else {
            if (cfg.mode == Mode.TREE && cfg.format != Format.XML) return
            
            cb.onProgress(relativePath)
            
            val isBinExt = BINARY_EXTS.any { node.name.endsWith(it, ignoreCase = true) }
            if (isBinExt || node.length > MAX_FILE_SIZE) return

            appendContent(ctx, node, relativePath, writer, cfg)
        }
    }

    // FastFile 接口及实现保持不变...
    interface FastFile {
        val name: String
        val isDirectory: Boolean
        val length: Long
        fun listFiles(): List<FastFile>
        fun openStream(ctx: Context): InputStream
    }
    class JavaIoFile(val file: File) : FastFile {
        override val name: String get() = file.name
        override val isDirectory: Boolean get() = file.isDirectory
        override val length: Long get() = file.length()
        override fun listFiles(): List<FastFile> = file.listFiles()?.map { JavaIoFile(it) } ?: emptyList()
        override fun openStream(ctx: Context): InputStream = FileInputStream(file)
    }
    class DocumentFileNode(val file: DocumentFile) : FastFile {
        override val name: String get() = file.name ?: ""
        override val isDirectory: Boolean get() = file.isDirectory
        override val length: Long get() = file.length()
        override fun listFiles(): List<FastFile> = file.listFiles().map { DocumentFileNode(it) }
        override fun openStream(ctx: Context): InputStream = ctx.contentResolver.openInputStream(file.uri) ?: throw IOException()
    }
    class ZipFastFile(
        override val name: String,
        override val isDirectory: Boolean,
        private val zipFile: ZipFile,
        private val entry: ZipEntry?,
        private val children: List<ZipFastFile> = emptyList()
    ) : FastFile {
        override val length: Long get() = entry?.size ?: 0L
        override fun listFiles(): List<FastFile> = children
        override fun openStream(ctx: Context): InputStream = if (entry != null) zipFile.getInputStream(entry) else ByteArrayInputStream(ByteArray(0))
    }
    
    // buildZipVFS 保持不变...
    private fun buildZipVFS(zipFile: ZipFile, projectName: String): ZipFastFile {
        val treeMap = mutableMapOf<String, MutableList<ZipEntry>>()
        val entries = zipFile.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val path = entry.name.removeSuffix("/")
            if (path.isEmpty()) continue
            val parentPath = if (path.contains("/")) path.substringBeforeLast("/") else ""
            treeMap.getOrPut(parentPath) { mutableListOf() }.add(entry)
        }
        fun buildNode(name: String, path: String, entry: ZipEntry?): ZipFastFile {
            val isDir = entry?.isDirectory ?: true
            val childrenEntries = treeMap[path] ?: emptyList()
            val childrenNodes = childrenEntries.map { childEntry ->
                val childName = childEntry.name.removeSuffix("/").substringAfterLast("/")
                val childPath = childEntry.name.removeSuffix("/")
                buildNode(childName, childPath, childEntry)
            }
            return ZipFastFile(name, isDir, zipFile, entry, childrenNodes)
        }
        val rootChildren = treeMap[""] ?: emptyList()
        if (rootChildren.size == 1 && rootChildren[0].isDirectory) {
            val realRoot = rootChildren[0]
            return buildNode(realRoot.name.removeSuffix("/"), realRoot.name.removeSuffix("/"), realRoot)
        }
        return buildNode(projectName, "", null)
    }

    suspend fun packToStream(ctx: Context, rootUri: Uri, destUri: Uri, uFiles: Set<String>, uExts: Set<String>, cfg: PackerConfig, cb: ProgressCallback) {
        val rootNode: FastFile = if (rootUri.scheme == "file") {
            JavaIoFile(File(rootUri.path!!))
        } else {
            DocumentFileNode(DocumentFile.fromTreeUri(ctx, rootUri)!!)
        }
        packToStream(ctx, rootNode, destUri, uFiles, uExts, cfg, cb)
    }

    private suspend fun appendContent(ctx: Context, node: FastFile, path: String, writer: BufferedWriter, cfg: PackerConfig) {
        try {
            writer.write(formatHeader(path, cfg.format))
            
            node.openStream(ctx).use { ins ->
                val headBuffer = ByteArray(1024)
                val headReadLen = readAtMost(ins, headBuffer)
                val isBinary = if (headReadLen > 0) isBufferBinary(headBuffer, headReadLen) else false
                
                if (isBinary) {
                    writer.write("[Binary content detected]")
                } else {
                    val headStream = ByteArrayInputStream(headBuffer, 0, headReadLen)
                    val combinedStream = SequenceInputStream(headStream, ins)
                    
                    // 读取文本 (IO 操作)
                    val rawContent = combinedStream.bufferedReader().use { it.readText() }
                    
                    // CPU 密集操作转移到 Default 线程
                    val contentToProcess = withContext(Dispatchers.Default) {
                        if (cfg.removeComments) removeComments(rawContent, path) else rawContent
                    }

                    // 写入
                    contentToProcess.lineSequence().forEach { line ->
                        if (cfg.compress) {
                            val trimmed = line.trim()
                            if (trimmed.isNotEmpty()) {
                                if (cfg.format == Format.XML) writer.write(escapeXml(trimmed))
                                else writer.write(trimmed)
                                writer.write(" ")
                            }
                        } else {
                             if (cfg.format == Format.XML) writer.write(escapeXml(line))
                             else writer.write(line)
                             writer.write("\n")
                        }
                    }
                }
            }
            writer.write(formatFooter(cfg.format))
        } catch (e: Exception) {
            writer.write("\n[Read Error: ${e.message}]\n")
        }
    }

    // removeComments, readAtMost, isBufferBinary, writeHeader, writeFooter, formatHeader, formatFooter, escapeXml 保持不变 ...
    
    private fun removeComments(text: String, fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "java", "kt", "kts", "c", "cpp", "h", "cs", "js", "ts", "jsx", "tsx", "swift", "go", "rs", "scala", "groovy", "css", "scss", "gradle" -> {
                val noBlock = text.replace(Regex("/\\*[\\s\\S]*?\\*/"), "")
                noBlock.replace(Regex("(?<!:)//.*"), "")
            }
            "py", "sh", "rb", "yaml", "yml", "properties", "dockerfile", "conf", "toml" -> text.replace(Regex("#.*"), "")
            "xml", "html", "htm", "svg", "androidmanifest" -> text.replace(Regex("<!--[\\s\\S]*?-->"), "")
            else -> text
        }
    }

    private fun readAtMost(input: InputStream, buffer: ByteArray): Int {
        var total = 0
        while (total < buffer.size) {
            val count = input.read(buffer, total, buffer.size - total)
            if (count == -1) break
            total += count
        }
        return total
    }

    private fun isBufferBinary(buf: ByteArray, len: Int): Boolean {
        for (i in 0 until len) if (buf[i] == 0.toByte()) return true
        return false
    }

    private fun writeHeader(writer: BufferedWriter, name: String, cfg: PackerConfig) {
        if (cfg.format == Format.XML) writer.write("<project name=\"$name\">\n<files>\n")
        else writer.write("# Project: $name\n\n")
    }

    private fun writeFooter(writer: BufferedWriter, cfg: PackerConfig) {
        if (cfg.format == Format.XML) writer.write("</files>\n</project>")
    }

    private fun formatHeader(name: String, format: Format): String {
        return when (format) {
            Format.MARKDOWN -> "\n## $name\n```${name.substringAfterLast('.', "")}\n"
            Format.XML -> "\n<file path=\"$name\">\n"
            Format.TEXT -> "\n--- $name ---\n"
        }
    }

    private fun formatFooter(format: Format): String {
        return when (format) {
            Format.MARKDOWN -> "```\n"
            Format.XML -> "</file>\n"
            Format.TEXT -> "\n"
        }
    }
    
    private fun escapeXml(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    suspend fun packListToStream(ctx: Context, uris: List<Uri>, destUri: Uri, cfg: PackerConfig, cb: ProgressCallback) = withContext(Dispatchers.IO) {
        val outputStream = ctx.contentResolver.openOutputStream(destUri, "w") ?: return@withContext
        val writer = BufferedWriter(OutputStreamWriter(outputStream), BUFFER_SIZE)
        writer.write(if (cfg.format == Format.XML) "<file_list>\n" else "# Selected Files\n\n")
        
        uris.forEach { uri ->
            val df = DocumentFile.fromSingleUri(ctx, uri) ?: return@forEach
            cb.onProgress(df.name ?: "unknown")
            appendContent(ctx, DocumentFileNode(df), df.name ?: "unknown", writer, cfg)
        }
        
        if (cfg.format == Format.XML) writer.write("</file_list>")
        writer.flush(); writer.close()
    }
}