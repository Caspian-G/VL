package com.Fch.vl.viewmodel

import java.util.zip.ZipFile

class ZipManager {
    fun getFileList(zipPath: String): List<ZipEntry> {
        val entries = mutableListOf<ZipEntry>()
        try {
            ZipFile(zipPath).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    val path = entry.name
                    val name = path.substringAfterLast("/", path)
                    val isDirectory = entry.isDirectory
                    // 获取父路径
                    val parentPath = if (path.contains("/")) {
                        path.substringBeforeLast("/")
                    } else {
                        null
                    }
                    // 计算层级深度
                    val depth = if (path.endsWith("/")) {
                        path.count { it == '/' }
                    } else {
                        path.count { it == '/' }
                    }
                    entries.add(
                        ZipEntry(
                            name = name,
                            fullPath = path,
                            isDirectory = isDirectory,
                            parentPath = parentPath,
                            depth = depth,
                            size = if (!isDirectory) entry.size else 0,
                            extension = if (!isDirectory) {
                                path.substringAfterLast(".", "")
                            } else ""
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // 按路径排序，文件夹优先
        return entries.sortedWith(compareBy({ !it.isDirectory }, { it.fullPath }))
    }
    fun getFolderContent(zipPath: String, folderPath: String? = null): List<ZipEntry> {
        val allEntries = getFileList(zipPath)

        return if (folderPath == null) {
            allEntries.filter { entry ->
                entry.parentPath == null
            }.map {
                if (it.isDirectory) it.copy(name = it.name.removeSuffix("/")) else it
            }
        } else {
            allEntries.filter { entry ->
                entry.parentPath == folderPath
            }.map {
                if (it.isDirectory) it.copy(name = it.name.removeSuffix("/")) else it
            }
        }
    }

    fun getAllFolders(zipPath: String): List<ZipEntry> {
        return getFileList(zipPath).filter { it.isDirectory }
    }

    fun getAllFiles(zipPath: String): List<ZipEntry> {
        return getFileList(zipPath).filter { !it.isDirectory }
    }

    fun getFilesByType(zipPath: String, vararg extensions: String): List<ZipEntry> {
        val extensionsList = extensions.map { it.lowercase() }
        return getAllFiles(zipPath).filter { entry ->
            extensionsList.contains(entry.extension.lowercase())
        }
    }

    fun getVideoFiles(zipPath: String): List<ZipEntry> {
        val videoExtensions = listOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v")
        return getFilesByType(zipPath, *videoExtensions.toTypedArray())
    }

    /**
     * 获取图片文件
     */
    fun getImageFiles(zipPath: String): List<ZipEntry> {
        val imageExtensions = listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg")
        return getFilesByType(zipPath, *imageExtensions.toTypedArray())
    }

    fun getAudioFiles(zipPath: String): List<ZipEntry> {
        val audioExtensions = listOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "wma")
        return getFilesByType(zipPath, *audioExtensions.toTypedArray())
    }

    fun buildFolderTree(zipPath: String): FolderNode {
        val root = FolderNode(name = "根目录", path = null)
        val allEntries = getFileList(zipPath)

        allEntries.forEach { entry ->
            if (entry.isDirectory) {
                // 添加文件夹
                addFolderToTree(root, entry.fullPath.removeSuffix("/"))
            } else {
                // 添加文件
                addFileToTree(root, entry)
            }
        }

        return root
    }

    private fun addFolderToTree(root: FolderNode, folderPath: String) {
        val parts = folderPath.split("/").filter { it.isNotEmpty() }
        var currentNode = root

        parts.forEach { part ->
            var child = currentNode.children.find { it.name == part && it.isFolder }
            if (child == null) {
                child = FolderNode(name = part, path = folderPath, parent = currentNode)
                currentNode.children.add(child)
            }
            currentNode = child
        }
    }

    private fun addFileToTree(root: FolderNode, file: ZipEntry) {
        val parts = file.fullPath.split("/").filter { it.isNotEmpty() }
        val fileName = parts.last()
        val folderParts = parts.dropLast(1)

        var currentNode = root
        folderParts.forEach { part ->
            var child = currentNode.children.find { it.name == part && it.isFolder }
            if (child == null) {
                child = FolderNode(name = part, path = part, parent = currentNode)
                currentNode.children.add(child)
            }
            currentNode = child
        }

        currentNode.children.add(
            FolderNode(
                name = fileName,
                path = file.fullPath,
                isFolder = false,
                size = file.size,
                extension = file.extension,
                parent = currentNode
            )
        )
    }
}

data class ZipEntry(
    val name: String,           // 文件名/文件夹名
    val fullPath: String,       // 完整路径
    val isDirectory: Boolean,   // 是否是文件夹
    val parentPath: String?,    // 父路径
    val depth: Int,             // 深度层级
    val size: Long = 0,         // 文件大小（文件夹为0）
    val extension: String = ""  // 扩展名
)

data class FolderNode(
    val name: String,
    val path: String?,
    val isFolder: Boolean = true,
    val size: Long = 0,
    val extension: String = "",
    val parent: FolderNode? = null,
    val children: MutableList<FolderNode> = mutableListOf()
) {
    fun isRoot(): Boolean = path == null
    fun getFullPath(): String = if (parent?.isRoot() == true) name else "${parent?.getFullPath()}/$name"
}
