package com.Fch.vl.view

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.Fch.vl.view.theme.VLTheme
import java.io.File
import com.Fch.vl.R
import com.Fch.vl.view.theme.*
import android.provider.Settings
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.io.extension
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.collectAsState
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.Fch.vl.viewmodel.NetManager
import com.Fch.vl.viewmodel.VlPlayer
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

//数据持久化
val Context.ipPortDataStore by preferencesDataStore(name = "IpPort")
object Keys {  //简化存取代码
    val ipPort = stringPreferencesKey("IpPort")  //定义钥匙
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ActivityMain()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                100
            )
        }
    }
}

//预览用
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    VLTheme {
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityMain() {
    //Toast
    val context = LocalContext.current
    //select_list是否展开
    var expanded by remember { mutableStateOf(false) }
    var isLocalFolderVisible by remember { mutableStateOf(false) }
    var isNetFolderVisible by remember { mutableStateOf(false) }
    //setting按钮旋转
    var rotationAngle by remember { mutableStateOf(0f) }
    val rotation by animateFloatAsState(
        targetValue = rotationAngle,
        animationSpec = tween(durationMillis = 300)
    )
    //当前本地文件夹的路径
    var currentLocalPath by remember { mutableStateOf("/storage/emulated/0") }
    var fileList by remember { mutableStateOf(emptyList<File>()) }
    //监听当前本地文件夹路径
    LaunchedEffect(currentLocalPath) {
        val folder = File(currentLocalPath)
        if (folder.exists() && folder.isDirectory) {
            // 获取文件夹里所有内容 文件夹排在前面 并按名称排序
            fileList = folder.listFiles()?.sortedWith(
                compareBy(
                    { !it.isDirectory },  // 文件夹在前
                    { it.name }  // 按名称
                )
            )?.toList() ?: emptyList()
        } else {
            fileList = emptyList()
        }
    }
    //当前网络文件夹的路径
    var currentNetPath by remember { mutableStateOf("") }
    var isShowingDialog by remember { mutableStateOf(false) }
    val lastIpPort by context.ipPortDataStore.data.map { it[Keys.ipPort] ?: "" }.collectAsState(initial = "")
    var textFieldValue by remember(key1 = lastIpPort) { mutableStateOf(lastIpPort) }
    var netFileList by remember { mutableStateOf(emptyList<String>()) }
    val netManager = remember { NetManager() }
    LaunchedEffect(currentNetPath) {
        if(!textFieldValue.isEmpty()) netFileList = netManager.getHTTPFileList(textFieldValue, currentNetPath).sortedWith(compareBy({!it.endsWith(".directory")}, {it.lowercase()}))
    }
    //视频扩展名
    val videoExtensions = listOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "3gp")
    val audioExtensions = listOf("mp3", "wav", "flac", "aac", "ogg")
    val photoExtensions = listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
    val rarExtensions = listOf("zip", "7z", "rar")
    //包裹surface的box的width
    var surfaceBoxWidth by remember { mutableStateOf(0) }
    var surfaceBoxHeight by remember { mutableStateOf(0) }
    var realSurfaceBoxHeight by remember { mutableStateOf(200.dp) }
    var isVideoExpanded by remember{mutableStateOf(false)}
    //播放器 确保屏幕翻转activity销毁不会导致vlplayer销毁
    val vlPlayer: VlPlayer = viewModel()
    //打开或关闭Surface
    var isShowingSurface by remember { mutableStateOf(false) }
    var videoSize by remember { mutableStateOf("0x0") }
    var videoPauseButtonResource by remember { mutableStateOf(R.drawable.ic_video) }
    var isShowingSurfaceButton by remember { mutableStateOf(true) }
    LaunchedEffect(isShowingSurfaceButton) {
        if(isShowingSurfaceButton) {
            delay(4000)
            isShowingSurfaceButton = false
        }
    }
    //用于视频进度条
    var isPlaying by remember { mutableStateOf(true) }
    //暂停键图标复位
    LaunchedEffect(videoPauseButtonResource){
        if(videoPauseButtonResource == R.drawable.ic_video) isPlaying = true
        else isPlaying = false
    }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        vlPlayer.onVideoSizeChanged = { size ->
            videoSize = size
        }
        onDispose { vlPlayer.onVideoSizeChanged = null }
    }
    // 定时更新进度条
    LaunchedEffect(isShowingSurface) {
        if (isShowingSurface) {
            while (true) {
                if (!isSeeking) {
                    val pos = vlPlayer.getCurrentPosition()
                    // 只有位置发生变化才更新 Compose 状态 减少重绘
                    if (pos >= 0 && pos != currentPosition) {
                        currentPosition = pos
                    }
                    val d = vlPlayer.getDuration()
                    if (d > 0 && d != duration) {   // 只要新值有效且不同就更新
                        duration = d
                    }
                }
                delay(if (isPlaying) 500 else 2000) // 暂停时每2秒才扫一次 几乎不占资源
            }
        }
    }
    //格式化时间
    fun formatTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = millis / (1000 * 60 * 60)
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
    //锁住屏幕
    var notFullScreenVideoLockResource:Int by remember { mutableStateOf(R.drawable.ic_lock_open) }
    var isNotFullScreenVideoLocked by remember { mutableStateOf(false) }
    //视频全屏
    var isShowingFullScreenVideo by remember { mutableStateOf(false) }
    var mainSurfaceHolder by remember { mutableStateOf<SurfaceHolder?>(null) }
    var shouldRebindSurface by remember { mutableStateOf(false) }
    LaunchedEffect(shouldRebindSurface) {
        if (shouldRebindSurface) {
            //等待一帧 确保视图已更新
            delay(50)
            mainSurfaceHolder?.surface?.let { surface ->
                if (surface.isValid) {
                    vlPlayer.setSurface(surface)
                }
            }
            shouldRebindSurface = false
        }
    }

    //协程异步等待使用
    val scope = rememberCoroutineScope()

    //图片处理
    var isShowingImage by remember { mutableStateOf(false) }
    var currentLocalImageIndex: Int by  remember { mutableStateOf(0) }
    var currentLocalImageList by remember { mutableStateOf(emptyList<String>()) }

    BackHandler {
        if(isLocalFolderVisible && isVideoExpanded){
            realSurfaceBoxHeight = 200.dp
            isLocalFolderVisible = false
        }else if(isLocalFolderVisible){
            if (!currentLocalPath.equals("/storage/emulated/0")) currentLocalPath =
                currentLocalPath.take(currentLocalPath.lastIndexOf("/"))
        }else if(isNetFolderVisible){
            scope.launch {
                netFileList = netManager.getHTTPFileList(textFieldValue, "esc_this_directory").sortedWith(compareBy({!it.endsWith(".directory")}, {it.lowercase()}))
            }
        }
    }

    //zippreview
    var isShowingZipViewer by remember { mutableStateOf(false) }
    var currentLocalZipPath by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()){
        Column{
            //标题栏
            Box(
                modifier = Modifier
                    .systemBarsPadding()
                    .fillMaxWidth()
                    .height(30.dp)
                    .padding(5.dp)
            ){
                Text(text = "VL", modifier = Modifier.align(Alignment.TopStart), color = Grey)
                IconButton(
                    onClick = {
                        rotationAngle += 180f
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .width(20.dp)
                        .graphicsLayer {
                            rotationZ = rotation
                        }
                ){
                    Icon(
                        painter = painterResource(id = R.drawable.ic_setting),
                        tint = Grey,
                        contentDescription = null
                    )
                }
            }
            //搜索栏
            Box(
                modifier = Modifier
                    .systemBarsPadding()
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(Grey)
            ){
                Box(modifier = Modifier.fillMaxSize().padding(5.dp)){
                    //search
                    IconButton(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .width(20.dp),
                        onClick = {

                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_search),
                            tint = White,
                            contentDescription = null,
                        )
                    }
                    //select
                    IconButton(
                        onClick = { expanded = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .width(20.dp)
                    ){
                        Icon(
                            painter = painterResource(id = R.drawable.ic_select_list),
                            tint = White,
                            contentDescription = null
                        )
                    }
                    Box(
                        modifier = Modifier
                            .offset(x = 380.dp, y = 20.dp)
                    ){
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ){
                            //select 1
                            DropdownMenuItem(
                                text = { Text(text = "本地", color = Grey) },
                                onClick = {
                                    isLocalFolderVisible = true
                                    isNetFolderVisible = false
                                    expanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("联网", color = Grey) },
                                onClick = {
                                    isNetFolderVisible = true
                                    isLocalFolderVisible = false
                                    isShowingDialog = true
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
            Column{
                if(isShowingSurface){
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(realSurfaceBoxHeight)
                            .background(Black)
                            .onGloballyPositioned { coordinates ->
                                surfaceBoxWidth = coordinates.size.width  //获取Box宽度
                                surfaceBoxHeight = coordinates.size.height
                            },
                        contentAlignment = Alignment.Center
                    ){
                        AndroidView(
                            factory = { context ->
                                SurfaceView(context).apply {
                                    holder.addCallback(object : SurfaceHolder.Callback {
                                        override fun surfaceCreated(holder: SurfaceHolder) {
                                            mainSurfaceHolder = holder
                                            //仅set一次 绑定surface和cpp中window
                                            if (!isShowingFullScreenVideo) {
                                                vlPlayer.setSurface(holder.surface)
                                            }
                                        }
                                        override fun surfaceChanged(
                                            holder: SurfaceHolder,
                                            format: Int,
                                            width: Int,
                                            height: Int
                                        ) {
                                        }
                                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                                            if (!isShowingFullScreenVideo) {
                                                mainSurfaceHolder = null
                                                vlPlayer.setSurface(null)
                                            }
                                        }
                                    })
                                }
                            } ,
                            update = { view ->
                                val videoSizeTrigger = videoSize
                                if (videoSize != "0x0") {
                                    val size = videoSize.split("x")
                                    val vWidth = size[0].toInt()
                                    val vHeight = size[1].toInt()
                                    val (targetWidth, targetHeight) = if (vHeight > vWidth) {
                                        (vWidth * surfaceBoxHeight / vHeight) to surfaceBoxHeight
                                    } else {
                                        surfaceBoxWidth to (vHeight * surfaceBoxWidth / vWidth)
                                    }
                                    view.holder.setFixedSize(targetWidth, targetHeight)
                                }
                            }
                        )
                        Button(
                            modifier = Modifier.fillMaxSize(),
                            colors = ButtonDefaults.buttonColors(containerColor = Transparent),
                            shape = RectangleShape,
                            onClick = {
                                isShowingSurfaceButton = true
                            }
                        ){}
                        if(isShowingSurfaceButton){
                            //VideoPause
                            IconButton(
                                modifier = Modifier
                                    .height(30.dp)
                                    .width(30.dp)
                                    .align(Alignment.Center),
                                onClick = {if(!isNotFullScreenVideoLocked){
                                    if(isPlaying){
                                        vlPlayer.pauseVideo()
                                        videoPauseButtonResource = R.drawable.ic_pause
                                        isPlaying = false
                                    }else{
                                        vlPlayer.resumeVideo()
                                        videoPauseButtonResource = R.drawable.ic_video
                                        isPlaying = true
                                    }
                                }
                                }
                            ){
                                Icon(
                                    painter = painterResource(videoPauseButtonResource),
                                    tint = Grey,
                                    contentDescription = null
                                )
                            }
                            //VideoOff
                            IconButton(
                                modifier = Modifier
                                    .height(30.dp)
                                    .width(30.dp)
                                    .align(Alignment.TopStart),
                                onClick = {
                                    isShowingSurface = false
                                    vlPlayer.releaseVideo()
                                }
                            ){
                                Icon(
                                    painter = painterResource(R.drawable.ic_cancel),
                                    tint = Grey,
                                    contentDescription = null
                                )
                            }
                            //VideoFullScreen
                            IconButton(
                                modifier = Modifier
                                    .height(30.dp)
                                    .width(30.dp)
                                    .align(Alignment.TopEnd),
                                onClick = {
                                    if(!isNotFullScreenVideoLocked) isShowingFullScreenVideo = true
                                }
                            ){
                                Icon(
                                    painter = painterResource(R.drawable.ic_fullscreen),
                                    tint = Grey,
                                    contentDescription = null
                                )
                            }
                            //VideoExpand
                            IconButton(
                                modifier = Modifier
                                    .height(30.dp)
                                    .width(30.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(y = -25.dp),
                                onClick = {
                                    if(!isNotFullScreenVideoLocked){
                                        if(isVideoExpanded){
                                            realSurfaceBoxHeight = 200.dp
                                            isVideoExpanded = false
                                        }else{
                                            realSurfaceBoxHeight = 800.dp
                                            isVideoExpanded = true

                                        }
                                    }
                                }
                            ){
                                Icon(
                                    painter = painterResource(R.drawable.ic_fit_screen),
                                    tint = Grey,
                                    contentDescription = null
                                )
                            }
                            //VideoLock
                            IconButton(
                                modifier = Modifier
                                    .height(30.dp)
                                    .width(30.dp)
                                    .align(Alignment.CenterStart),
                                onClick = {
                                    if(notFullScreenVideoLockResource == R.drawable.ic_lock_open){
                                        isNotFullScreenVideoLocked = true
                                        notFullScreenVideoLockResource = R.drawable.ic_lock_close
                                    }else{
                                        isNotFullScreenVideoLocked = false
                                        notFullScreenVideoLockResource = R.drawable.ic_lock_open
                                    }
                                }
                            ){
                                Icon(
                                    painter = painterResource(notFullScreenVideoLockResource),
                                    tint = Grey,
                                    contentDescription = null
                                )
                            }
                            if(!isNotFullScreenVideoLocked){
                                Box(modifier = Modifier.width(100.dp).height(20.dp).align(Alignment.BottomStart).offset(x = 8.dp, y = -30.dp)) {
                                    Row {
                                        Text(
                                            color = Grey,
                                            text = formatTime(currentPosition),
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            color = Grey,
                                            text = " : ",
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            color = Grey,
                                            text = formatTime(duration),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                Slider(
                                    value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                                    onValueChange = { progress ->
                                        isSeeking = true
                                        currentPosition = (progress * duration).toLong()
                                    },
                                    onValueChangeFinished = {
                                        scope.launch {
                                            vlPlayer.setPosition(currentPosition)
                                            isSeeking = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart).offset(y = 10.dp),
                                    track = { sliderState ->
                                        SliderDefaults.Track(
                                            colors = SliderDefaults.colors(
                                                activeTrackColor = Grey,
                                                inactiveTrackColor = Grey.copy(alpha = 0.3f)
                                            ),
                                            sliderState = sliderState,
                                            modifier = Modifier.height(3.dp)
                                        )
                                    },
                                    thumb = {
                                        Box(
                                            modifier = Modifier
                                                .width(10.dp)
                                                .height(5.dp)
                                                .background(Grey, RectangleShape)
                                                .align(Alignment.Center)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                Column {
                    //本地文件目录Esc
                    if(isLocalFolderVisible) {
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
                                    if (!currentLocalPath.equals("/storage/emulated/0")) currentLocalPath =
                                        currentLocalPath.take(currentLocalPath.lastIndexOf("/"))
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
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            items(fileList, key = {it}) { file ->
                                TextButton(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(5.dp),
                                    onClick = {
                                        if (file.isDirectory) {
                                            currentLocalPath = file.absolutePath
                                        } else {
                                            //文件处理逻辑
                                            val fileExtension = file.extension
                                            if (fileExtension in videoExtensions) {
                                                isShowingSurface = true
                                                videoSize = "0x0"
                                                vlPlayer.playingVideo(file.absolutePath)
                                            }else if(fileExtension in photoExtensions){
                                                val folder = File(currentLocalPath)
                                                val imageFiles = folder.listFiles()?.filter {
                                                    !it.isDirectory && it.extension.lowercase() in photoExtensions
                                                } ?.sortedBy { it.name } //按名称排序
                                                currentLocalImageList = imageFiles?.map { it.absolutePath } ?:emptyList()
                                                currentLocalImageIndex = imageFiles?.indexOf(file) ?: 0
                                                isShowingImage = true
                                            }else if(fileExtension in rarExtensions){
                                                isShowingZipViewer = true
                                                currentLocalZipPath = file.absolutePath
                                            }
                                        }
                                    }
                                ) {
                                    if (file.isDirectory) {
                                        Icon(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .offset(x = 1.dp),
                                            painter = painterResource(R.drawable.ic_folder),
                                            tint = Grey,
                                            contentDescription = null
                                        )
                                    }else if(file.extension in videoExtensions){
                                        Icon(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .offset(x = 1.dp),
                                            painter = painterResource(R.drawable.ic_video_file),
                                            tint = Grey,
                                            contentDescription = null
                                        )
                                    }else if(file.extension in audioExtensions){
                                        Icon(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .offset(x = 1.dp),
                                            painter = painterResource(R.drawable.ic_audio_file),
                                            tint = Grey,
                                            contentDescription = null
                                        )
                                    }else if(file.extension in photoExtensions){
                                        Icon(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .offset(x = 1.dp),
                                            painter = painterResource(R.drawable.ic_photo_file),
                                            tint = Grey,
                                            contentDescription = null
                                        )
                                    }else if(file.extension in rarExtensions){
                                        Icon(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .offset(x = 1.dp),
                                            painter = painterResource(R.drawable.ic_rar_file),
                                            tint = Grey,
                                            contentDescription = null
                                        )
                                    }else{
                                        Icon(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .offset(x = 1.dp),
                                            painter = painterResource(R.drawable.ic_normal_file),
                                            tint = Grey,
                                            contentDescription = null
                                        )
                                    }
                                    Text(
                                        text = if (file.isDirectory) "${file.name}" else "${file.name}",
                                        color = Grey,
                                        fontSize = 15.sp,
                                        modifier = Modifier.fillMaxSize().offset(x = 20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Column {
                    //网络文件目录Esc
                    if(isNetFolderVisible) {
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
                                    scope.launch {
                                        netFileList = netManager.getHTTPFileList(textFieldValue, "esc_this_directory").sortedWith(compareBy({!it.endsWith(".directory")}, {it.lowercase()}))
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
                        if(netFileList.isNotEmpty() && netFileList != listOf("")){
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(items = netFileList, key = {it}) { file ->
                                    TextButton(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(5.dp),
                                        onClick = {
                                            if (file.endsWith(".directory")) {
                                                currentNetPath = file.substringBeforeLast(".")
                                            } else {
                                                //文件处理逻辑
                                                if (file.endsWith(".video")) {
                                                    scope.launch {
                                                        isShowingSurface = true
                                                        videoSize = "0x0"
                                                        vlPlayer.playingVideo(netManager.getHTTPCurPath(textFieldValue) + file.substringBeforeLast(".") )
                                                    }
                                                }else if(file.endsWith(".photo")){
//                                                val folder = File(currentLocalPath)
//                                                val imageFiles = folder.listFiles()?.filter {
//                                                    !it.isDirectory && it.extension.lowercase() in photoExtensions
//                                                } ?.sortedBy { it.name } //按名称排序
//                                                currentLocalImageList = imageFiles?.map { it.absolutePath } ?:emptyList()
//                                                currentLocalImageIndex = imageFiles?.indexOf(file) ?: 0
//                                                isShowingImage = true
                                                }else if(file.endsWith(".audio")){
                                                    //
                                                }else if(file.endsWith(".rar")){
//                                                isShowingZipViewer = true
//                                                currentLocalZipPath = file.absolutePath
                                                }
                                            }
                                        }
                                    ) {
                                        if (file.endsWith(".directory")) {
                                            Icon(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .offset(x = 1.dp),
                                                painter = painterResource(R.drawable.ic_folder),
                                                tint = Grey,
                                                contentDescription = null
                                            )
                                        }else if(file.endsWith(".video")){
                                            Icon(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .offset(x = 1.dp),
                                                painter = painterResource(R.drawable.ic_video_file),
                                                tint = Grey,
                                                contentDescription = null
                                            )
                                        }else if(file.endsWith(".audio")){
                                            Icon(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .offset(x = 1.dp),
                                                painter = painterResource(R.drawable.ic_audio_file),
                                                tint = Grey,
                                                contentDescription = null
                                            )
                                        }else if(file.endsWith(".photo")){
                                            Icon(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .offset(x = 1.dp),
                                                painter = painterResource(R.drawable.ic_photo_file),
                                                tint = Grey,
                                                contentDescription = null
                                            )
                                        }else if(file.endsWith(".rar")){
                                            Icon(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .offset(x = 1.dp),
                                                painter = painterResource(R.drawable.ic_rar_file),
                                                tint = Grey,
                                                contentDescription = null
                                            )
                                        }else{
                                            Icon(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .offset(x = 1.dp),
                                                painter = painterResource(R.drawable.ic_normal_file),
                                                tint = Grey,
                                                contentDescription = null
                                            )
                                        }
                                        Text(
                                            text = file.substringBeforeLast("."),
                                            color = Grey,
                                            fontSize = 15.sp,
                                            modifier = Modifier.fillMaxSize().offset(x = 20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                //IP输入
                if(isShowingDialog){
                    AlertDialog(
                        onDismissRequest = { isShowingDialog = false },
                        title = { Text("") },
                        text = {
                            OutlinedTextField(
                                value = textFieldValue,
                                onValueChange = { textFieldValue = it },
                                label = { Text("IP:Port") },
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        if (Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{1,5}$").matches(
                                                textFieldValue
                                            )
                                        ) {
                                            try{
                                                //强制IO线程执行
                                                netFileList = withContext(Dispatchers.IO) {
                                                    netManager.getHTTPFileList(textFieldValue, currentNetPath).sortedWith(compareBy({!it.endsWith(".directory")}, {it.lowercase()}))
                                                }
                                                isShowingDialog = false
                                                context.ipPortDataStore.edit { preferences ->
                                                    preferences[Keys.ipPort] = textFieldValue
                                                }
                                            }catch (e: Exception){
                                                //http访问失败
                                                Toast.makeText(context, "Null", Toast.LENGTH_SHORT).show()
                                            }
                                        } else Toast.makeText(context, "Null", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text("comfirm")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { isShowingDialog = false }) {
                                Text("cancel")
                            }
                        }
                    )
                    if(isShowingZipViewer){
                        zipViewer(currentLocalZipPath, onClose = {})
                    }
                }
            }
        }
        if(isShowingImage){
            ImageViewer(currentLocalImageList, currentLocalImageIndex, onClose = {isShowingImage = false})
        }
        if(isShowingFullScreenVideo){
            videoFullScreen(vlPlayer, videoSize, onClose = { isShowingFullScreenVideo = false; shouldRebindSurface = true; videoPauseButtonResource = vlPlayer.playingButtonState; videoSize = "0x0";}, videoPauseButtonResource)
        }
    }
}
