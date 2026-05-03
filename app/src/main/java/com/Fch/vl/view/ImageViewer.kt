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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.Fch.vl.view.theme.Black
import com.Fch.vl.view.theme.Grey

@Composable
fun ImageViewer(isLocalMode: Boolean, images: List<String>, currentIndex: Int, onClose: () -> Unit) {
    if (images.isEmpty()) onClose()
    val pagerState = rememberPagerState(
        initialPage = currentIndex,
        pageCount = { images.size }
    )
    val view = LocalView.current
    DisposableEffect(Unit) {
        val windowInsetsController = view.windowInsetsController
        windowInsetsController?.hide(
            WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
        )
        windowInsetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            windowInsetsController?.show(
                WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
            )
        }
    }
    val scaleStates = remember(images) {
        images.indices.map { mutableStateOf(ScaleState()) }
    }
    BackHandler {
        onClose()
    }
    Box(modifier = Modifier.fillMaxSize().background(Black)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val scaleState = scaleStates[page]
            var zoom by remember { mutableStateOf(scaleState.value.zoom) }
            var offsetX by remember { mutableStateOf(scaleState.value.offsetX) }
            var offsetY by remember { mutableStateOf(scaleState.value.offsetY) }

            LaunchedEffect(zoom, offsetX, offsetY) {
                scaleState.value = ScaleState(zoom, offsetX, offsetY)
            }
            var containerSize by remember { mutableStateOf(Offset.Zero) }
            val imageModifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    containerSize = Offset(
                        coordinates.size.width.toFloat(),
                        coordinates.size.height.toFloat()
                    )
                }
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    translationX = offsetX
                    translationY = offsetY
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            if (zoom > 1f) {
                                zoom = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                val newZoom = 3f
                                val centerX = containerSize.x / 2
                                val centerY = containerSize.y / 2
                                offsetX = (centerX - tapOffset.x) * (newZoom - 1)
                                offsetY = (centerY - tapOffset.y) * (newZoom - 1)

                                val maxOffsetX = (containerSize.x * (newZoom - 1) / 2)
                                val maxOffsetY = (containerSize.y * (newZoom - 1) / 2)
                                offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
                                offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
                                zoom = newZoom
                            }
                        }
                    )
                }
            if (isLocalMode) {
                Image(
                    painter = rememberAsyncImagePainter(images[page]),
                    contentDescription = null,
                    modifier = imageModifier,
                    contentScale = ContentScale.Fit
                )
            } else {
                AsyncImage(
                    model = images[page],
                    contentDescription = null,
                    modifier = imageModifier,
                    contentScale = ContentScale.Fit
                )
            }
        }
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
data class ScaleState(
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)
