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
        height: Float
    ) {
        val horizonY = height * 0.28f
        val horizonX = width * 0.5f
        val playerY = height * 0.78f
        val horizonRoadWidth = width * 0.24f
        val bottomRoadWidth = width * 0.92f

        // 1. Apocalyptic Sky & Atmospheric Backdrop (Improved Gradient)
        drawScope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF050508),
                    Color(0xFF101015),
                    Color(0xFF281018),
                    Color(0xFF441822), 
                    Color(0xFF662230)
                ),
                startY = 0f,
                endY = horizonY
            ),
            size = Size(width, horizonY)
        )
        // Atmospheric Haze
        drawScope.drawRect(
            color = Color(0x33333344),
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
                    0 -> 35f
                    1 -> 20f
                    2 -> 45f
                    3 -> 25f
                    else -> 15f
                }
                lineTo(curX, horizonY - bH)
                lineTo(curX + buildingStep, horizonY - bH)
                curX += buildingStep
            }
            lineTo(width, horizonY)
            close()
        }
        drawScope.drawPath(skylinePath, Color(0xFF131118))

        // Red aircraft warning lights on top of ruined skyscrapers
        for (i in 0..5) {
            val lightX = (width * 0.15f) + i * (width * 0.14f)
            val lightY = horizonY - (22f + (i % 3) * 10f)
            drawScope.drawCircle(Color(0xFFFF1744), radius = 2.5f, center = Offset(lightX, lightY))
        }

        // Toxic Ocean / Harbor water below bridge on left & right sides
        drawScope.drawRect(
            color = Color(0xFF0C141C),
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
                color = Color(0x334DD0E1),
                start = Offset(rxLeft - 25f, ry),
                end = Offset(rxLeft + 25f, ry),
                strokeWidth = 2f
            )
            drawScope.drawLine(
                color = Color(0x334DD0E1),
                start = Offset(rxRight - 25f, ry),
                end = Offset(rxRight + 25f, ry),
                strokeWidth = 2f
            )
        }

        // 2. Elevated Highway Bridge Road Polygon
        val roadPath = Path().apply {
            moveTo(horizonX - horizonRoadWidth / 2f, horizonY)
            lineTo(horizonX + horizonRoadWidth / 2f, horizonY)
            lineTo(horizonX + bottomRoadWidth / 2f, height)
            lineTo(horizonX - bottomRoadWidth / 2f, height)
            close()
        }
        drawScope.drawPath(roadPath, Color(0xFF1E2126))

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

        // 9. The Abomination Boss
        state.boss?.let { boss ->
            if (boss.distanceZ in -15f..400f) {
                val (bPos, bScale, bLaneW) = project(boss.visualLaneX, boss.distanceZ, width, height)
                val bossW = bLaneW * 3.2f
                val bossH = bLaneW * 4.0f

                when {
                    boss.isDying -> {
                        // Death animation: collapse and fade
                        val collapse = boss.deathTimer.toFloat() / boss.deathDuration.toFloat()
                        drawScope.drawOval(Color(0xFF424242), Offset(bPos.x - bossW / 2f + collapse * 20f, bPos.y - bossH + collapse * 50f), Size(bossW * (1f - collapse * 0.5f), bossH * (0.8f - collapse * 0.5f)))
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

                // Health bar
                val hpBarW = bossW * 1.2f
                val hpRatio = boss.health.toFloat() / boss.maxHealth.toFloat()
                drawScope.drawRect(Color(0xFF212121), Offset(bPos.x - hpBarW / 2f, bPos.y - bossH * 1.2f), Size(hpBarW, 8f * bScale))
                drawScope.drawRect(Color(0xFFE040FB), Offset(bPos.x - hpBarW / 2f, bPos.y - bossH * 1.2f), Size(hpBarW * hpRatio, 8f * bScale))
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

        // Draw Recruited Squad Members running in formation around the Commander
        val squadCount = state.player.squadCount
        if (squadCount > 1) {
            val memberCount = minOf(16, squadCount - 1)
            for (m in 1..memberCount) {
                // Wedge formation offsets: left wing, right wing, back row
                val isLeftWing = (m % 2 != 0)
                val rank = (m + 1) / 2
                val offsetX = if (isLeftWing) -rank * 26f else rank * 26f
                val offsetY = rank * 22f

                val mPos = Offset(playerPos.x + offsetX, playerPos.y + offsetY)
                val mAnim = sin((state.player.walkAnimPhase + m) * 10f) * 4f

                // Squad member shadow
                drawScope.drawOval(
                    color = Color(0x66000000),
                    topLeft = Offset(mPos.x - 14f, mPos.y + 6f),
                    size = Size(28f, 10f)
                )
                // Tactical blue beret & combat uniform
                drawScope.drawCircle(Color(0xFF0288D1), radius = 9f, center = Offset(mPos.x, mPos.y - 32f + mAnim * 0.4f))
                drawScope.drawRect(
                    color = Color(0xFF1565C0),
                    topLeft = Offset(mPos.x - 11f, mPos.y - 23f + mAnim * 0.3f),
                    size = Size(22f, 20f)
                )
                // Support rifle
                drawScope.drawLine(
                    color = Color(0xFF37474F),
                    start = Offset(mPos.x + 4f, mPos.y - 14f),
                    end = Offset(mPos.x + 16f, mPos.y - 30f),
                    strokeWidth = 3f
                )
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
