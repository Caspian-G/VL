package com.Fch.vl.view

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.Fch.vl.R
import com.Fch.vl.view.theme.Black
import com.Fch.vl.view.theme.Grey
import com.Fch.vl.view.theme.TransparentGrey
import com.Fch.vl.viewmodel.VlPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun videoFullScreen(vlPlayer: VlPlayer, initVideoSize: String, onClose: () -> Unit, videoPauseButton: Int){
    val view = LocalView.current
    var videoSize by remember { mutableStateOf(initVideoSize) }
    DisposableEffect(Unit) {
        // 隐藏状态栏和导航栏
        val windowInsetsController = view.windowInsetsController
        windowInsetsController?.hide(
            WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
        )
        windowInsetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        onDispose {
            // 退出时恢复
            windowInsetsController?.show(
                WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
            )
        }
    }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var surfaceBoxWidth by remember { mutableStateOf(0) }
    var surfaceBoxHeight by remember { mutableStateOf(0) }
    var isSeeking by remember { mutableStateOf(false) }
    var isShowingFullScreenVideo by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    var isShowingFullScreenVideoButton by remember { mutableStateOf(true) }
    var videoPauseButtonResource by remember { mutableStateOf(videoPauseButton) }
    var zoomRatio by remember { mutableStateOf(2.5f) }
    var isOnDoubleTap by remember { mutableStateOf(false) }
    //记录双击的位置
    var tapOffset by remember { mutableStateOf(Offset.Zero) }
    var speedRatio by remember { mutableStateOf(2.0f) }
    var currentVideoRatio by remember { mutableStateOf(1.0f) }
    //锁定视频
    var fullScreenVideoResource by remember { mutableStateOf(R.drawable.ic_lock_open) }
    var isFullScreenVideoLocked by remember { mutableStateOf(false) }
    //屏幕旋转用
    var activity = LocalContext.current as? Activity

    var dropDownMenuIsExpanded by remember { mutableStateOf(false) }
    var surfaceHolder by remember { mutableStateOf<SurfaceHolder?>(null) }
    LaunchedEffect(isShowingFullScreenVideo) {
        if (isShowingFullScreenVideo) {
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
    LaunchedEffect(currentVideoRatio){
        vlPlayer.setSpeed(currentVideoRatio)
    }
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
    //暂停键图标复位
    LaunchedEffect(videoPauseButtonResource){
        if(videoPauseButtonResource == R.drawable.ic_video) isPlaying = true
        else isPlaying = false
    }
    //拦截侧边栏退出手势
    BackHandler {
        vlPlayer.playingButtonState = videoPauseButtonResource
        vlPlayer.setSurface(null)
        isShowingFullScreenVideo = false
        vlPlayer.setSpeed(1.0f)
        onClose()
    }
    DisposableEffect(Unit) {
        val originalCallback = vlPlayer.onVideoSizeChanged
        vlPlayer.onVideoSizeChanged = { size ->
            videoSize = size
            originalCallback?.invoke(size)
        }
        onDispose {
            vlPlayer.onVideoSizeChanged = originalCallback
        }
    }
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Black)
                .onGloballyPositioned { coordinates ->
                    surfaceBoxHeight = coordinates.size.height
                    surfaceBoxWidth = coordinates.size.width
                }
                .pointerInput(Unit){
                    detectTapGestures(
                        onTap = {
                            isShowingFullScreenVideoButton = !isShowingFullScreenVideoButton
                        },
                        onDoubleTap = {
                                offset -> tapOffset = offset
                            isOnDoubleTap = !isOnDoubleTap
                        },
                    )
                }
                .pointerInput(Unit) {
                    if(!isFullScreenVideoLocked){
                        awaitEachGesture{
                            val down = awaitFirstDown()
                            var dragEvent: PointerInputChange? = null
                            try {  //等待长按
                                dragEvent = withTimeout(viewConfiguration.longPressTimeoutMillis) {
                                    waitForUpOrCancellation()
                                }
                            } catch (e: PointerEventTimeoutCancellationException) {  //捕获长按
                                vlPlayer.setSpeed(speedRatio)
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.changes.any { it.changedToUp() }) {  //长按取消
                                        vlPlayer.setSpeed(currentVideoRatio)
                                        break
                                    }
                                }
                            }
                        }
                    }
                }.clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            val scale by animateFloatAsState(if (isOnDoubleTap) zoomRatio else 1f, label = "scale")
            var lastSettledOrigin by remember { mutableStateOf(TransformOrigin.Center) }
            LaunchedEffect(isOnDoubleTap) {
                if (isOnDoubleTap) {
                    lastSettledOrigin = TransformOrigin(
                        tapOffset.x / surfaceBoxWidth.coerceAtLeast(1),
                        tapOffset.y / surfaceBoxHeight.coerceAtLeast(1)
                    )
                }
            }

            // 新增：解析视频比例
            val videoAspectRatio = remember(videoSize) {
                val size = videoSize.split("x")
                if (size.size == 2) {
                    val w = size[0].toFloatOrNull() ?: 1f
                    val h = size[1].toFloatOrNull() ?: 1f
                    if (h != 0f) w / h else 1f
                } else 1f
            }

            AndroidView(
                factory = { context ->
                    SurfaceView(context).apply {
                        holder.addCallback(object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                surfaceHolder = holder
                                vlPlayer.setSurface(holder.surface)
                            }
                            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                                surfaceHolder = null
                                vlPlayer.setSurface(null)
                            }
                        })
                    }
                },
                modifier = Modifier
                    .aspectRatio(videoAspectRatio)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = lastSettledOrigin
                    }
            )
        }
        if (isShowingFullScreenVideoButton) {
            //Close
            IconButton(
                modifier = Modifier.align(Alignment.TopEnd).width(30.dp).height(30.dp),
                onClick = {
                    vlPlayer.playingButtonState = videoPauseButtonResource
                    vlPlayer.setSurface(null)
                    isShowingFullScreenVideo = false
                    vlPlayer.setSpeed(1.0f)
                    onClose()
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_cancel),
                    tint = Grey,
                    contentDescription = null
                )
            }
            //Pause
            IconButton(
                modifier = Modifier
                    .height(30.dp)
                    .width(30.dp)
                    .align(Alignment.Center),
                onClick = {
                    if(!isFullScreenVideoLocked){
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
            if(!isFullScreenVideoLocked){
                Box(
                    modifier = Modifier.width(70.dp).height(20.dp).align(Alignment.BottomStart).offset(x = 8.dp, y = -30.dp)
                ) {
                    Text(
                        modifier = Modifier.align(Alignment.BottomStart),
                        color = Grey,
                        text = formatTime(currentPosition),
                        fontSize = 12.sp
                    )
                    Text(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        color = Grey,
                        text = ":",
                        fontSize = 12.sp
                    )
                    Text(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        color = Grey,
                        text = formatTime(duration),
                        fontSize = 12.sp
                    )
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
            IconButton(
                modifier = Modifier
                    .height(30.dp)
                    .width(30.dp)
                    .align(Alignment.CenterEnd)
                    .offset(y = -45.dp),
                onClick = {
                    if(!isFullScreenVideoLocked) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                }
            ){
                Icon(
                    painter = painterResource(R.drawable.ic_rotate_anticlockwise),
                    tint = Grey,
                    contentDescription = null
                )
            }
            IconButton(
                modifier = Modifier
                    .height(30.dp)
                    .width(30.dp)
                    .align(Alignment.CenterEnd),
                onClick = {
                    if(!isFullScreenVideoLocked) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                }
            ){
                Icon(
                    painter = painterResource(R.drawable.ic_rotate_by_phone),
                    tint = Grey,
                    contentDescription = null
                )
            }
            IconButton(
                modifier = Modifier
                    .height(30.dp)
                    .width(30.dp)
                    .align(Alignment.CenterEnd)
                    .offset(y = 45.dp),
                onClick = {
                    if(!isFullScreenVideoLocked) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            ){
                Icon(
                    painter = painterResource(R.drawable.ic_rotate_clockwise),
                    tint = Grey,
                    contentDescription = null
                )
            }
            if(!isFullScreenVideoLocked){
                Button(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .height(40.dp)
                        .width(60.dp)
                        .offset(x = -20.dp ,y = -40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TransparentGrey),
                    onClick = {
                        dropDownMenuIsExpanded = true
                    }
                ){
                    Text(text = "S", color = Grey)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(y = -30.dp)
                ){
                    DropdownMenu(
                        expanded = dropDownMenuIsExpanded,
                        onDismissRequest = { dropDownMenuIsExpanded = false },
                    ){
                        DropdownMenuItem(
                            text = { Text(text = "x4.0", color = Grey) },
                            onClick = {
                                vlPlayer.setSpeed(4.0f)
                                dropDownMenuIsExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = "x3.0", color = Grey) },
                            onClick = {
                                vlPlayer.setSpeed(3.0f)
                                dropDownMenuIsExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = "x2.0", color = Grey) },
                            onClick = {
                                vlPlayer.setSpeed(2.0f)
                                dropDownMenuIsExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = "x1.5", color = Grey) },
                            onClick = {
                                vlPlayer.setSpeed(1.5f)
                                dropDownMenuIsExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = "x1.0", color = Grey) },
                            onClick = {
                                vlPlayer.setSpeed(1.0f)
                                dropDownMenuIsExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = "x0.5", color = Grey) },
                            onClick = {
                                vlPlayer.setSpeed(0.5f)
                                dropDownMenuIsExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = "x0.25", color = Grey) },
                            onClick = {
                                vlPlayer.setSpeed(0.25f)
                                dropDownMenuIsExpanded = false
                            },
                        )
                    }
                }
            }
            IconButton(
                modifier = Modifier
                    .height(30.dp)
                    .width(30.dp)
                    .align(Alignment.CenterStart),
                onClick = {
                    if(fullScreenVideoResource == R.drawable.ic_lock_open){
                        isFullScreenVideoLocked = true
                        fullScreenVideoResource = R.drawable.ic_lock_close
                    }else{
                        isFullScreenVideoLocked = false
                        fullScreenVideoResource = R.drawable.ic_lock_open
                    }
                }
            ){
                Icon(
                    painter = painterResource(fullScreenVideoResource),
                    tint = Grey,
                    contentDescription = null
                )
            }
        }
    }
}
