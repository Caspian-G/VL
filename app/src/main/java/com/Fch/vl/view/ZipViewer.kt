package com.Fch.vl.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Fch.vl.R
import com.Fch.vl.view.theme.Grey
import com.Fch.vl.viewmodel.ZipEntry
import com.Fch.vl.viewmodel.ZipManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun zipViewer(zipPath: String, onClose: () -> Unit) {
    val videoExtensions = listOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "3gp")
    val audioExtensions = listOf("mp3")
    val photoExtensions = listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
    val rarExtensions = listOf("zip", "7z", "rar")

    val zipManager = remember { ZipManager() }

    var fileList by remember { mutableStateOf<List<ZipEntry>>(emptyList()) }
    var pathInZip by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // 使用 LaunchedEffect 替代手动调用
    LaunchedEffect(zipPath, pathInZip) {
        isLoading = true
        val result = withContext(Dispatchers.IO) {
            zipManager.getFolderContent(zipPath, pathInZip.ifEmpty { null })
        }
        fileList = result
        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(5.dp)
    ) {
        Icon(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxHeight(),
            painter = painterResource(R.drawable.ic_backspace),
            tint = Grey,
            contentDescription = null
        )
        TextButton(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(5.dp),
            onClick = {
                // 返回上一级
                if (pathInZip.isNotEmpty()) {
                    pathInZip = pathInZip.substringBeforeLast("/")
                } else {
                    onClose()
                }
            }
        ) {
            Text(
                text = "Esc",
                color = Grey,
                fontSize = 15.sp,
                modifier = Modifier.fillMaxSize().offset(x = 35.dp)
            )
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(fileList) { file ->
                TextButton(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(5.dp),
                    onClick = {
                        if (file.isDirectory) {
                            pathInZip = if (pathInZip.isEmpty()) file.name else "$pathInZip/${file.name}"
                        } else {
                            // 文件处理逻辑
                            val fileExtension = file.extension
                            if (fileExtension in videoExtensions) {
                                // 播放视频
                            } else if (fileExtension in photoExtensions) {
                                // 查看图片
                            } else if (fileExtension in rarExtensions) {
                                // 打开压缩包
                            }
                        }
                    }
                ) {
                    when {
                        file.isDirectory -> {
                            Icon(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .offset(x = 1.dp),
                                painter = painterResource(R.drawable.ic_folder),
                                tint = Grey,
                                contentDescription = null
                            )
                        }
                        file.extension in videoExtensions -> {
                            Icon(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .offset(x = 1.dp),
                                painter = painterResource(R.drawable.ic_video_file),
                                tint = Grey,
                                contentDescription = null
                            )
                        }
                        file.extension in audioExtensions -> {
                            Icon(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .offset(x = 1.dp),
                                painter = painterResource(R.drawable.ic_audio_file),
                                tint = Grey,
                                contentDescription = null
                            )
                        }
                        file.extension in photoExtensions -> {
                            Icon(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .offset(x = 1.dp),
                                painter = painterResource(R.drawable.ic_photo_file),
                                tint = Grey,
                                contentDescription = null
                            )
                        }
                        file.extension in rarExtensions -> {
                            Icon(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .offset(x = 1.dp),
                                painter = painterResource(R.drawable.ic_rar_file),
                                tint = Grey,
                                contentDescription = null
                            )
                        }
                        else -> {
                            Icon(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .offset(x = 1.dp),
                                painter = painterResource(R.drawable.ic_normal_file),
                                tint = Grey,
                                contentDescription = null
                            )
                        }
                    }
                    Text(
                        text = file.name,
                        color = Grey,
                        fontSize = 15.sp,
                        modifier = Modifier.fillMaxSize().offset(x = 20.dp)
                    )
                }
            }
        }
    }
}
