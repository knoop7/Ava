package com.example.ava.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import android.graphics.BitmapFactory
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.res.Configuration

/**
 * Glass Music Player - A hand-crafted Compose UI inspired by iOS design.
 * 
 * Features:
 * - Blurred background with playing state transition
 * - Noise texture overlay
 * - Ambient glow breathing animation
 * - Live indicator
 * - Vignette effect
 * - Shimmer text effect
 * - Glass-style control buttons (Squircle)
 * - Progress bar with glow
 * - Time capsules
 */
@Composable
fun GlassMusicPlayerView(
    coverUrl: String?,
    coverBitmap: Bitmap?,
    songTitle: String,
    artistName: String,
    isPlaying: Boolean,
    volumeLevel: Float = 1.0f,
    repeatMode: String = "off",
    shuffleEnabled: Boolean = false,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onVolumeChange: (Float) -> Unit = {},
    onRepeatClick: () -> Unit = {},
    onShuffleClick: () -> Unit = {},
    onBackgroundClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )
    
    // Background blur/opacity transition based on playing state
    val targetBlur = if (isPlaying) 0.dp else 20.dp
    val targetOpacity = if (isPlaying) 0.7f else 0.4f
    
    val backgroundBlur by animateDpAsState(targetValue = targetBlur, animationSpec = tween(800), label = "blur")
    val backgroundOpacity by animateFloatAsState(targetValue = targetOpacity, animationSpec = tween(800), label = "opacity")
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { },
                onLongClick = onBackgroundClick
            )
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(1.2f)
                .alpha(backgroundOpacity)
                .blur(backgroundBlur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        ) {
            if (coverBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = coverBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF111111)))
            }
        }


        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(glowScale)
                .alpha(glowAlpha)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    radius = size.minDimension * 0.8f
                ),
                radius = size.minDimension * 0.8f,
                center = center
            )
        }


        NoiseOverlay(modifier = Modifier.fillMaxSize().alpha(0.08f))


        Vignette(modifier = Modifier.fillMaxSize())


        val context = LocalContext.current
        val haLogoBitmap = remember {
            try {
                context.assets.open("ha_logo.png").use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: Exception) {
                null
            }
        }
        
        haLogoBitmap?.let { bitmap ->
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Home Assistant",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 32.dp, end = 32.dp)
                    .size(48.dp)
                    .alpha(0.25f)
            )
        }



        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        


        val horizontalPadding = if (isLandscape) 80.dp else 40.dp
        val topPadding = if (isLandscape) 60.dp else 80.dp
        val bottomPadding = if (isLandscape) 40.dp else 60.dp
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = horizontalPadding, end = horizontalPadding, top = topPadding, bottom = bottomPadding),
            verticalArrangement = Arrangement.Bottom
        ) {

            Column(modifier = Modifier.padding(bottom = 10.dp)) {
                ShimmerText(
                    text = songTitle,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                
                Column {
                    Text(
                        text = artistName,
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(2.dp)
                            .background(
                                Color.White.copy(alpha = 0.3f),
                                RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
            
            
            Spacer(modifier = Modifier.height(20.dp))


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isLandscape) Arrangement.Center else Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {

                ControlButton(onClick = onShuffleClick) {
                    IconWithShadow(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleEnabled) Color.White else Color.White.copy(alpha = 0.3f),
                        size = if (isLandscape) 28.dp else 24.dp
                    )
                }
                
                if (isLandscape) Spacer(modifier = Modifier.width(40.dp))
                

                ControlButton(onClick = onPreviousClick) {
                    IconWithShadow(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White.copy(alpha = 0.5f),
                        size = if (isLandscape) 36.dp else 30.dp
                    )
                }
                
                if (isLandscape) Spacer(modifier = Modifier.width(40.dp))
                
                // Play/Pause
                GlassPlayButton(
                    isPlaying = isPlaying,
                    onClick = onPlayPauseClick
                )
                
                if (isLandscape) Spacer(modifier = Modifier.width(40.dp))
                

                ControlButton(onClick = onNextClick) {
                    IconWithShadow(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White.copy(alpha = 0.5f),
                        size = if (isLandscape) 36.dp else 30.dp
                    )
                }
                
                if (isLandscape) Spacer(modifier = Modifier.width(40.dp))
                

                ControlButton(onClick = onRepeatClick) {
                    when (repeatMode) {
                        "one" -> IconWithShadow(
                            imageVector = Icons.Filled.RepeatOne,
                            contentDescription = "Repeat One",
                            tint = Color.White,
                            size = if (isLandscape) 28.dp else 24.dp
                        )
                        "all" -> IconWithShadow(
                            imageVector = Icons.Filled.Repeat,
                            contentDescription = "Repeat All",
                            tint = Color.White,
                            size = if (isLandscape) 28.dp else 24.dp
                        )
                        else -> IconWithShadow(
                            imageVector = Icons.Filled.Repeat,
                            contentDescription = "Repeat Off",
                            tint = Color.White.copy(alpha = 0.3f),
                            size = if (isLandscape) 28.dp else 24.dp
                        )
                    }
                }
            }
        }
    }
}



@Composable
private fun IconWithShadow(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    size: androidx.compose.ui.unit.Dp
) {
    Box {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = Color.Black.copy(alpha = 0.15f),
            modifier = Modifier
                .size(size)
                .offset(x = 0.dp, y = 3.dp)
                .blur(3.dp)
        )
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = Color.Black.copy(alpha = 0.2f),
            modifier = Modifier
                .size(size)
                .offset(x = 0.dp, y = 2.dp)
                .blur(2.dp)
        )
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size)
        )
    }
}

@Composable
private fun ShimmerText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 42.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.5).sp,
        lineHeight = 48.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}


@Composable
private fun ControlButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "controlScale"
    )
    
    Box(
        modifier = Modifier
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    pressed = true
                    onClick()
                }
            )
            .padding(if (isLandscape) 12.dp else 8.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
    
    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(150)
            pressed = false
        }
    }
}

@Composable
private fun GlassPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "playBtnScale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (pressed) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.2f),
        animationSpec = tween(150),
        label = "playBtnBg"
    )
    
    val squircleShape = RoundedCornerShape(28)
    
    Box(
        modifier = Modifier
            .size(if (isLandscape) 88.dp else 72.dp)
            .scale(scale)
            .background(backgroundColor, squircleShape)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.6f),
                        Color.White.copy(alpha = 0.15f)
                    )
                ),
                shape = squircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    pressed = true
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.White,
            modifier = Modifier.size(if (isLandscape) 38.dp else 32.dp)
        )
    }
    
    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(100)
            pressed = false
        }
    }
}

@Composable
private fun NoiseOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val random = java.util.Random(42)
        val dotCount = 500
        for (i in 0 until dotCount) {
            val x = random.nextFloat() * size.width
            val y = random.nextFloat() * size.height
            val alpha = random.nextFloat() * 0.5f
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = 1.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun Vignette(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.4f),
                    Color.Black.copy(alpha = 0.95f)
                ),
                center = center,
                radius = size.maxDimension * 0.7f
            )
        )
    }
}

