package com.example.ava.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.example.ava.R
import com.example.ava.settings.QuickEntitySlot
import com.example.ava.settings.quickEntitySettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.min

class QuickEntityOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: QuickEntityOverlayView? = null
    private var windowParams: WindowManager.LayoutParams? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isEnabled = false
    private var isVisible = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    private fun ensureViewCreated() {
        if (overlayView == null) {
            createOverlayView()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createOverlayView() {
        val displayMetrics = resources.displayMetrics
        val realMetrics = android.util.DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            windowManager?.defaultDisplay?.getRealMetrics(realMetrics)
        } else {
            windowManager?.defaultDisplay?.getMetrics(realMetrics)
        }
        val realWidth = realMetrics.widthPixels
        val realHeight = realMetrics.heightPixels

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        overlayView = QuickEntityOverlayView(this, realWidth, realHeight).apply {
            setOnDoubleTap { toggleVisibility() }
            onEntityClick = { slot -> handleEntityClick(slot) }
            setOnSlotsReordered { newSlots -> handleSlotsReordered(newSlots) }
        }

        try {
            windowManager?.addView(overlayView, windowParams)
            overlayView?.visibility = View.GONE
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay view", e)
        }
    }

    private fun handleEntityClick(slot: QuickEntitySlot) {
        if (slot.entityId.isEmpty()) {
            Log.d(TAG, "Entity click ignored: empty entityId")
            return
        }
        
        Log.d(TAG, "Entity clicked: ${slot.entityId}")
        
        serviceScope.launch {
            try {
                val service = VoiceSatelliteService.getInstance()
                if (service == null) {
                    Log.e(TAG, "VoiceSatelliteService is null!")
                    return@launch
                }

                val serviceName = when {
                    slot.entityId.startsWith("switch.") -> "switch.toggle"
                    slot.entityId.startsWith("light.") -> "light.toggle"
                    slot.entityId.startsWith("fan.") -> "fan.toggle"
                    slot.entityId.startsWith("cover.") -> "cover.toggle"
                    slot.entityId.startsWith("input_boolean.") -> "input_boolean.toggle"
                    slot.entityId.startsWith("automation.") -> "automation.toggle"
                    slot.entityId.startsWith("button.") -> "button.press"
                    slot.entityId.startsWith("script.") -> "script.turn_on"
                    slot.entityId.startsWith("scene.") -> "scene.turn_on"
                    else -> "homeassistant.toggle"
                }
                Log.d(TAG, "Calling HA service: $serviceName for ${slot.entityId}")
                service.callHaService(serviceName, slot.entityId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to call HA service", e)
            }
        }
    }

    private fun toggleVisibility() {
        if (!isEnabled) return
        
        isVisible = !isVisible
        if (isVisible) {
            overlayView?.visibility = View.VISIBLE
        } else {
            overlayView?.visibility = View.GONE
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(this, R.string.quick_entity_hidden, Toast.LENGTH_SHORT).show()
            }
        }
        
        serviceScope.launch {
            quickEntitySettingsStore.updateData { 
                it.copy(enableQuickEntityDisplay = isVisible) 
            }
        }
    }

    private fun handleSlotsReordered(newSlots: List<QuickEntitySlot>) {
        serviceScope.launch {
            quickEntitySettingsStore.updateData { settings ->
                val fullSlots = newSlots.toMutableList()
                while (fullSlots.size < 6) {
                    fullSlots.add(QuickEntitySlot())
                }
                settings.copy(slots = fullSlots)
            }
        }
    }

    fun updateEntityState(entityId: String, state: String) {
        overlayView?.updateEntityState(entityId, state)
    }

    fun updateEntityUnit(entityId: String, unit: String) {
        overlayView?.updateEntityUnit(entityId, unit)
    }

    fun updateEntityLabel(entityId: String, label: String) {
        overlayView?.updateEntityLabel(entityId, label)
    }

    fun updateSlots(slots: List<QuickEntitySlot>) {
        overlayView?.updateSlots(slots)
    }
    
    fun reloadSlots() {
        serviceScope.launch {
            val settings = quickEntitySettingsStore.data.first()
            val hasAnyEntity = settings.slots.any { it.entityId.isNotEmpty() }
            
            if (!hasAnyEntity) {
                if (isVisible) {
                    isVisible = false
                    isEnabled = false
                    overlayView?.visibility = View.GONE
                    quickEntitySettingsStore.updateData {
                        it.copy(enableQuickEntityDisplay = false)
                    }
                }
                return@launch
            }
            
            if (!isVisible) {
                isEnabled = true
                isVisible = true
                ensureViewCreated()
                overlayView?.visibility = View.VISIBLE
                quickEntitySettingsStore.updateData {
                    it.copy(enableQuickEntityDisplay = true)
                }
            }
            
            overlayView?.updateSlots(settings.slots)
            VoiceSatelliteService.getInstance()?.let { service ->
                service.resubscribeQuickEntities()
                service.getQuickEntityStates().forEach { (entityId, state) ->
                    overlayView?.updateEntityState(entityId, state)
                }
                service.getQuickEntityUnits().forEach { (entityId, unit) ->
                    overlayView?.updateEntityUnit(entityId, unit)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> {
                ensureViewCreated()
                isEnabled = true
                isVisible = true
                overlayView?.visibility = View.VISIBLE
                serviceScope.launch {
                    val settings = quickEntitySettingsStore.data.first()
                    overlayView?.updateSlots(settings.slots)

                    VoiceSatelliteService.getInstance()?.let { service ->
                        service.getQuickEntityStates().forEach { (entityId, state) ->
                            overlayView?.updateEntityState(entityId, state)
                        }
                        service.getQuickEntityUnits().forEach { (entityId, unit) ->
                            overlayView?.updateEntityUnit(entityId, unit)
                        }
                    }
                }
            }
            ACTION_HIDE -> {
                isEnabled = false
                overlayView?.visibility = View.GONE
                isVisible = false
            }
            ACTION_TOGGLE -> {
                if (isEnabled) {
                    toggleVisibility()
                }
            }
            ACTION_UPDATE_SLOTS -> {
                serviceScope.launch {
                    val settings = quickEntitySettingsStore.data.first()
                    overlayView?.updateSlots(settings.slots)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun bringToFront() {
        if (!isEnabled || !isVisible) return
        overlayView?.let { view ->
            windowParams?.let { params ->
                try {
                    if (view.isAttachedToWindow) {
                        windowManager?.removeView(view)
                        windowManager?.addView(view, params)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to bring quick entity to front", e)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        try {
            overlayView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove overlay view", e)
        }
        instance = null
    }

    companion object {
        private const val TAG = "QuickEntityOverlay"
        private var instance: QuickEntityOverlayService? = null

        const val ACTION_SHOW = "com.example.ava.SHOW_QUICK_ENTITY"
        const val ACTION_HIDE = "com.example.ava.HIDE_QUICK_ENTITY"
        const val ACTION_TOGGLE = "com.example.ava.TOGGLE_QUICK_ENTITY"
        const val ACTION_UPDATE_SLOTS = "com.example.ava.UPDATE_QUICK_ENTITY_SLOTS"

        fun getInstance() = instance
        
        fun bringToFrontStatic() {
            instance?.bringToFront()
        }

        fun show(context: Context) {
            context.startService(Intent(context, QuickEntityOverlayService::class.java).apply {
                action = ACTION_SHOW
            })
        }

        fun hide(context: Context) {
            context.startService(Intent(context, QuickEntityOverlayService::class.java).apply {
                action = ACTION_HIDE
            })
        }

        fun toggle(context: Context) {
            context.startService(Intent(context, QuickEntityOverlayService::class.java).apply {
                action = ACTION_TOGGLE
            })
        }

        fun updateSlots(context: Context) {
            context.startService(Intent(context, QuickEntityOverlayService::class.java).apply {
                action = ACTION_UPDATE_SLOTS
            })
        }
    }
}

class QuickEntityOverlayView(
    context: Context,
    private var screenWidth: Int,
    private var screenHeight: Int
) : View(context) {

    private val slots = mutableListOf<QuickEntitySlot>()
    private val entityStates = mutableMapOf<String, String>()
    private val entityUnits = mutableMapOf<String, String>()
    private var onDoubleTap: (() -> Unit)? = null
    var onEntityClick: ((QuickEntitySlot) -> Unit)? = null

    private var lastTapTime = 0L
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var touchedSlotIndex = -1
    
    private var isDragging = false
    private var dragStartTime = 0L
    private var dragSlotIndex = -1
    private var dragCurrentX = 0f
    private var dragCurrentY = 0f
    private var dragTargetIndex = -1
    private val longPressThreshold = 400L
    private val dragThreshold = 20f
    private var onSlotsReordered: ((List<QuickEntitySlot>) -> Unit)? = null
    private var longPressHintShown = false
    

    private var buttonPressedIndex = -1
    private var buttonPressedTime = 0L

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val tileRects = mutableListOf<RectF>()

    private val typeface: Typeface = try {
        Typeface.create("sans-serif-medium", Typeface.NORMAL)
    } catch (e: Exception) {
        Typeface.DEFAULT
    }
    
    private val rajdhaniSemibold: Typeface = try {
        androidx.core.content.res.ResourcesCompat.getFont(context, com.example.ava.R.font.rajdhani_semibold)
            ?: Typeface.create("sans-serif", Typeface.BOLD)
    } catch (e: Exception) {
        Typeface.create("sans-serif", Typeface.BOLD)
    }

    init {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        }
        repeat(6) { slots.add(QuickEntitySlot()) }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            screenWidth = w
            screenHeight = h
            calculateTileRects()
        }
    }

    private fun calculateTileRects() {
        tileRects.clear()
        
        val activeCount = slots.count { it.entityId.isNotEmpty() }
        if (activeCount == 0) return
        
        val isLandscape = screenWidth > screenHeight
        val padding = (min(screenWidth, screenHeight) * 0.05f)
        val gap = (min(screenWidth, screenHeight) * 0.03f)
        
        val availableWidth = screenWidth - padding * 2
        val availableHeight = screenHeight - padding * 2
        
        when (activeCount) {
            1 -> {

                tileRects.add(RectF(padding, padding, padding + availableWidth, padding + availableHeight))
            }
            2 -> {

                if (isLandscape) {
                    val tileWidth = (availableWidth - gap) / 2
                    tileRects.add(RectF(padding, padding, padding + tileWidth, padding + availableHeight))
                    tileRects.add(RectF(padding + tileWidth + gap, padding, padding + availableWidth, padding + availableHeight))
                } else {
                    val tileHeight = (availableHeight - gap) / 2
                    tileRects.add(RectF(padding, padding, padding + availableWidth, padding + tileHeight))
                    tileRects.add(RectF(padding, padding + tileHeight + gap, padding + availableWidth, padding + availableHeight))
                }
            }
            3 -> {

                val leftWidth = availableWidth * 0.45f
                val rightWidth = availableWidth - leftWidth - gap
                val halfHeight = (availableHeight - gap) / 2
                
                tileRects.add(RectF(padding, padding, padding + leftWidth, padding + halfHeight))
                tileRects.add(RectF(padding, padding + halfHeight + gap, padding + leftWidth, padding + availableHeight))
                tileRects.add(RectF(padding + leftWidth + gap, padding, padding + availableWidth, padding + availableHeight))
            }
            4 -> {

                val tileWidth = (availableWidth - gap) / 2
                val tileHeight = (availableHeight - gap) / 2
                
                tileRects.add(RectF(padding, padding, padding + tileWidth, padding + tileHeight))
                tileRects.add(RectF(padding + tileWidth + gap, padding, padding + availableWidth, padding + tileHeight))
                tileRects.add(RectF(padding, padding + tileHeight + gap, padding + tileWidth, padding + availableHeight))
                tileRects.add(RectF(padding + tileWidth + gap, padding + tileHeight + gap, padding + availableWidth, padding + availableHeight))
            }
            5 -> {

                val leftWidth = availableWidth * 0.4f
                val rightWidth = availableWidth - leftWidth - gap
                val halfHeight = (availableHeight - gap) / 2
                val thirdHeight = (availableHeight - gap * 2) / 3
                
                tileRects.add(RectF(padding, padding, padding + leftWidth, padding + halfHeight))
                tileRects.add(RectF(padding, padding + halfHeight + gap, padding + leftWidth, padding + availableHeight))
                tileRects.add(RectF(padding + leftWidth + gap, padding, padding + availableWidth, padding + thirdHeight))
                tileRects.add(RectF(padding + leftWidth + gap, padding + thirdHeight + gap, padding + availableWidth, padding + thirdHeight * 2 + gap))
                tileRects.add(RectF(padding + leftWidth + gap, padding + thirdHeight * 2 + gap * 2, padding + availableWidth, padding + availableHeight))
            }
            else -> {

                val cols = if (isLandscape) 3 else 2
                val rows = if (isLandscape) 2 else 3
                val tileWidth = (availableWidth - gap * (cols - 1)) / cols
                val tileHeight = (availableHeight - gap * (rows - 1)) / rows
                
                for (i in 0 until activeCount.coerceAtMost(6)) {
                    val col = i % cols
                    val row = i / cols
                    val left = padding + col * (tileWidth + gap)
                    val top = padding + row * (tileHeight + gap)
                    tileRects.add(RectF(left, top, left + tileWidth, top + tileHeight))
                }
            }
        }
    }

    fun setOnDoubleTap(callback: () -> Unit) {
        onDoubleTap = callback
    }

    fun updateSlots(newSlots: List<QuickEntitySlot>) {
        slots.clear()
        slots.addAll(newSlots.filter { it.entityId.isNotEmpty() }.take(6))
        calculateTileRects()
        postInvalidate()
    }

    fun updateEntityState(entityId: String, state: String) {
        entityStates[entityId] = state
        postInvalidate()
    }

    fun updateEntityUnit(entityId: String, unit: String) {
        entityUnits[entityId] = unit
        postInvalidate()
    }

    fun updateEntityLabel(entityId: String, label: String) {
        val idx = slots.indexOfFirst { it.entityId == entityId }
        if (idx >= 0) {
            slots[idx] = slots[idx].copy(label = label)
            postInvalidate()
        }
    }

    fun setOnSlotsReordered(callback: (List<QuickEntitySlot>) -> Unit) {
        onSlotsReordered = callback
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
                touchedSlotIndex = findTouchedSlot(event.x, event.y)
                dragStartTime = System.currentTimeMillis()
                dragSlotIndex = -1
                isDragging = false
                longPressHintShown = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(event.x - touchStartX)
                val dy = abs(event.y - touchStartY)
                val holdTime = System.currentTimeMillis() - dragStartTime
                

                if (!longPressHintShown && holdTime >= 2000 && dx < 50 && dy < 50) {
                    longPressHintShown = true
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, R.string.quick_entity_long_press_hint, Toast.LENGTH_SHORT).show()
                    }
                }
                
                if (!isDragging && touchedSlotIndex >= 0 && holdTime > longPressThreshold && (dx > dragThreshold || dy > dragThreshold)) {
                    isDragging = true
                    dragSlotIndex = touchedSlotIndex
                }
                
                if (isDragging) {
                    dragCurrentX = event.x
                    dragCurrentY = event.y
                    dragTargetIndex = findTouchedSlot(event.x, event.y)
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val holdTime = System.currentTimeMillis() - dragStartTime
                val dx = abs(event.x - touchStartX)
                val dy = abs(event.y - touchStartY)
                

                if (holdTime >= 3000 && dx < 300 && dy < 300) {
                    onDoubleTap?.invoke()
                    isDragging = false
                    dragSlotIndex = -1
                    dragTargetIndex = -1
                    touchedSlotIndex = -1
                    invalidate()
                    return true
                } else if (!isDragging && dx < 80 && dy < 80 && holdTime < 3000) {

                    if (touchedSlotIndex >= 0 && touchedSlotIndex < slots.size) {
                        val slot = slots[touchedSlotIndex]
                        if (slot.entityId.isNotEmpty()) {

                            if (slot.entityId.startsWith("button.") || slot.entityId.startsWith("script.") || slot.entityId.startsWith("scene.")) {
                                buttonPressedIndex = touchedSlotIndex
                                buttonPressedTime = System.currentTimeMillis()
                                invalidate()

                                Handler(Looper.getMainLooper()).postDelayed({
                                    buttonPressedIndex = -1
                                    invalidate()
                                }, 500)
                            }
                            onEntityClick?.invoke(slot)
                        }
                    }
                } else if (isDragging && dragSlotIndex >= 0 && dragTargetIndex >= 0 && dragSlotIndex != dragTargetIndex) {
                    val temp = slots[dragSlotIndex]
                    slots[dragSlotIndex] = slots[dragTargetIndex]
                    slots[dragTargetIndex] = temp
                    calculateTileRects()
                    onSlotsReordered?.invoke(slots.toList())
                }
                
                isDragging = false
                dragSlotIndex = -1
                dragTargetIndex = -1
                touchedSlotIndex = -1
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun findTouchedSlot(x: Float, y: Float): Int {
        for (i in tileRects.indices) {
            if (tileRects[i].contains(x, y)) {
                return i
            }
        }
        return -1
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (width > 0 && height > 0) {
            screenWidth = width
            screenHeight = height
            if (tileRects.isEmpty()) {
                calculateTileRects()
            }
        }

        drawBackground(canvas)
        drawTiles(canvas)
    }

    private fun drawBackground(canvas: Canvas) {

        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, screenHeight.toFloat(),
            intArrayOf(Color.parseColor("#050608"), Color.parseColor("#0a0a0c")),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), bgPaint)
        

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        dotPaint.color = Color.argb(13, 255, 255, 255)
        val gridSize = 40f
        val dotRadius = 1f
        var x = gridSize / 2
        while (x < screenWidth) {
            var y = gridSize / 2
            while (y < screenHeight) {
                canvas.drawCircle(x, y, dotRadius, dotPaint)
                y += gridSize
            }
            x += gridSize
        }
    }

    private fun drawTiles(canvas: Canvas) {
        val cornerRadius = min(screenWidth, screenHeight) * 0.04f
        

        for (i in slots.indices) {
            if (i >= tileRects.size) break
            if (isDragging && i == dragSlotIndex) continue
            
            val slot = slots[i]
            val rect = tileRects[i]
            val isActive = getEntityState(slot.entityId)
            val isPressed = touchedSlotIndex == i && !isDragging
            val isDropTarget = isDragging && i == dragTargetIndex

            if (isDropTarget) {
                val highlightPaint = Paint().apply {
                    color = Color.argb(60, 255, 255, 255)
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, highlightPaint)
            }
            
            val isButtonPressed = buttonPressedIndex == i
            drawTile(canvas, rect, slot, isActive, false, isPressed, isButtonPressed, cornerRadius)
        }
        

        if (isDragging && dragSlotIndex >= 0 && dragSlotIndex < slots.size) {
            val slot = slots[dragSlotIndex]
            val originalRect = tileRects[dragSlotIndex]
            val dragSize = min(originalRect.width(), originalRect.height()) * 1.1f
            val dragRect = RectF(
                dragCurrentX - dragSize / 2,
                dragCurrentY - dragSize / 2,
                dragCurrentX + dragSize / 2,
                dragCurrentY + dragSize / 2
            )
            
            val isActive = getEntityState(slot.entityId)
            val isButtonPressed = buttonPressedIndex == dragSlotIndex
            drawTile(canvas, dragRect, slot, isActive, false, true, isButtonPressed, cornerRadius)
        }
    }

    private fun drawTile(
        canvas: Canvas,
        rect: RectF,
        slot: QuickEntitySlot,
        isActive: Boolean,
        isEmpty: Boolean,
        isPressed: Boolean,
        isButtonPressed: Boolean,
        cornerRadius: Float
    ) {
        val theme = getThemeColors(slot.icon, slot.color.ifEmpty { null })
        
        val scale = if (isPressed) 0.96f else 1f
        val scaledRect = RectF(
            rect.centerX() - rect.width() * scale / 2,
            rect.centerY() - rect.height() * scale / 2,
            rect.centerX() + rect.width() * scale / 2,
            rect.centerY() + rect.height() * scale / 2
        )
        
        if (isActive && !isEmpty && slot.entityType != "sensor") {
            glowPaint.shader = RadialGradient(
                scaledRect.centerX(), scaledRect.centerY(),
                scaledRect.width() * 0.8f,
                intArrayOf(theme.glow, Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(scaledRect, cornerRadius, cornerRadius, glowPaint)
        }


        if (isEmpty) {
            drawEmptySlot(canvas, scaledRect)
        } else if (slot.entityType == "sensor") {
            drawSensorTile(canvas, scaledRect, slot, theme)
        } else {
            drawButtonTile(canvas, scaledRect, slot, isActive, isButtonPressed, theme)
        }
    }

    private fun drawEmptySlot(canvas: Canvas, rect: RectF) {
        val tileSize = min(rect.width(), rect.height())
        val cornerRadius = tileSize * 0.12f
        

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bgPaint.shader = LinearGradient(
            rect.left, rect.top,
            rect.left + rect.width() * 0.3f, rect.bottom,
            Color.parseColor("#0c0c0e"), Color.parseColor("#060607"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
        
        val edgeGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        edgeGlowPaint.style = Paint.Style.STROKE
        edgeGlowPaint.strokeWidth = 1f
        edgeGlowPaint.shader = LinearGradient(
            rect.left, rect.top,
            rect.left + rect.width() * 0.4f, rect.top + rect.height() * 0.4f,
            Color.argb(20, 255, 255, 255), Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        val edgePath = Path()
        edgePath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.drawPath(edgePath, edgeGlowPaint)
        
        val iconSize = (tileSize * 0.2f).toInt()
        val drawable = ContextCompat.getDrawable(context, R.drawable.mdi_plus)
        drawable?.let {
            it.setTint(Color.argb(40, 255, 255, 255))
            val iconLeft = (rect.centerX() - iconSize / 2).toInt()
            val iconTop = (rect.centerY() - iconSize / 2).toInt()
            it.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
            it.draw(canvas)
        }
    }

    private fun drawButtonTile(
        canvas: Canvas,
        rect: RectF,
        slot: QuickEntitySlot,
        isActive: Boolean,
        isButtonPressed: Boolean,
        theme: TileTheme
    ) {
        val tileWidth = rect.width()
        val tileHeight = rect.height()
        val tileSize = min(tileWidth, tileHeight)
        val cornerRadius = tileSize * 0.12f
        
        val isActionEntity = slot.entityId.startsWith("button.") || 
                            slot.entityId.startsWith("script.") || 
                            slot.entityId.startsWith("scene.")
        

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (isActive || isButtonPressed) {

            bgPaint.shader = LinearGradient(
                rect.left, rect.top,
                rect.left + tileWidth * 0.3f, rect.bottom,
                theme.topColor, theme.bottomColor,
                Shader.TileMode.CLAMP
            )
        } else {
            bgPaint.shader = LinearGradient(
                rect.left, rect.top,
                rect.left + tileWidth * 0.3f, rect.bottom,
                Color.parseColor("#0f1012"), Color.parseColor("#08090a"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
        

        val presetColor = com.example.ava.ui.components.MdiColorMapper.getTileColorForIcon(slot.icon, slot.color.ifEmpty { null })
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = 1.5f
        val borderAlpha = if (isActive || isButtonPressed) 220 else 100
        borderPaint.color = Color.argb(borderAlpha, 
            Color.red(presetColor.topColor), 
            Color.green(presetColor.topColor), 
            Color.blue(presetColor.topColor))
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
        

        val screenMin = min(screenWidth, screenHeight).toFloat()
        val tileArea = tileWidth * tileHeight
        val screenArea = screenWidth.toFloat() * screenHeight.toFloat()
        val areaRatio = tileArea / screenArea
        val baseIconSize = when {
            areaRatio > 0.3f -> tileSize * 0.80f
            areaRatio > 0.15f -> tileSize * 0.65f
            else -> tileSize * 0.40f
        }
        val iconSize = baseIconSize.coerceIn(32f, 600f).toInt()
        val iconResId = getIconResIdForState(slot.icon, isActive)
        val drawable = ContextCompat.getDrawable(context, iconResId)
        
        val iconColor = if (isActive || isButtonPressed) {
            Color.argb(230, 0, 0, 0)
        } else {
            Color.parseColor("#5a5a5f")
        }
        
        drawable?.let {
            it.setTint(iconColor)
            val iconLeft = (rect.centerX() - iconSize / 2).toInt()
            val iconTop = (rect.centerY() - iconSize / 2).toInt()
            it.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
            it.draw(canvas)
        }
    }

    private fun drawSensorTile(
        canvas: Canvas,
        rect: RectF,
        slot: QuickEntitySlot,
        theme: TileTheme
    ) {
        val rawState = entityStates[slot.entityId] ?: "--"
        val state = rawState.toDoubleOrNull()?.let { v ->
            val dotIdx = rawState.indexOf('.')
            if (dotIdx < 0) rawState
            else {
                val decimals = rawState.length - dotIdx - 1
                if (decimals <= 2) rawState
                else String.format("%.2f", v)
            }
        } ?: rawState
        val tileWidth = rect.width()
        val tileHeight = rect.height()
        val tileSize = min(tileWidth, tileHeight)
        val cornerRadius = tileSize * 0.12f
        val padding = tileSize * 0.12f
        
        val sensorType = extractSensorType(slot.entityId)
        

        val presetColor = com.example.ava.ui.components.MdiColorMapper.getTileColorForIcon(slot.icon, slot.color.ifEmpty { null })
        val hasCustomColor = slot.color.isNotEmpty()
        

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bgPaint.shader = LinearGradient(
            rect.left, rect.top,
            rect.left + tileWidth * 0.3f, rect.bottom,
            Color.parseColor("#0f1012"), Color.parseColor("#08090a"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
        

        if (hasCustomColor) {
            val centerX = rect.centerX()
            val centerY = rect.centerY()
            val diagonal = kotlin.math.sqrt(tileWidth * tileWidth + tileHeight * tileHeight)
            val radius = diagonal * 0.55f
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            glowPaint.shader = RadialGradient(
                centerX, centerY, radius,
                intArrayOf(
                    Color.argb(80, Color.red(presetColor.topColor), Color.green(presetColor.topColor), Color.blue(presetColor.topColor)),
                    Color.argb(30, Color.red(presetColor.topColor), Color.green(presetColor.topColor), Color.blue(presetColor.topColor)),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.save()
            val clipPath = Path()
            clipPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
            canvas.clipPath(clipPath)
            canvas.drawCircle(centerX, centerY, radius, glowPaint)
            canvas.restore()
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = 1.5f
        borderPaint.color = Color.argb(100, 
            Color.red(presetColor.topColor), 
            Color.green(presetColor.topColor), 
            Color.blue(presetColor.topColor))
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
        

        val iconResId = getIconResId(slot.icon)
        val bgDrawable = ContextCompat.getDrawable(context, iconResId)
        bgDrawable?.let {
            it.setTint(Color.argb(8, 255, 255, 255))

            val bgIconSize = (tileSize * 1.3f).toInt()
            val iconRight = (rect.right + tileSize * 0.35f).toInt()
            val iconBottom = (rect.bottom + tileSize * 0.35f).toInt()
            canvas.save()
            val clipPath = Path()
            clipPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
            canvas.clipPath(clipPath)
            it.setBounds(iconRight - bgIconSize, iconBottom - bgIconSize, iconRight, iconBottom)
            it.draw(canvas)
            canvas.restore()
        }
        

        val isLargeTile = tileHeight > tileWidth * 1.3f || tileWidth > tileHeight * 1.3f || tileSize > 300
        val isLandscape = tileWidth > tileHeight
        val availableWidth = tileWidth - padding * 2
        val availableHeight = tileHeight - padding * 2
        val aspectRatio = tileWidth / tileHeight
        val horizontalShift = if (aspectRatio > 1.5f) {
            (tileWidth - tileHeight) * 0.25f
        } else if (aspectRatio < 0.67f) {
            0f
        } else {
            0f
        }


        val labelSize = (availableHeight * 0.10f).coerceIn(8f, 36f)
        textPaint.color = Color.parseColor("#6a6a6f")
        textPaint.textSize = labelSize
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textPaint.letterSpacing = 0.15f
        val labelText = slot.label.uppercase().ifEmpty { sensorType.uppercase() }
        val labelCenterX = rect.centerX()
        val labelY = rect.top + padding + labelSize
        canvas.drawText(labelText, labelCenterX, labelY, textPaint)
        

        val labelWidth = textPaint.measureText(labelText)
        val lineY = labelY - labelSize * 0.3f
        val lineGap = padding * 0.4f
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        linePaint.strokeWidth = 1f
        linePaint.shader = LinearGradient(
            labelCenterX + labelWidth / 2 + lineGap, lineY,
            rect.right - padding, lineY,
            Color.argb(77, 255, 214, 10), Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawLine(labelCenterX + labelWidth / 2 + lineGap, lineY, rect.right - padding, lineY, linePaint)
        val linePaintLeft = Paint(Paint.ANTI_ALIAS_FLAG)
        linePaintLeft.strokeWidth = 1f
        linePaintLeft.shader = LinearGradient(
            labelCenterX - labelWidth / 2 - lineGap, lineY,
            rect.left + padding, lineY,
            Color.argb(77, 255, 214, 10), Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawLine(labelCenterX - labelWidth / 2 - lineGap, lineY, rect.left + padding, lineY, linePaintLeft)
        

        if (isLargeTile) {
            val decorLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            decorLinePaint.shader = LinearGradient(
                rect.left + padding * 0.3f, rect.top + padding * 2,
                rect.left + padding * 0.3f, rect.bottom - padding * 2,
                Color.argb(0, Color.red(presetColor.topColor), Color.green(presetColor.topColor), Color.blue(presetColor.topColor)),
                Color.argb(60, Color.red(presetColor.topColor), Color.green(presetColor.topColor), Color.blue(presetColor.topColor)),
                Shader.TileMode.CLAMP
            )
            decorLinePaint.strokeWidth = 2f
            canvas.drawLine(rect.left + padding * 0.3f, rect.top + padding * 2, rect.left + padding * 0.3f, rect.bottom - padding * 2, decorLinePaint)
            
            val decorTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            decorTextPaint.color = Color.argb(30, 255, 255, 255)
            decorTextPaint.textSize = (tileSize * 0.05f).coerceIn(10f, 18f)
            decorTextPaint.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            decorTextPaint.letterSpacing = 0.3f
            decorTextPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("SENSOR DATA", rect.right - padding, rect.bottom - padding * 0.5f, decorTextPaint)
            
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            dotPaint.color = Color.argb(20, 255, 255, 255)
            val dotSpacing = tileSize * 0.03f
            val dotRadius = 1.5f
            for (row in 0..2) {
                for (col in 0..2) {
                    canvas.drawCircle(
                        rect.left + padding + col * dotSpacing,
                        rect.bottom - padding * 1.5f - row * dotSpacing,
                        dotRadius, dotPaint
                    )
                }
            }
        }
        

        val contentTop = rect.top + padding + labelSize + padding * 0.3f
        val contentBottom = rect.bottom - padding
        val contentHeight = contentBottom - contentTop
        var valueSize = (contentHeight * 0.85f).coerceIn(16f, 700f)
        
        textPaint.textSize = valueSize
        textPaint.typeface = rajdhaniSemibold
        textPaint.letterSpacing = 0.02f
        textPaint.textAlign = Paint.Align.LEFT
        
        val maxValueWidth = availableWidth * 0.92f
        while (textPaint.measureText(state) > maxValueWidth && valueSize > 12f) {
            valueSize -= 2f
            textPaint.textSize = valueSize
        }
        
        val contentCenterY = (contentTop + contentBottom) / 2
        val verticalOffset = if (isLargeTile) -valueSize * 0.05f else 0f
        val valueY = contentCenterY + valueSize * 0.35f + verticalOffset
        
        var unit = entityUnits[slot.entityId] ?: ""
        if (unit.length > 5) unit = unit.take(5)
        val stateWidth = textPaint.measureText(state)
        val gap = (padding * 0.3f).coerceIn(2f, 12f)
        var unitSize = (contentHeight * 0.28f).coerceIn(10f, 56f)
        val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        unitPaint.textSize = unitSize
        unitPaint.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        unitPaint.letterSpacing = 0.02f
        val unitWidth = if (unit.isNotEmpty()) unitPaint.measureText(unit) + gap else 0f
        val totalWidth = stateWidth + unitWidth
        val valueX = rect.centerX() - totalWidth / 2
        
        textPaint.shader = LinearGradient(
            valueX, valueY - valueSize,
            valueX, valueY,
            Color.WHITE, Color.argb(80, 255, 255, 255),
            Shader.TileMode.CLAMP
        )
        canvas.drawText(state, valueX, valueY, textPaint)
        textPaint.shader = null
        

        if (unit.isNotEmpty()) {
            unitPaint.color = Color.parseColor("#6a6a6f")
            canvas.drawText(unit, valueX + stateWidth + gap, valueY - unitSize * 0.15f, unitPaint)
        }
    }

    private fun getEntityState(entityId: String): Boolean {
        if (entityId.isEmpty()) return false
        val state = entityStates[entityId]?.lowercase() ?: return false

        val activeStates = listOf("on", "true", "playing", "home", "open", "unlocked", "detected", "active")
        val inactiveStates = listOf("off", "false", "paused", "idle", "standby", "unavailable", "unknown", "away", "closed", "locked", "not_home")
        return when {
            state in activeStates -> true
            state in inactiveStates -> false
            state.toDoubleOrNull() != null -> state.toDouble() > 0
            else -> false
        }
    }

    private fun extractSensorType(entityId: String): String {

        val knownTypes = listOf(
            "temperature", "humidity", "battery", "pressure", "illuminance", "lux",
            "power", "energy", "voltage", "current", "co2", "pm25", "pm10",
            "motion", "door", "window", "smoke", "gas", "water", "moisture"
        )
        val lowerEntityId = entityId.lowercase()
        for (type in knownTypes) {
            if (lowerEntityId.contains(type)) return type
        }
        return "sensor"
    }

    private fun getIconResId(icon: String): Int = 
        com.example.ava.ui.components.MdiIconMapper.getIconResId(icon)

    private fun getIconResIdForState(icon: String, isActive: Boolean): Int = 
        com.example.ava.ui.components.MdiIconMapper.getIconResIdForState(icon, isActive)

    private fun getThemeColors(icon: String, customColor: String? = null): TileTheme {
        val tileColor = com.example.ava.ui.components.MdiColorMapper.getTileColorForIcon(icon, customColor)
        return TileTheme(tileColor.topColor, tileColor.bottomColor, tileColor.glowColor)
    }

    data class TileTheme(
        val topColor: Int,
        val bottomColor: Int,
        val glow: Int
    )
}
