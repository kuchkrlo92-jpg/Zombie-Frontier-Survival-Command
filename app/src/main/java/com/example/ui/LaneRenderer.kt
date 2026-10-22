package com.example.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.*

/**
 * 3D-inspired Perspective Renderer for the Zombie Frontier Lane-Runner Combat.
 * Simulates a high-speed third-person chase camera over a post-apocalyptic elevated highway.
 */
object LaneRenderer {

    // Cached paints for high performance 60FPS rendering without GC churn
    private val textPaintGateTitle = Paint().apply {
        isAntiAlias = true
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val textPaintDamage = Paint().apply {
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val textPaintZombie = Paint().apply {
        isAntiAlias = true
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        textSize = 22f
    }

    /**
     * Projects world coordinates (laneFloat, distanceZ) to 2D screen coordinates
     * using simulated 3D perspective projection.
     */
    fun project(
        laneFloat: Float,
        z: Float,
        width: Float,
        height: Float
    ): Triple<Offset, Float, Float> {
        val horizonY = height * 0.28f
        val horizonX = width * 0.5f
        val playerY = height * 0.78f
        val horizonRoadWidth = width * 0.24f
        val bottomRoadWidth = width * 0.92f

        // Depth perspective calculation
        val depthFactor = 1.0f / (1.0f + maxOf(0f, z) * 0.0035f)
        val screenY = horizonY + (playerY - horizonY) * depthFactor
        val currentRoadWidth = horizonRoadWidth + (bottomRoadWidth - horizonRoadWidth) * depthFactor
        val laneWidth = currentRoadWidth / 3.0f

        // laneFloat: 0.0 = Left, 1.0 = Center, 2.0 = Right
        val screenX = horizonX + (laneFloat - 1f) * laneWidth
        return Triple(Offset(screenX, screenY), depthFactor, laneWidth)
    }

    fun renderScene(
        drawScope: DrawScope,
        state: CombatState,
        width: Float,
        height: Float,
        environmentMode: String = "DAY"
    ) {
        val horizonY = height * 0.28f
        val horizonX = width * 0.5f
        val playerY = height * 0.78f
        val horizonRoadWidth = width * 0.24f
        val bottomRoadWidth = width * 0.92f
        val isNight = environmentMode == "NIGHT"

        // 1. Sky & Atmospheric Backdrop (Day vs Night)
        if (isNight) {
            drawScope.drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF02030A),
                        Color(0xFF060918),
                        Color(0xFF0F172A),
                        Color(0xFF1E293B)
                    ),
                    startY = 0f,
                    endY = horizonY
                ),
                size = Size(width, horizonY)
            )
            // Moon with soft halo glow
            drawScope.drawCircle(
                color = Color(0xFFE2E8F0).copy(alpha = 0.85f),
                radius = 28f,
                center = Offset(width * 0.8f, horizonY * 0.35f)
            )
            drawScope.drawCircle(
                color = Color(0x3394A3B8),
                radius = 48f,
                center = Offset(width * 0.8f, horizonY * 0.35f)
            )
            // Stars
            for (s in 0..12) {
                val sx = (s * 73f) % width
                val sy = (s * 31f) % (horizonY * 0.7f)
                drawScope.drawCircle(Color(0xCCFFFFFF), radius = 1.5f, center = Offset(sx, sy))
            }
        } else {
            drawScope.drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1565C0),
                        Color(0xFF1E88E5),
                        Color(0xFF42A5F5),
                        Color(0xFF90CAF9)
                    ),
                    startY = 0f,
                    endY = horizonY
                ),
                size = Size(width, horizonY)
            )
            // Bright Sun
            drawScope.drawCircle(
                color = Color(0xFFFFEE58).copy(alpha = 0.9f),
                radius = 32f,
                center = Offset(width * 0.78f, horizonY * 0.3f)
            )
            drawScope.drawCircle(
                color = Color(0x33FFEE58),
                radius = 60f,
                center = Offset(width * 0.78f, horizonY * 0.3f)
            )
        }

        // Atmospheric Haze
        drawScope.drawRect(
            color = if (isNight) Color(0x440F172A) else Color(0x3390CAF9),
            topLeft = Offset(0f, horizonY - 40f),
            size = Size(width, 40f)
        )

        // Distant Ruined City Silhouette along horizon
        val skylinePath = Path().apply {
            moveTo(0f, horizonY)
            var curX = 0f
            val buildingStep = width / 18f
            for (i in 0..18) {
                val bH = when (i % 5) {
                    0 -> 38f
                    1 -> 22f
                    2 -> 48f
                    3 -> 28f
                    else -> 16f
                }
                lineTo(curX, horizonY - bH)
                lineTo(curX + buildingStep, horizonY - bH)
                curX += buildingStep
            }
            lineTo(width, horizonY)
            close()
        }
        drawScope.drawPath(skylinePath, if (isNight) Color(0xFF090D14) else Color(0xFF263238))

        // Red aircraft warning lights on top of ruined skyscrapers
        for (i in 0..5) {
            val lightX = (width * 0.15f) + i * (width * 0.14f)
            val lightY = horizonY - (22f + (i % 3) * 10f)
            drawScope.drawCircle(Color(0xFFFF1744), radius = 2.5f, center = Offset(lightX, lightY))
        }

        // Toxic Ocean / Harbor water below bridge on left & right sides
        drawScope.drawRect(
            color = if (isNight) Color(0xFF04080D) else Color(0xFF0C141C),
            topLeft = Offset(0f, horizonY),
            size = Size(width, height - horizonY)
        )

        // Toxic water ripples
        val ripplePhase = (state.player.runDistance * 4f) % 40f
        for (r in 0..6) {
            val ry = horizonY + 30f + r * 45f
            val rxLeft = width * 0.08f + sin(r.toFloat() + ripplePhase * 0.1f) * 12f
            val rxRight = width * 0.92f - sin(r.toFloat() + ripplePhase * 0.1f) * 12f
            drawScope.drawLine(
                color = if (isNight) Color(0x3300E5FF) else Color(0x334DD0E1),
                start = Offset(rxLeft - 25f, ry),
                end = Offset(rxLeft + 25f, ry),
                strokeWidth = 2f
            )
            drawScope.drawLine(
                color = if (isNight) Color(0x3300E5FF) else Color(0x334DD0E1),
                start = Offset(rxRight - 25f, ry),
                end = Offset(rxRight + 25f, ry),
                strokeWidth = 2f
            )
        }

        // 2. Realistic Elevated Highway Bridge Road Polygon with Asphalt Texture & Cracks
        val roadPath = Path().apply {
            moveTo(horizonX - horizonRoadWidth / 2f, horizonY)
            lineTo(horizonX + horizonRoadWidth / 2f, horizonY)
            lineTo(horizonX + bottomRoadWidth / 2f, height)
            lineTo(horizonX - bottomRoadWidth / 2f, height)
            close()
        }
        drawScope.drawPath(roadPath, if (isNight) Color(0xFF101418) else Color(0xFF21252B))

        // Scrolling Asphalt Cracks, Potholes & Texture Details for realism
        val roadScroll = (state.player.runDistance * 12f) % 120f
        for (c in 0..8) {
            val cy = horizonY + ((c * 45f - roadScroll + 300f) % 300f) * ((height - horizonY) / 300f)
            val cWidth = (bottomRoadWidth - horizonRoadWidth) * ((cy - horizonY) / (height - horizonY)) + horizonRoadWidth
            val cx = horizonX + ((c * 17f) % 50f - 25f) * (cWidth / horizonRoadWidth) * 0.3f
            val crackSize = 12f * (1f + (cy - horizonY) / (height - horizonY) * 1.5f)

            drawScope.drawOval(
                color = if (isNight) Color(0xFF080A0D) else Color(0xFF15181C),
                topLeft = Offset(cx - crackSize, cy - crackSize * 0.4f),
                Size(crackSize * 2f, crackSize * 0.8f)
            )
        }

        // Bridge Concrete Guardrails on left and right
        val leftRail = Path().apply {
            moveTo(horizonX - horizonRoadWidth / 2f - 4f, horizonY)
            lineTo(horizonX - horizonRoadWidth / 2f, horizonY)
            lineTo(horizonX - bottomRoadWidth / 2f, height)
            lineTo(horizonX - bottomRoadWidth / 2f - 18f, height)
            close()
        }
        drawScope.drawPath(leftRail, Color(0xFF37474F))

        val rightRail = Path().apply {
            moveTo(horizonX + horizonRoadWidth / 2f, horizonY)
            lineTo(horizonX + horizonRoadWidth / 2f + 4f, horizonY)
            lineTo(horizonX + bottomRoadWidth / 2f + 18f, height)
            lineTo(horizonX + bottomRoadWidth / 2f, height)
            close()
        }
        drawScope.drawPath(rightRail, Color(0xFF37474F))

        // Working Streetlights along guardrails in Night mode casting light pools
        if (isNight) {
            for (sl in 0..4) {
                val slZ = sl * 70f - (state.player.runDistance * 3f) % 280f
                if (slZ in 0f..250f) {
                    val (slPos, slScale, _) = project(0.0f, slZ, width, height)
                    drawScope.drawOval(
                        color = Color(0xFFFFEE58).copy(alpha = 0.25f),
                        topLeft = Offset(slPos.x - 40f * slScale, slPos.y - 15f * slScale),
                        size = Size(80f * slScale, 30f * slScale)
                    )
                }
            }
        }

        // Yellow and black hazard edge lines along highway borders
        drawScope.drawLine(
            color = Color(0xFFFFB300),
            start = Offset(horizonX - horizonRoadWidth / 2f, horizonY),
            end = Offset(horizonX - bottomRoadWidth / 2f, height),
            strokeWidth = 3f
        )
        drawScope.drawLine(
            color = Color(0xFFFFB300),
            start = Offset(horizonX + horizonRoadWidth / 2f, horizonY),
            end = Offset(horizonX + bottomRoadWidth / 2f, height),
            strokeWidth = 3f
        )

        // 3. Scrolling Highway Lane Dividers (Giving dynamic speed sensation)
        val scrollOffset = (state.player.runDistance * 10f) % 50f
        val numSegments = 16
        for (i in 0 until numSegments) {
            val zStart = i * 28f - scrollOffset + 10f
            if (zStart < 0f) continue
            val zEnd = zStart + 14f

            val (p1Left, _, _) = project(0.5f, zStart, width, height)
            val (p2Left, _, _) = project(0.5f, zEnd, width, height)
            val (p1Right, _, _) = project(1.5f, zStart, width, height)
            val (p2Right, _, _) = project(1.5f, zEnd, width, height)

            drawScope.drawLine(
                color = Color(0xCCECEFF1),
                start = p1Left,
                end = p2Left,
                strokeWidth = maxOf(1.5f, 5f * (1f / (1f + zStart * 0.004f)))
            )
            drawScope.drawLine(
                color = Color(0xCCECEFF1),
                start = p1Right,
                end = p2Right,
                strokeWidth = maxOf(1.5f, 5f * (1f / (1f + zStart * 0.004f)))
            )
        }

        // 4. Extraction Helipad / Convoy Gate at Target Distance (Campaign missions)
        if (!state.isSurvivalMode) {
            val distToGoal = state.player.targetDistance - state.player.runDistance
            if (distToGoal in -20f..350f) {
                val (extractPos, extractScale, extractRoadWidth) = project(1.0f, distToGoal, width, height)
                val gateW = extractRoadWidth * 0.95f
                val gateH = gateW * 0.45f

                // Extraction Arch
                drawScope.drawRect(
                    color = Color(0x8800E676),
                    topLeft = Offset(extractPos.x - gateW / 2f, extractPos.y - gateH),
                    size = Size(gateW, gateH)
                )
                drawScope.drawRect(
                    color = Color(0xFF00E676),
                    topLeft = Offset(extractPos.x - gateW / 2f, extractPos.y - gateH),
                    size = Size(gateW, gateH),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f * extractScale)
                )

                // Helicopter rotor & body above extraction gate
                val rotorAngle = (System.currentTimeMillis() % 360) * 4f
                val chopperY = extractPos.y - gateH - (30f * extractScale)
                drawScope.drawCircle(
                    color = Color(0xFF37474F),
                    radius = 16f * extractScale,
                    center = Offset(extractPos.x, chopperY)
                )
                val rotorW = 55f * extractScale
                val rad = Math.toRadians(rotorAngle.toDouble())
                val rx = (cos(rad) * rotorW).toFloat()
                val ry = (sin(rad) * rotorW).toFloat()
                drawScope.drawLine(
                    color = Color(0xAAFFFFFF),
                    start = Offset(extractPos.x - rx, chopperY - ry),
                    end = Offset(extractPos.x + rx, chopperY + ry),
                    strokeWidth = 3f * extractScale
                )

                drawScope.drawIntoCanvas { canvas ->
                    textPaintGateTitle.color = android.graphics.Color.WHITE
                    textPaintGateTitle.textSize = maxOf(12f, 26f * extractScale)
                    canvas.nativeCanvas.drawText(
                        "EXTRACTION HELIPAD",
                        extractPos.x,
                        extractPos.y - gateH * 0.45f,
                        textPaintGateTitle
                    )
                }
            }
        }

        // 5. Boss Attack Telegraphs (Ground Warning on Lanes)
        state.boss?.let { boss ->
            if (boss.currentPattern == BossPattern.TELEGRAPH_SLAM || boss.currentPattern == BossPattern.TELEGRAPH_CHARGE) {
                val targetLane = boss.telegraphLane.toFloat()
                val isCharge = boss.currentPattern == BossPattern.TELEGRAPH_CHARGE
                val flashAlpha = ((sin(System.currentTimeMillis() * 0.02) + 1.0) / 2.0).toFloat()
                val warningColor = if (isCharge) Color(0xFFFF1744).copy(alpha = 0.45f + flashAlpha * 0.35f)
                else Color(0xFFFF9100).copy(alpha = 0.45f + flashAlpha * 0.35f)

                // Highlight the targeted danger lane from player up to boss
                val (nearPos, _, nearLaneW) = project(targetLane, 0f, width, height)
                val (farPos, _, farLaneW) = project(targetLane, boss.distanceZ, width, height)

                val dangerPath = Path().apply {
                    moveTo(nearPos.x - nearLaneW * 0.45f, nearPos.y)
                    lineTo(nearPos.x + nearLaneW * 0.45f, nearPos.y)
                    lineTo(farPos.x + farLaneW * 0.45f, farPos.y)
                    lineTo(farPos.x - farLaneW * 0.45f, farPos.y)
                    close()
                }
                drawScope.drawPath(dangerPath, warningColor)

                // Warning chevrons and alert text
                drawScope.drawIntoCanvas { canvas ->
                    textPaintGateTitle.color = android.graphics.Color.YELLOW
                    textPaintGateTitle.textSize = 24f
                    val alertText = if (isCharge) "⚠ CHARGE DANGER // DODGE ⚠" else "⚠ SLAM IMPACT // DODGE ⚠"
                    canvas.nativeCanvas.drawText(
                        alertText,
                        nearPos.x,
                        (nearPos.y + farPos.y) * 0.5f,
                        textPaintGateTitle
                    )
                }
            }
        }

        // 6. Boss Shockwaves
        for (shockwave in state.laneShockwaves) {
            val (sPos, sScale, sLaneW) = project(shockwave.lane.toFloat(), shockwave.distanceZ, width, height)
            val ringW = sLaneW * 0.9f
            drawScope.drawOval(
                color = Color(0xFFFF5722),
                topLeft = Offset(sPos.x - ringW / 2f, sPos.y - 12f * sScale),
                size = Size(ringW, 24f * sScale),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f * sScale)
            )
            drawScope.drawOval(
                color = Color(0x88FFD54F),
                topLeft = Offset(sPos.x - ringW * 0.4f, sPos.y - 8f * sScale),
                size = Size(ringW * 0.8f, 16f * sScale)
            )
        }

        // 7. Tactical Supply Gates Spanning Lanes
        for (gate in state.tacticalGates) {
            val z = gate.distanceZ
            if (z !in -8f..380f) continue
            val (gPos, gScale, gLaneW) = project(gate.lane.toFloat(), z, width, height)
            val gateW = gLaneW * 0.92f
            val gateH = gLaneW * 1.15f
            val primaryColor = if (gate.isPositive) Color(0xFF00E5FF) else Color(0xFFFF1744)
            val bgColor = if (gate.isPositive) Color(0x5500B0FF) else Color(0x55D50000)

            // Holographic Energy Wall
            drawScope.drawRect(
                color = bgColor,
                topLeft = Offset(gPos.x - gateW / 2f, gPos.y - gateH),
                size = Size(gateW, gateH)
            )

            // Glowing Pylons on left and right
            drawScope.drawLine(
                color = primaryColor,
                start = Offset(gPos.x - gateW / 2f, gPos.y),
                end = Offset(gPos.x - gateW / 2f, gPos.y - gateH),
                strokeWidth = 4f * gScale
            )
            drawScope.drawLine(
                color = primaryColor,
                start = Offset(gPos.x + gateW / 2f, gPos.y),
                end = Offset(gPos.x + gateW / 2f, gPos.y - gateH),
                strokeWidth = 4f * gScale
            )
            // Top beam
            drawScope.drawLine(
                color = primaryColor,
                start = Offset(gPos.x - gateW / 2f, gPos.y - gateH),
                end = Offset(gPos.x + gateW / 2f, gPos.y - gateH),
                strokeWidth = 5f * gScale
            )

            // Gate Display Label Box
            val bannerH = gateH * 0.42f
            drawScope.drawRect(
                color = Color(0xDD0D1117),
                topLeft = Offset(gPos.x - gateW / 2f, gPos.y - gateH),
                size = Size(gateW, bannerH)
            )
            drawScope.drawRect(
                color = primaryColor,
                topLeft = Offset(gPos.x - gateW / 2f, gPos.y - gateH),
                size = Size(gateW, bannerH),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f * gScale)
            )

            drawScope.drawIntoCanvas { canvas ->
                textPaintGateTitle.color = if (gate.isPositive) android.graphics.Color.WHITE else android.graphics.Color.RED
                textPaintGateTitle.textSize = maxOf(11f, 22f * gScale)
                canvas.nativeCanvas.drawText(
                    gate.label,
                    gPos.x,
                    gPos.y - gateH + bannerH * 0.65f,
                    textPaintGateTitle
                )
            }
        }

        // 8. Road Obstacles (Wrecked Cars, Barricades, Toxic Barrels)
        for (obstacle in state.laneObstacles) {
            val z = obstacle.distanceZ
            if (z !in -8f..380f) continue
            val (oPos, oScale, oLaneW) = project(obstacle.lane.toFloat(), z, width, height)
            val oW = oLaneW * 0.65f
            val oH = oLaneW * 0.4f

            when (obstacle.type) {
                LaneObstacleType.WRECKED_CAR -> {
                    // Detailed wrecked vehicle
                    drawScope.drawRect(
                        color = Color(0xFF263238),
                        topLeft = Offset(oPos.x - oW / 2f, oPos.y - oH),
                        size = Size(oW, oH)
                    )
                    // Vehicle details: windows/roof
                    drawScope.drawRect(
                        color = Color(0xFF37474F),
                        topLeft = Offset(oPos.x - oW / 3f, oPos.y - oH * 0.9f),
                        size = Size(oW * 0.6f, oH * 0.4f)
                    )
                    // Fire & Smoke on wreck
                    val flashAlpha = ((sin(System.currentTimeMillis() * 0.03) + 1.0) / 2.0).toFloat()
                    drawScope.drawCircle(
                        color = Color(0xFFFF5722).copy(alpha = 0.6f + flashAlpha * 0.4f),
                        radius = 12f * oScale,
                        center = Offset(oPos.x, oPos.y - oH - 6f * oScale)
                    )
                }
                LaneObstacleType.BURNING_BARRIER -> {
                    // Barbed wire sandbag barricade
                    drawScope.drawRect(
                        color = Color(0xFF5D4037),
                        topLeft = Offset(oPos.x - oW / 2f, oPos.y - oH * 0.7f),
                        size = Size(oW, oH * 0.7f)
                    )
                }
                LaneObstacleType.TOXIC_BARRELS -> {
                    // Toxic glowing barrels
                    drawScope.drawCircle(
                        color = Color(0xFF76FF03),
                        radius = oW * 0.28f,
                        center = Offset(oPos.x - oW * 0.2f, oPos.y - oH * 0.4f)
                    )
                    drawScope.drawCircle(
                        color = Color(0xFF64DD17),
                        radius = oW * 0.28f,
                        center = Offset(oPos.x + oW * 0.2f, oPos.y - oH * 0.4f)
                    )
                }
                LaneObstacleType.CONCRETE_BLOCK -> {
                    drawScope.drawRect(
                        color = Color(0xFF78909C),
                        topLeft = Offset(oPos.x - oW / 2f, oPos.y - oH * 0.8f),
                        size = Size(oW, oH * 0.8f)
                    )
                }
            }
        }

        // 9. The Boss (The Abomination, The Iron Tyrant, The Plague Queen)
        state.boss?.let { boss ->
            if (boss.distanceZ in -15f..400f) {
                val (bPos, bScale, bLaneW) = project(boss.visualLaneX, boss.distanceZ, width, height)
                val bossW = bLaneW * 3.2f
                val bossH = bLaneW * 4.0f

                when {
                    boss.isDying -> {
                        // Dramatic death animation: stagger, shudder, collapse, and dissolve
                        val collapse = (boss.deathTimer.toFloat() / boss.deathDuration.toFloat()).coerceIn(0f, 1f)
                        val staggerX = sin(boss.deathTimer * 0.8f) * 10f * (1f - collapse)
                        val fadeAlpha = (1f - collapse * 0.75f).coerceIn(0.15f, 1f)

                        when (boss.bossType) {
                            BossType.ABOMINATION -> {
                                val deathColor = Color(0xFF1B5E20).copy(alpha = fadeAlpha)
                                drawScope.drawOval(
                                    deathColor,
                                    Offset(bPos.x - bossW * 0.5f + staggerX, bPos.y - bossH * (1f - collapse * 0.6f)),
                                    Size(bossW * (1f - collapse * 0.35f), bossH * (0.8f - collapse * 0.5f))
                                )
                                // Dissolving toxic smoke & sparks
                                for (s in 0 until 6) {
                                    val spX = bPos.x - bossW * 0.3f + (s * bossW * 0.12f) + sin((boss.deathTimer + s).toFloat()) * 12f
                                    val spY = bPos.y - bossH * 0.5f - (collapse * 60f) - (s * 8f)
                                    drawScope.drawCircle(Color(0xFF76FF03).copy(alpha = (1f - collapse).coerceIn(0f, 1f)), 4f * bScale, Offset(spX, spY))
                                }
                            }
                            BossType.IRON_TYRANT -> {
                                val deathColor = Color(0xFF37474F).copy(alpha = fadeAlpha)
                                drawScope.drawRect(
                                    deathColor,
                                    Offset(bPos.x - bossW * 0.5f + staggerX, bPos.y - bossH * (0.9f - collapse * 0.5f)),
                                    Size(bossW * (1f - collapse * 0.25f), bossH * (0.8f - collapse * 0.5f))
                                )
                                // Metal explosion sparks
                                for (s in 0 until 6) {
                                    val spX = bPos.x - bossW * 0.3f + (s * bossW * 0.12f)
                                    val spY = bPos.y - bossH * 0.4f - (collapse * 50f) - (s * 6f)
                                    drawScope.drawCircle(Color(0xFFFF9100).copy(alpha = (1f - collapse).coerceIn(0f, 1f)), 5f * bScale, Offset(spX, spY))
                                }
                            }
                            BossType.PLAGUE_QUEEN -> {
                                val deathColor = Color(0xFF4A148C).copy(alpha = fadeAlpha)
                                drawScope.drawOval(
                                    deathColor,
                                    Offset(bPos.x - bossW * 0.3f + staggerX, bPos.y - bossH * (1f - collapse * 0.6f)),
                                    Size(bossW * (0.6f - collapse * 0.2f), bossH * (0.9f - collapse * 0.5f))
                                )
                                // Purple spore burst
                                for (s in 0 until 6) {
                                    val spX = bPos.x - bossW * 0.2f + (s * bossW * 0.08f)
                                    val spY = bPos.y - bossH * 0.5f - (collapse * 55f) - (s * 7f)
                                    drawScope.drawCircle(Color(0xFFEA80FC).copy(alpha = (1f - collapse).coerceIn(0f, 1f)), 4f * bScale, Offset(spX, spY))
                                }
                            }
                        }
                    }
                    boss.isDefeated -> {
                        // Defeated state: non-interactive smoking remains on the ground, no health bar or attacks
                        val carcassColor = Color(0xFF1E1E1E).copy(alpha = 0.45f)
                        drawScope.drawOval(
                            carcassColor,
                            Offset(bPos.x - bossW * 0.4f, bPos.y - bossH * 0.22f),
                            Size(bossW * 0.8f, bossH * 0.22f)
                        )
                    }
                    else -> {
                        when (boss.bossType) {
                            BossType.ABOMINATION -> {
                                val baseColor = if (boss.hitFlash > 0) Color.White else Color(0xFF2E7D32)
                                drawScope.drawOval(baseColor, Offset(bPos.x - bossW / 2f, bPos.y - bossH), Size(bossW, bossH * 0.7f))
                                drawScope.drawCircle(Color(0xFF76FF03), bossW * 0.15f, Offset(bPos.x - bossW * 0.2f, bPos.y - bossH * 0.6f))
                                drawScope.drawCircle(Color(0xFFFF1744), 6f * bScale, Offset(bPos.x - bossW * 0.15f, bPos.y - bossH * 0.9f))
                                drawScope.drawCircle(Color(0xFFFF1744), 6f * bScale, Offset(bPos.x + bossW * 0.15f, bPos.y - bossH * 0.9f))
                            }
                            BossType.IRON_TYRANT -> {
                                val armorColor = if (boss.hitFlash > 0) Color.White else Color(0xFF455A64)
                                drawScope.drawRect(armorColor, Offset(bPos.x - bossW / 2f, bPos.y - bossH), Size(bossW, bossH * 0.8f))
                                drawScope.drawRect(Color(0xFF37474F), Offset(bPos.x - bossW * 0.6f, bPos.y - bossH * 0.5f), Size(bossW * 0.3f, bossH * 0.4f))
                                drawScope.drawRect(Color(0xFF00B0FF), Offset(bPos.x - bossW * 0.2f, bPos.y - bossH * 0.9f), Size(bossW * 0.4f, 8f * bScale))
                            }
                            BossType.PLAGUE_QUEEN -> {
                                val queenColor = if (boss.hitFlash > 0) Color.White else Color(0xFF8E24AA)
                                drawScope.drawOval(queenColor, Offset(bPos.x - bossW * 0.3f, bPos.y - bossH), Size(bossW * 0.6f, bossH))
                                drawScope.drawPath(Path().apply { moveTo(bPos.x, bPos.y - bossH); lineTo(bPos.x - bossW * 0.2f, bPos.y - bossH * 0.5f) }, Color(0xFFC6FF00), style = androidx.compose.ui.graphics.drawscope.Stroke(4f * bScale))
                                drawScope.drawCircle(queenColor, bossW * 0.25f, Offset(bPos.x, bPos.y - bossH * 1.05f))
                            }
                        }
                    }
                }

                // Health bar (only rendered while actively alive, hidden when dying or defeated)
                if (!boss.isDying && !boss.isDefeated) {
                    val hpBarW = bossW * 1.2f
                    val hpRatio = (boss.health.toFloat() / boss.maxHealth.toFloat()).coerceIn(0f, 1f)
                    drawScope.drawRect(Color(0xFF212121), Offset(bPos.x - hpBarW / 2f, bPos.y - bossH * 1.2f), Size(hpBarW, 8f * bScale))
                    drawScope.drawRect(Color(0xFFE040FB), Offset(bPos.x - hpBarW / 2f, bPos.y - bossH * 1.2f), Size(hpBarW * hpRatio, 8f * bScale))
                }
            }
        }

        // 10. Zombie Swarms Sorted by Distance
        val sortedZombies = state.zombies.sortedByDescending { it.distanceZ }
        for (zombie in sortedZombies) {
            val z = zombie.distanceZ
            if (z !in -6f..360f) continue
            val (zPos, zScale, zLaneW) = project(zombie.visualLaneX, z, width, height)

            val isRunner = zombie.type == "Runner"
            val isBrute = zombie.type == "Brute"
            val isSpitter = zombie.type == "ToxicSpitter"
            
            val scaleMult = when {
                isBrute -> 1.6f
                isRunner -> 0.8f
                isSpitter -> 0.9f
                else -> 1.0f
            }
            val zW = zLaneW * 0.45f * scaleMult
            val zH = zLaneW * 0.75f * scaleMult

            val bodyColor = when {
                zombie.hitFlash > 0 -> Color.White
                isBrute -> Color(0xFF33691E)
                isRunner -> Color(0xFFBF360C)
                isSpitter -> Color(0xFF388E3C)
                else -> Color(0xFF455A64)
            }
            
            // Running/Stagger animation
            val animPhase = if (zombie.hitFlash > 0) 0f else zombie.animPhase
            val legPhase = (animPhase * 8f)
            val legOffset = sin(legPhase) * (8f * zScale)
            val staggerOffset = if (zombie.hitFlash > 0) sin(zombie.hitFlash * 2f) * 10f * zScale else 0f
            
            drawScope.drawLine(
                color = Color(0xFF1B1B1B),
                start = Offset(zPos.x - zW * 0.2f + staggerOffset, zPos.y - zH * 0.2f),
                end = Offset(zPos.x - zW * 0.2f + legOffset + staggerOffset, zPos.y),
                strokeWidth = 4f * zScale
            )
            drawScope.drawLine(
                color = Color(0xFF1B1B1B),
                start = Offset(zPos.x + zW * 0.2f + staggerOffset, zPos.y - zH * 0.2f),
                end = Offset(zPos.x + zW * 0.2f - legOffset + staggerOffset, zPos.y),
                strokeWidth = 4f * zScale
            )

            // Torso (Layered decaying body with subtle sway)
            val torsoSway = sin(zombie.animPhase * 5f) * 4f
            drawScope.drawPath(Path().apply {
                moveTo(zPos.x - zW / 2f + torsoSway, zPos.y - zH)
                lineTo(zPos.x + zW / 2f + torsoSway, zPos.y - zH)
                lineTo(zPos.x + zW * 0.4f + torsoSway, zPos.y - zH * 0.3f)
                lineTo(zPos.x - zW * 0.4f + torsoSway, zPos.y - zH * 0.3f)
                close()
            }, bodyColor)
            // Infection accent
            drawScope.drawRect(
                color = Color(0x6676FF03),
                topLeft = Offset(zPos.x - zW * 0.3f, zPos.y - zH * 0.8f),
                size = Size(zW * 0.6f, zH * 0.4f)
            )

            if (isBrute) {
                // Riot armor fragments on Brute (Detailed)
                drawScope.drawRect(Color(0xFF37474F), Offset(zPos.x - zW * 0.5f, zPos.y - zH * 0.9f), Size(zW * 1.0f, zH * 0.25f))
                drawScope.drawRect(Color(0xFF263238), Offset(zPos.x - zW * 0.4f, zPos.y - zH * 0.85f), Size(zW * 0.8f, zH * 0.15f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
            }
            
            // Head (More detail)
            val headRadius = zW * (if (isBrute) 0.4f else 0.28f)
            drawScope.drawCircle(
                color = if (isBrute) Color(0xFF1B5E20) else Color(0xFF263238),
                radius = headRadius,
                center = Offset(zPos.x, zPos.y - zH - headRadius * 0.5f)
            )
            // Jaw detail
            drawScope.drawRect(Color(0xFF1B1B1B), Offset(zPos.x - headRadius * 0.4f, zPos.y - zH - headRadius * 0.2f), Size(headRadius * 0.8f, headRadius * 0.5f))


            // Glowing Eyes (Enhanced with slight glow effect)
            val eyeColor = when {
                isRunner -> Color(0xFFFF9100)
                isSpitter -> Color(0xFFC6FF00)
                isBrute -> Color(0xFFFF1744)
                else -> Color(0xFFFFD54F)
            }
            // Primary eye
            drawScope.drawCircle(eyeColor, radius = 3.5f * zScale, center = Offset(zPos.x - headRadius * 0.4f, zPos.y - zH - headRadius * 0.5f))
            drawScope.drawCircle(eyeColor, radius = 3.5f * zScale, center = Offset(zPos.x + headRadius * 0.4f, zPos.y - zH - headRadius * 0.5f))
            // Secondary aura
            drawScope.drawCircle(eyeColor.copy(alpha = 0.4f), radius = 6f * zScale, center = Offset(zPos.x - headRadius * 0.4f, zPos.y - zH - headRadius * 0.5f))
            drawScope.drawCircle(eyeColor.copy(alpha = 0.4f), radius = 6f * zScale, center = Offset(zPos.x + headRadius * 0.4f, zPos.y - zH - headRadius * 0.5f))

            
            if (isSpitter) {
                // Toxic bubbling effect
                val bubblePhase = (System.currentTimeMillis() * 0.01).toFloat()
                drawScope.drawCircle(Color(0xAA76FF03), radius = headRadius * 0.5f + sin(bubblePhase)*2f, center = Offset(zPos.x, zPos.y - zH - headRadius * 1.2f))
            }

            // Zombie Mini Health Bar
            if (zombie.health < zombie.maxHealth) {
                val zHpW = zW * 1.1f
                val zHpRatio = zombie.health.toFloat() / zombie.maxHealth.toFloat()
                drawScope.drawRect(
                    color = Color.Black,
                    topLeft = Offset(zPos.x - zHpW / 2f, zPos.y - zH - headRadius * 2f),
                    size = Size(zHpW, 4f * zScale)
                )
                drawScope.drawRect(
                    color = Color(0xFFFF5252),
                    topLeft = Offset(zPos.x - zHpW / 2f, zPos.y - zH - headRadius * 2f),
                    size = Size(zHpW * zHpRatio, 4f * zScale)
                )
            }
        }

        // 11. Bullets & Tracer Beams Flying Down Lanes
        for (bullet in state.laneBullets) {
            val (bPos, bScale, _) = project(bullet.startLaneX, bullet.distanceZ, width, height)
            val tracerLen = 28f * bScale
            drawScope.drawLine(
                color = bullet.color,
                start = bPos,
                end = Offset(bPos.x, bPos.y - tracerLen),
                strokeWidth = if (bullet.isSquadBullet) 2.5f * bScale else 4.5f * bScale
            )
        }

        // 12. The Survivor Squad at Player Depth (z = 0)
        val (playerPos, _, playerLaneW) = project(state.player.visualLaneX, 0f, width, height)

        // Draw Recruited Squad Members: Elite Modern Special Forces Commandos (True 85–90% Scale)
        val squadCount = state.player.squadCount
        if (squadCount > 1) {
            val memberCount = minOf(16, squadCount - 1)
            val timeSinceFire = System.currentTimeMillis() - state.player.lastFireTimeMs
            val sRecoil = if (timeSinceFire < 70) 5f else 0f
            val isSquadShooting = timeSinceFire < 60

            for (m in 1..memberCount) {
                // Tactical Wedge / Delta formation flanking the Commander (wider spacing for true 85-90% scale)
                // Leaving central corridor (-78f to +78f) clear so road ahead is completely visible
                val isLeftWing = (m % 2 != 0)
                val rank = (m + 1) / 2
                val offsetX = if (isLeftWing) -78f - (rank - 1) * 54f else 78f + (rank - 1) * 54f
                val offsetY = 26f + (rank - 1) * 36f

                val mPos = Offset(playerPos.x + offsetX, playerPos.y + offsetY)
                val sLegAnim = sin((state.player.walkAnimPhase + m * 0.7f) * 10f) * 8f
                val sBob = abs(sLegAnim) * 0.35f

                // Role variation: 0 = Support/Comms, 1 = Rifleman, 2 = Breacher
                val role = m % 3

                // 1. Scaled Ground Shadow (proportional to true 85-90% size)
                drawScope.drawOval(
                    color = Color(0x66000000),
                    topLeft = Offset(mPos.x - 40f, mPos.y + 4f),
                    size = Size(80f, 22f)
                )

                // 2. Combat Boots (Matte Black)
                drawScope.drawRect(
                    color = Color(0xFF0A0A0A),
                    topLeft = Offset(mPos.x - 20f + sLegAnim, mPos.y - 10f),
                    size = Size(16f, 12f)
                )
                drawScope.drawRect(
                    color = Color(0xFF0A0A0A),
                    topLeft = Offset(mPos.x + 4f - sLegAnim, mPos.y - 10f),
                    size = Size(16f, 12f)
                )

                // 3. Tactical Pants & Articulated Legs with Knee Pads
                drawScope.drawLine(
                    color = Color(0xFF212121),
                    start = Offset(mPos.x - 12f, mPos.y - 70f + sBob),
                    end = Offset(mPos.x - 12f + sLegAnim, mPos.y - 8f),
                    strokeWidth = 10f
                )
                drawScope.drawLine(
                    color = Color(0xFF212121),
                    start = Offset(mPos.x + 12f, mPos.y - 70f + sBob),
                    end = Offset(mPos.x + 12f - sLegAnim, mPos.y - 8f),
                    strokeWidth = 10f
                )
                // Reinforced knee pads
                drawScope.drawRect(
                    color = Color(0xFF141414),
                    topLeft = Offset(mPos.x - 17f + sLegAnim * 0.6f, mPos.y - 42f + sBob),
                    size = Size(12f, 12f)
                )
                drawScope.drawRect(
                    color = Color(0xFF141414),
                    topLeft = Offset(mPos.x + 5f - sLegAnim * 0.6f, mPos.y - 42f + sBob),
                    size = Size(12f, 12f)
                )

                // 4. Utility Belt & Mag Pouches
                drawScope.drawRect(
                    color = Color(0xFF0F0F0F),
                    topLeft = Offset(mPos.x - 30f, mPos.y - 84f + sBob),
                    size = Size(60f, 11f)
                )
                // Ammo pouches along belt
                drawScope.drawRect(
                    color = Color(0xFF2B2B2B),
                    topLeft = Offset(mPos.x - 25f, mPos.y - 82f + sBob),
                    size = Size(11f, 9f)
                )
                drawScope.drawRect(
                    color = Color(0xFF2B2B2B),
                    topLeft = Offset(mPos.x - 7.5f, mPos.y - 82f + sBob),
                    size = Size(11f, 9f)
                )
                drawScope.drawRect(
                    color = Color(0xFF2B2B2B),
                    topLeft = Offset(mPos.x + 13f, mPos.y - 82f + sBob),
                    size = Size(11f, 9f)
                )

                // 5. Compact Tactical Backpack & Comms Antenna
                drawScope.drawRect(
                    color = Color(0xFF1A1A1A),
                    topLeft = Offset(mPos.x - 25f, mPos.y - 160f + sBob),
                    size = Size(50f, 60f)
                )
                drawScope.drawRect(
                    color = Color(0xFF282828),
                    topLeft = Offset(mPos.x - 21f, mPos.y - 155f + sBob),
                    size = Size(42f, 50f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(3.8f)
                )
                // Support/Comms role antenna with blinking blue status LED
                if (role == 0) {
                    drawScope.drawLine(
                        color = Color(0xFF121212),
                        start = Offset(mPos.x - 21f, mPos.y - 160f + sBob),
                        end = Offset(mPos.x - 25f, mPos.y - 210f + sBob),
                        strokeWidth = 3.8f
                    )
                    drawScope.drawCircle(
                        color = Color(0xFF00E5FF),
                        radius = 3.8f,
                        center = Offset(mPos.x - 25f, mPos.y - 210f + sBob)
                    )
                }

                // 6. Tactical Armor Torso & Plate Carrier (Matte-Black / Charcoal)
                val commandoTorsoPath = Path().apply {
                    moveTo(mPos.x - 34f, mPos.y - 165f + sBob)
                    lineTo(mPos.x + 34f, mPos.y - 165f + sBob)
                    lineTo(mPos.x + 28f, mPos.y - 84f + sBob)
                    lineTo(mPos.x - 28f, mPos.y - 84f + sBob)
                    close()
                    // Shoulder armor pauldrons
                    moveTo(mPos.x - 36f, mPos.y - 165f + sBob)
                    lineTo(mPos.x - 46f, mPos.y - 140f + sBob)
                    lineTo(mPos.x - 34f, mPos.y - 130f + sBob)
                    moveTo(mPos.x + 36f, mPos.y - 165f + sBob)
                    lineTo(mPos.x + 46f, mPos.y - 140f + sBob)
                    lineTo(mPos.x + 34f, mPos.y - 130f + sBob)
                }
                drawScope.drawPath(commandoTorsoPath, Color(0xFF171717))

                // Plate Carrier Front Chest Plate
                drawScope.drawRect(
                    color = Color(0xFF262626),
                    topLeft = Offset(mPos.x - 23f, mPos.y - 156f + sBob),
                    size = Size(46f, 48f)
                )
                // MOLLE tactical webbing straps
                drawScope.drawLine(
                    color = Color(0xFF111111),
                    start = Offset(mPos.x - 21f, mPos.y - 138f + sBob),
                    end = Offset(mPos.x + 21f, mPos.y - 138f + sBob),
                    strokeWidth = 3.8f
                )
                drawScope.drawLine(
                    color = Color(0xFF111111),
                    start = Offset(mPos.x - 21f, mPos.y - 120f + sBob),
                    end = Offset(mPos.x + 21f, mPos.y - 120f + sBob),
                    strokeWidth = 3.8f
                )

                // 7. Tactical Balaclava & Fast Ballistic Helmet
                // Neck gaiter
                drawScope.drawRect(
                    color = Color(0xFF1C1C1C),
                    topLeft = Offset(mPos.x - 12f, mPos.y - 176f + sBob),
                    size = Size(24f, 15f)
                )
                // Ballistic Helmet dome (True 85-90% scale)
                drawScope.drawCircle(
                    color = Color(0xFF191919),
                    radius = 34f,
                    center = Offset(mPos.x, mPos.y - 205f + sBob)
                )
                // Front NVG mount / accessory shroud
                drawScope.drawRect(
                    color = Color(0xFF2D2D2D),
                    topLeft = Offset(mPos.x - 10f, mPos.y - 212f + sBob),
                    size = Size(20f, 8f)
                )
                // Tactical Visor / Dark Ballistic Goggles
                drawScope.drawRect(
                    color = Color(0xFF212529),
                    topLeft = Offset(mPos.x - 18f, mPos.y - 200f + sBob),
                    size = Size(36f, 16f)
                )
                // Subtle blue/gray lens reflection line
                drawScope.drawLine(
                    color = Color(0xFF455A64),
                    start = Offset(mPos.x - 15f, mPos.y - 195f + sBob),
                    end = Offset(mPos.x + 10f, mPos.y - 195f + sBob),
                    strokeWidth = 3.8f
                )
                // Balaclava lower face mask
                drawScope.drawRect(
                    color = Color(0xFF141414),
                    topLeft = Offset(mPos.x - 14f, mPos.y - 184f + sBob),
                    size = Size(28f, 14f)
                )

                // 8. Modern Military Assault Rifle / Carbine (Held forward with recoil)
                val gunTipX = mPos.x + 35f - sRecoil
                val gunTipY = mPos.y - 120f + sBob
                // Stock against shoulder
                drawScope.drawLine(
                    color = Color(0xFF121212),
                    start = Offset(mPos.x + 6f, mPos.y - 140f + sBob),
                    end = Offset(mPos.x + 16f, mPos.y - 130f + sBob),
                    strokeWidth = 10f
                )
                // Receiver & handguard
                drawScope.drawLine(
                    color = Color(0xFF1E1E1E),
                    start = Offset(mPos.x + 13f, mPos.y - 132f + sBob),
                    end = Offset(gunTipX, gunTipY),
                    strokeWidth = 9f
                )
                // Barrel extension
                drawScope.drawLine(
                    color = Color(0xFF37474F),
                    start = Offset(gunTipX - 8f, gunTipY),
                    end = Offset(gunTipX + 13f, gunTipY),
                    strokeWidth = 5.5f
                )
                // Curved ammo magazine
                drawScope.drawLine(
                    color = Color(0xFF262626),
                    start = Offset(mPos.x + 21f, mPos.y - 125f + sBob),
                    end = Offset(mPos.x + 27f, mPos.y - 110f + sBob),
                    strokeWidth = 6f
                )

                // 9. Synchronized Muzzle Flash when Squad Fires
                if (isSquadShooting && m <= 8) {
                    val muzzlePos = Offset(gunTipX + 16f, gunTipY)
                    // Outer golden flash glow
                    drawScope.drawCircle(
                        color = Color(0xFFFFC107).copy(alpha = 0.65f),
                        radius = 14f,
                        center = muzzlePos
                    )
                    // Inner white core
                    drawScope.drawCircle(
                        color = Color.White,
                        radius = 5.5f,
                        center = muzzlePos
                    )
                }
            }
        }

        // Main Commander: Elite Special Forces Commando (In center)
        val commanderAnim = sin(state.player.walkAnimPhase * 10f) * 6f
        val armorColor = Color(0xFF1B1B1B)
        val armorHighlight = Color(0xFF424242)
        val visorColor = Color(0xFF000000)
        val visorGlow = Color(0xFF1A1A1A)

        // Shadow under player
        drawScope.drawOval(
            color = Color(0x66000000),
            topLeft = Offset(playerPos.x - 30f, playerPos.y + 4f),
            size = Size(60f, 20f)
        )

        // Combat Boots
        drawScope.drawRect(Color(0xFF000000), Offset(playerPos.x - 16f, playerPos.y - 2f), Size(12f, 10f))
        drawScope.drawRect(Color(0xFF000000), Offset(playerPos.x + 4f, playerPos.y - 2f), Size(12f, 10f))

        // Legs (Tactical Pants with slight sway)
        val sway = sin(state.player.runDistance * 2f) * 2f
        drawScope.drawLine(
            color = Color(0xFF212121),
            start = Offset(playerPos.x - 10f, playerPos.y - 12f),
            end = Offset(playerPos.x - 10f + commanderAnim + sway, playerPos.y),
            strokeWidth = 8f
        )
        drawScope.drawLine(
            color = Color(0xFF212121),
            start = Offset(playerPos.x + 10f, playerPos.y - 12f),
            end = Offset(playerPos.x + 10f - commanderAnim - sway, playerPos.y),
            strokeWidth = 8f
        )

        // Tactical Armor Torso (Increased size with added detail)
        val torsoPath = Path().apply {
            // Main torso
            moveTo(playerPos.x - 50f, playerPos.y - 150f)
            lineTo(playerPos.x + 50f, playerPos.y - 150f)
            lineTo(playerPos.x + 45f, playerPos.y - 80f)
            lineTo(playerPos.x - 45f, playerPos.y - 80f)
            close()
            // Shoulder plates
            moveTo(playerPos.x - 55f, playerPos.y - 150f)
            lineTo(playerPos.x - 65f, playerPos.y - 130f)
            lineTo(playerPos.x - 50f, playerPos.y - 120f)
            moveTo(playerPos.x + 55f, playerPos.y - 150f)
            lineTo(playerPos.x + 65f, playerPos.y - 130f)
            lineTo(playerPos.x + 50f, playerPos.y - 120f)
        }
        drawScope.drawPath(torsoPath, armorColor)
        
        // Belt line
        drawScope.drawRect(Color(0xFF000000), Offset(playerPos.x - 45f, playerPos.y - 85f), Size(90f, 6f))
        
        // Chest armor plating
        drawScope.drawRect(
            color = armorHighlight,
            topLeft = Offset(playerPos.x - 36f, playerPos.y - 140f),
            size = Size(72f, 45f)
        )
        // Tactical Straps
        drawScope.drawLine(Color(0xFF000000), Offset(playerPos.x - 45f, playerPos.y - 180f), Offset(playerPos.x - 45f, playerPos.y - 80f), 8f)
        drawScope.drawLine(Color(0xFF000000), Offset(playerPos.x + 45f, playerPos.y - 180f), Offset(playerPos.x + 45f, playerPos.y - 80f), 8f)

        // Combat Backpack (Detailed)
        drawScope.drawRect(
            color = Color(0xFF1A1A1A),
            topLeft = Offset(playerPos.x - 45f, playerPos.y - 175f),
            size = Size(90f, 90f)
        )
        drawScope.drawRect(
            color = Color(0xFF333333),
            topLeft = Offset(playerPos.x - 40f, playerPos.y - 170f),
            size = Size(80f, 80f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(4f)
        )

        // Tactical Helmet with Visor (Larger)
        drawScope.drawCircle(
            color = armorColor,
            radius = 55f,
            center = Offset(playerPos.x, playerPos.y - 210f + abs(commanderAnim) * 0.3f)
        )
        // Combat Visor
        drawScope.drawRect(
            color = visorColor,
            topLeft = Offset(playerPos.x - 35f, playerPos.y - 230f + abs(commanderAnim) * 0.3f),
            size = Size(70f, 35f)
        )
        // Tactical Mask
        drawScope.drawRect(
            color = Color(0xFF333333),
            topLeft = Offset(playerPos.x - 20f, playerPos.y - 195f + abs(commanderAnim) * 0.3f),
            size = Size(40f, 20f)
        )

        // Equipped Weapon Barrel held forward with Recoil
        val timeSinceFire = System.currentTimeMillis() - state.player.lastFireTimeMs
        val recoil = if (timeSinceFire < 80) (80 - timeSinceFire) * 0.2f else 0f
        val gunBarrelTip = Offset(playerPos.x + 40f - recoil, playerPos.y - 100f)
        
        drawScope.drawLine(
            color = Color(0xFF212121),
            start = Offset(playerPos.x + 14f, playerPos.y - 70f),
            end = gunBarrelTip,
            strokeWidth = 14f
        )

        // Muzzle Flash effect (Enhanced)
        if (timeSinceFire < 60) {
            // Inner core
            drawScope.drawCircle(
                color = Color(0xFFFFFFFF),
                radius = 8f,
                center = gunBarrelTip
            )
            // Outer glow
            drawScope.drawCircle(
                color = Color(0xFFFFC107).copy(alpha = 0.6f),
                radius = 20f,
                center = gunBarrelTip
            )
        }

        // 13. Floating Damage Numbers & Effects
        for (effect in state.effects) {
            drawScope.drawCircle(
                color = effect.color,
                radius = effect.size * (effect.ttl / 10f),
                center = effect.position
            )
        }

        drawScope.drawIntoCanvas { canvas ->
            for (damage in state.floatingDamages) {
                textPaintDamage.color = if (damage.isCritical) android.graphics.Color.RED else android.graphics.Color.YELLOW
                textPaintDamage.textSize = if (damage.isCritical) 28f else 22f
                canvas.nativeCanvas.drawText(
                    damage.text,
                    damage.position.x,
                    damage.position.y,
                    textPaintDamage
                )
            }
        }
    }
}
