package com.Fch.vl.view

import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.Fch.vl.view.theme.Black
import com.Fch.vl.view.theme.Grey

@Composable
fun ImageViewer(images: List<String>, currentIndex: Int, onClose: () -> Unit){  //onClose: () -> Unit 一个无参数无返回值的函数参数
    if(images.isEmpty()) onClose()

    //图片处理
    val pagerState = rememberPagerState(
        initialPage = currentIndex,
        pageCount = { images.size }
    )

    // 进入全屏
    val view = LocalView.current
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
    //拦截侧边栏退出手势
    BackHandler {
        onClose()
    }
    Box(modifier = Modifier.fillMaxSize().background(Black).pointerInput(Unit) {
        detectTapGestures{}  //拦截点击
    }){
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ){
                page -> Image(
            painter = rememberAsyncImagePainter(images[page]),  //参数是绝对路径
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        }
        //显示当前第几张
        Text(
            text = "${pagerState.currentPage + 1} / ${images.size}",
            color = Grey,
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .background(Black.copy(alpha = 0.5f))
                .padding(8.dp)
        )
    }
}
