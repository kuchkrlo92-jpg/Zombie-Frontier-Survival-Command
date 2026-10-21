package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.GameSettings
import com.example.data.WeaponCatalog
import com.example.data.WeaponDef
import com.example.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

@Composable
fun CombatScreen(
    navController: NavController,
    viewModel: GameViewModel,
    missionId: Int = 1
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        SoundFx.init(context)
    }

    val equippedWeaponData by viewModel.equippedWeapon.collectAsState()
    val (weaponDef, weaponLevel) = equippedWeaponData

    // Settings
    val sensitivity = remember { GameSettings.getJoystickSensitivity(context) }
    val showDamageNumbers = remember { GameSettings.isShowDamageNumbers(context) }
    val isSurvivalMode = (missionId == 100)
    val isBossMission = (missionId == 4)

    val missionTitle = when (missionId) {
        1 -> "Abandoned Highway"
        2 -> "Ruined City"
        3 -> "Military Outpost"
        4 -> "Containment Breach"
        100 -> "Endless Siege"
        else -> "Abandoned Highway"
    }

    val targetKills = when (missionId) {
        1 -> 15
        2 -> 25
        3 -> 35
        4 -> 1 // Defeat The Abomination
        else -> 15
    }

    val (rewardCoins, rewardFood) = when (missionId) {
        1 -> Pair(250L, 100L)
        2 -> Pair(500L, 200L)
        3 -> Pair(1000L, 400L)
        4 -> Pair(1500L, 600L)
        else -> Pair(250L, 100L)
    }

    val magazineCapacity = when (weaponDef.id) {
        1 -> 30 // Assault Rifle
        2 -> 45 // SMG
        3 -> 8  // Shotgun
        4 -> 5  // Sniper
        else -> 30
    }

    val reloadDurationMs = when (weaponDef.id) {
        1 -> 1200L
        2 -> 950L
        3 -> 1600L
        4 -> 1900L
        else -> 1200L
    }

    fun getWaveTotalEnemies(wave: Int): Int = when (wave) {
        1 -> 10
        2 -> 16
        3 -> 21
        4 -> 28
        5 -> 7 // Boss (The Abomination) + 6 escorts
        else -> 10 + wave * 5
    }

    fun createInitialState(): CombatState {
        val obstacles = when {
            isSurvivalMode -> listOf(
                Obstacle(Offset(340f, 300f), 85f, 45f, "DEFENSE SANDBAGS"),
                Obstacle(Offset(600f, 520f), 100f, 50f, "BURNING BARRIER"),
                Obstacle(Offset(750f, 280f), 75f, 75f, "CONCRETE SLAB")
            )
            isBossMission -> listOf(
                Obstacle(Offset(320f, 260f), 90f, 50f, "CRYO TANK"),
                Obstacle(Offset(680f, 580f), 90f, 50f, "BIO-CONTAINER")
            )
            missionId == 1 -> listOf(
                Obstacle(Offset(350f, 320f), 90f, 45f, "WRECKED SEDAN"),
                Obstacle(Offset(600f, 540f), 110f, 55f, "BURNING TRUCK")
            )
            missionId == 2 -> listOf(
                Obstacle(Offset(320f, 280f), 80f, 80f, "RUBBLE PILE"),
                Obstacle(Offset(580f, 480f), 90f, 60f, "COLLAPSED WALL"),
                Obstacle(Offset(750f, 300f), 70f, 70f, "CRACKED SLAB")
            )
            else -> listOf(
                Obstacle(Offset(300f, 260f), 100f, 40f, "SANDBAG BUNKER"),
                Obstacle(Offset(520f, 550f), 120f, 30f, "BARBED BARRIER"),
                Obstacle(Offset(720f, 340f), 90f, 40f, "COMMAND TRENCH")
            )
        }

        val initialBoss = if (isBossMission) {
            BossState(
                name = "THE ABOMINATION",
                position = Offset(880f, 480f),
                health = 1200,
                maxHealth = 1200
            )
        } else null

        return CombatState(
            missionId = missionId,
            targetKills = targetKills,
            missionTitle = missionTitle,
            weaponDef = weaponDef,
            weaponLevel = weaponLevel,
            obstacles = obstacles,
            isSurvivalMode = isSurvivalMode,
            currentWave = 1,
            maxWaves = 5,
            waveEnemiesTotal = if (isSurvivalMode) getWaveTotalEnemies(1) else targetKills,
            waveEnemiesSpawned = 0,
            waveAnnouncement = if (isSurvivalMode) "WAVE 1 // SURVIVE THE HORDE" else null,
            waveAnnouncementTtl = if (isSurvivalMode) 60 else 0,
            boss = initialBoss,
            player = PlayerState(
                position = Offset(140f, 480f),
                health = 100,
                maxHealth = 100,
                ammoCurrent = magazineCapacity,
                ammoMax = magazineCapacity,
                reloadDurationMs = reloadDurationMs
            )
        )
    }

    var combatState by remember { mutableStateOf(createInitialState()) }
    var isHoldingFire by remember { mutableStateOf(false) }

    // Reload Action
    fun triggerReload() {
        if (combatState.player.isReloading || combatState.player.ammoCurrent == combatState.player.ammoMax) return
        val now = System.currentTimeMillis()
        SoundFx.playReload(context)
        GameSettings.triggerVibration(context, 25)
        combatState = combatState.copy(
            player = combatState.player.copy(
                isReloading = true,
                reloadTimerMs = now
            )
        )
    }

    // Fire helper
    fun triggerFire() {
        if (combatState.isGameOver || combatState.isVictory || combatState.isPaused) return
        val player = combatState.player

        if (player.isReloading) return

        if (player.ammoCurrent <= 0) {
            triggerReload()
            return
        }

        val now = System.currentTimeMillis()
        if (now - player.lastFireTimeMs < weaponDef.fireRateMs) return

        val bulletDamage = weaponDef.calculateDamage(weaponLevel)
        val newProjectiles = mutableListOf<Projectile>()
        val aimAngle = player.facingAngle
        val fireOrigin = player.position + Offset(cos(aimAngle) * 26f, sin(aimAngle) * 26f)

        if (weaponDef.pellets == 1) {
            val angleSpread = if (weaponDef.spreadAngle > 0f) {
                ((-10..10).random() / 100f) * weaponDef.spreadAngle
            } else 0f
            val totalAngle = aimAngle + angleSpread
            val vx = weaponDef.projectileSpeed * cos(totalAngle)
            val vy = weaponDef.projectileSpeed * sin(totalAngle)

            val isCrit = Random.nextFloat() < 0.15f
            val finalDmg = if (isCrit) (bulletDamage * 1.5f).toInt() else bulletDamage

            newProjectiles.add(
                Projectile(
                    position = fireOrigin,
                    velocity = Offset(vx, vy),
                    damage = finalDmg,
                    rangeRemaining = weaponDef.range,
                    size = weaponDef.projectileSize,
                    color = Color(weaponDef.colorHex),
                    isCritical = isCrit
                )
            )
        } else {
            // Shotgun Spread
            val spreadStep = weaponDef.spreadAngle / (weaponDef.pellets - 1)
            for (i in 0 until weaponDef.pellets) {
                val offset = -weaponDef.spreadAngle / 2f + i * spreadStep + ((-5..5).random() / 200f)
                val totalAngle = aimAngle + offset
                val vx = weaponDef.projectileSpeed * cos(totalAngle)
                val vy = weaponDef.projectileSpeed * sin(totalAngle)

                val isCrit = Random.nextFloat() < 0.12f
                val finalDmg = if (isCrit) (bulletDamage * 1.4f).toInt() else bulletDamage

                newProjectiles.add(
                    Projectile(
                        position = fireOrigin,
                        velocity = Offset(vx, vy),
                        damage = finalDmg,
                        rangeRemaining = weaponDef.range,
                        size = weaponDef.projectileSize,
                        color = Color(weaponDef.colorHex),
                        isCritical = isCrit
                    )
                )
            }
        }

        SoundFx.playGunshot(context, weaponDef.type)
        val vibeDuration = when (weaponDef.id) {
            3 -> 30L
            4 -> 40L
            else -> 12L
        }
        GameSettings.triggerVibration(context, vibeDuration)

        val flashEffect = Effect(
            position = fireOrigin,
            type = "Flash",
            ttl = 4,
            color = Color(0xFFFFF176),
            size = 18f
        )

        val ejectAngle = aimAngle - PI.toFloat() / 2f + ((-20..20).random() / 100f)
        val ejectVel = Offset(cos(ejectAngle) * 3.5f, sin(ejectAngle) * 3.5f)
        val shell = ShellCasing(
            position = player.position + Offset(cos(aimAngle) * 8f, sin(aimAngle) * 8f),
            velocity = ejectVel,
            rotation = Random.nextFloat() * 360f,
            ttl = 28
        )

        val nextAmmo = player.ammoCurrent - 1
        combatState = combatState.copy(
            player = player.copy(
                lastFireTimeMs = now,
                ammoCurrent = nextAmmo
            ),
            projectiles = combatState.projectiles + newProjectiles,
            effects = combatState.effects + flashEffect,
            shellCasings = (combatState.shellCasings + shell).takeLast(20),
            totalShotsFired = combatState.totalShotsFired + (if (weaponDef.pellets > 1) weaponDef.pellets else 1)
        )

        if (nextAmmo <= 0) {
            triggerReload()
        }
    }

    // Main 60FPS Game Loop
    LaunchedEffect(missionId, weaponDef, weaponLevel) {
        var lastSpawnTime = System.currentTimeMillis()

        while (!combatState.isGameOver && !combatState.isVictory) {
            if (combatState.isPaused) {
                delay(100)
                continue
            }
            delay(16) // ~60 FPS

            val now = System.currentTimeMillis()

            // 0. Hold-to-fire execution
            if (isHoldingFire) {
                triggerFire()
            }

            // 1. Update Player Reload State
            var currentPlayer = combatState.player
            if (currentPlayer.isReloading) {
                if (now - currentPlayer.reloadTimerMs >= currentPlayer.reloadDurationMs) {
                    currentPlayer = currentPlayer.copy(
                        isReloading = false,
                        ammoCurrent = currentPlayer.ammoMax
                    )
                }
            }

            // 2. Move Player & Aiming
            val moveSpeed = 5.2f * sensitivity
            val hasMovement = combatState.joystickInput.getDistance() > 0.05f
            val nextX = (currentPlayer.position.x + combatState.joystickInput.x * moveSpeed).coerceIn(40f, 960f)
            val nextY = (currentPlayer.position.y + combatState.joystickInput.y * moveSpeed).coerceIn(180f, 820f)
            val nextWalkAnim = if (hasMovement) currentPlayer.walkAnimPhase + 0.22f else currentPlayer.walkAnimPhase

            // Auto-aim towards nearest living threat (Boss takes priority if near, then zombies)
            val livingZombies = combatState.zombies.filter { !it.isDying }
            val currentBoss = combatState.boss
            val targetPos = if (currentBoss != null && !currentBoss.isDying && (currentBoss.position - currentPlayer.position).getDistance() < 550f) {
                currentBoss.position
            } else {
                val nearestZombie = livingZombies.minByOrNull { (it.position - currentPlayer.position).getDistance() }
                if (nearestZombie != null && (nearestZombie.position - currentPlayer.position).getDistance() < 550f) {
                    nearestZombie.position
                } else null
            }

            val targetAngle = if (targetPos != null) {
                val diff = targetPos - currentPlayer.position
                atan2(diff.y, diff.x)
            } else if (hasMovement) {
                atan2(combatState.joystickInput.y, combatState.joystickInput.x)
            } else {
                currentPlayer.facingAngle
            }

            val currentAngle = currentPlayer.facingAngle
            var angleDiff = (targetAngle - currentAngle) % (2f * PI.toFloat())
            if (angleDiff > PI.toFloat()) angleDiff -= 2f * PI.toFloat()
            if (angleDiff < -PI.toFloat()) angleDiff += 2f * PI.toFloat()
            val newFacingAngle = currentAngle + angleDiff * 0.28f

            currentPlayer = currentPlayer.copy(
                position = Offset(nextX, nextY),
                facingAngle = newFacingAngle,
                walkAnimPhase = nextWalkAnim
            )

            // 3. Wave Transition & Enemy Spawning Engine
            var currentWave = combatState.currentWave
            var waveEnemiesTotal = combatState.waveEnemiesTotal
            var waveEnemiesSpawned = combatState.waveEnemiesSpawned
            var isWaveBreak = combatState.isWaveBreak
            var waveBreakTimer = combatState.waveBreakTimer
            var waveAnnouncement = combatState.waveAnnouncement
            var waveAnnouncementTtl = (combatState.waveAnnouncementTtl - 1).coerceAtLeast(0)
            if (waveAnnouncementTtl <= 0) waveAnnouncement = null

            var currentZombies = combatState.zombies
            var updatedBoss = combatState.boss

            // Handle Inter-Wave Break in Survival Mode
            if (isSurvivalMode && isWaveBreak) {
                waveBreakTimer--
                if (waveBreakTimer <= 0) {
                    isWaveBreak = false
                    currentWave++
                    waveEnemiesTotal = getWaveTotalEnemies(currentWave)
                    waveEnemiesSpawned = 0
                    waveAnnouncement = "WAVE $currentWave // INCOMING!"
                    waveAnnouncementTtl = 65
                    SoundFx.playBossRoar(context)

                    // Spawn boss if Wave 5
                    if (currentWave == 5 && updatedBoss == null) {
                        updatedBoss = BossState(
                            name = "THE ABOMINATION",
                            position = Offset(900f, 480f),
                            health = 1200,
                            maxHealth = 1200
                        )
                    }
                }
            }

            // Normal Zombie Spawning
            val livingCount = currentZombies.count { !it.isDying }
            val canSpawn = if (isSurvivalMode) {
                !isWaveBreak && waveEnemiesSpawned < waveEnemiesTotal && livingCount < 10
            } else if (isBossMission) {
                livingCount < 3 // Maintain small escort group
            } else {
                combatState.killCount + currentZombies.size < combatState.targetKills + 5 && livingCount < 10
            }

            val spawnInterval = when {
                isSurvivalMode -> (900L - (currentWave * 70L)).coerceAtLeast(550L)
                missionId == 2 -> 900L
                missionId >= 3 -> 750L
                else -> 1200L
            }

            if (canSpawn && (now - lastSpawnTime > spawnInterval)) {
                lastSpawnTime = now
                val spawnY = (220..780).random().toFloat()
                val spawnPos = Offset(1080f, spawnY)

                val newZombie = when {
                    isSurvivalMode -> {
                        val rand = (0..100).random()
                        when (currentWave) {
                            1 -> Zombie((0..99999).random(), spawnPos, 100, 100, "Walker", 2.0f, 10, 16f)
                            2 -> {
                                if (rand < 70) Zombie((0..99999).random(), spawnPos, 105, 105, "Walker", 2.1f, 10, 16f)
                                else Zombie((0..99999).random(), spawnPos, 65, 65, "Runner", 4.3f, 8, 13f)
                            }
                            3 -> {
                                if (rand < 50) Zombie((0..99999).random(), spawnPos, 115, 115, "Walker", 2.2f, 12, 16f)
                                else if (rand < 85) Zombie((0..99999).random(), spawnPos, 75, 75, "Runner", 4.5f, 10, 13f)
                                else Zombie((0..99999).random(), spawnPos, 320, 320, "Brute", 1.25f, 28, 26f)
                            }
                            4 -> {
                                if (rand < 35) Zombie((0..99999).random(), spawnPos, 130, 130, "Walker", 2.4f, 14, 16f)
                                else if (rand < 75) Zombie((0..99999).random(), spawnPos, 85, 85, "Runner", 4.8f, 12, 13f)
                                else Zombie((0..99999).random(), spawnPos, 380, 380, "Brute", 1.35f, 32, 28f)
                            }
                            else -> {
                                // Wave 5 Escorts
                                if (rand < 65) Zombie((0..99999).random(), spawnPos, 90, 90, "Runner", 4.7f, 12, 13f)
                                else Zombie((0..99999).random(), spawnPos, 350, 350, "Brute", 1.3f, 30, 27f)
                            }
                        }
                    }
                    isBossMission -> {
                        // Boss minions
                        Zombie((0..99999).random(), spawnPos, 90, 90, "Runner", 4.6f, 10, 13f)
                    }
                    missionId == 1 -> {
                        val rand = (0..100).random()
                        if (rand < 75) Zombie((0..99999).random(), spawnPos, 100, 100, "Walker", 2.0f, 10, 16f)
                        else Zombie((0..99999).random(), spawnPos, 60, 60, "Runner", 4.3f, 8, 13f)
                    }
                    missionId == 2 -> {
                        val rand = (0..100).random()
                        if (rand < 45) Zombie((0..99999).random(), spawnPos, 115, 115, "Walker", 2.2f, 12, 16f)
                        else if (rand < 82) Zombie((0..99999).random(), spawnPos, 70, 70, "Runner", 4.6f, 10, 13f)
                        else Zombie((0..99999).random(), spawnPos, 320, 320, "Brute", 1.2f, 28, 26f)
                    }
                    else -> {
                        val rand = (0..100).random()
                        if (rand < 30) Zombie((0..99999).random(), spawnPos, 130, 130, "Walker", 2.4f, 15, 17f)
                        else if (rand < 70) Zombie((0..99999).random(), spawnPos, 85, 85, "Runner", 4.9f, 12, 14f)
                        else Zombie((0..99999).random(), spawnPos, 420, 420, "Brute", 1.35f, 35, 28f)
                    }
                }
                currentZombies = currentZombies + newZombie
                if (isSurvivalMode) waveEnemiesSpawned++
            }

            // 4. Move Projectiles and Check Obstacles
            val newEffects = combatState.effects.map {
                it.copy(
                    ttl = it.ttl - 1,
                    position = it.position + it.velocity
                )
            }.filter { it.ttl > 0 }.toMutableList()

            val movedProjectiles = mutableListOf<Projectile>()
            for (p in combatState.projectiles) {
                val nextPos = p.position + p.velocity
                val rem = p.rangeRemaining - p.velocity.getDistance()

                var hitObstacle = false
                for (obs in combatState.obstacles) {
                    if (nextPos.x >= obs.topLeft.x && nextPos.x <= obs.topLeft.x + obs.width &&
                        nextPos.y >= obs.topLeft.y && nextPos.y <= obs.topLeft.y + obs.height
                    ) {
                        hitObstacle = true
                        newEffects.add(
                            Effect(
                                position = nextPos,
                                type = "Spark",
                                ttl = 8,
                                color = Color(0xFFFFD54F),
                                size = 8f,
                                velocity = Offset((-3..3).random().toFloat(), (-3..3).random().toFloat())
                            )
                        )
                        break
                    }
                }

                if (!hitObstacle && rem > 0 && nextPos.x < 1150f && nextPos.x > -50f && nextPos.y > 140f && nextPos.y < 880f) {
                    movedProjectiles.add(p.copy(position = nextPos, rangeRemaining = rem))
                }
            }

            // 5. Boss State Machine & Attack Execution
            var playerDamageTaken = 0
            var newShakeIntensity = combatState.screenShakeIntensity * 0.85f
            val newShockwaves = combatState.shockwaves.map {
                it.copy(
                    radius = it.radius + (it.maxRadius - it.radius) * 0.25f,
                    ttl = it.ttl - 1
                )
            }.filter { it.ttl > 0 }.toMutableList()

            if (updatedBoss != null) {
                var b = updatedBoss
                if (b.isDying) {
                    val dt = b.deathTimer - 1
                    if (dt > 0) {
                        updatedBoss = b.copy(deathTimer = dt)
                    } else {
                        // Boss permanently collapsed
                        combatState.bloodDecals.toMutableList().add(
                            BloodDecal(
                                position = b.position,
                                radius = 55f,
                                color = Color(0xAA6A1B9A),
                                alpha = 0.8f
                            )
                        )
                        updatedBoss = null
                    }
                } else {
                    val bDiff = currentPlayer.position - b.position
                    val bDist = bDiff.getDistance()
                    val bDir = if (bDist > 0.1f) bDiff / bDist else Offset(-1f, 0f)
                    val bFacing = atan2(bDir.y, bDir.x)

                    var slamCd = (b.slamCooldown - 1).coerceAtLeast(0)
                    var chargeCd = (b.chargeCooldown - 1).coerceAtLeast(0)
                    var summonCd = (b.summonCooldown - 1).coerceAtLeast(0)
                    var atkCd = (b.attackCooldown - 1).coerceAtLeast(0)
                    var pTimer = b.patternTimer
                    var pattern = b.currentPattern
                    var bPos = b.position
                    var chargeDir = b.chargeDirection
                    var hasDamagedInCharge = b.hasDamagedInCharge
                    var tProg = b.telegraphProgress

                    when (pattern) {
                        BossPattern.IDLE -> {
                            // Steer toward player
                            if (bDist > 55f) {
                                bPos += bDir * b.speed
                            }

                            // Pattern Decision Tree
                            if (summonCd <= 0 && currentZombies.count { !it.isDying } < 3) {
                                pattern = BossPattern.TELEGRAPH_SUMMON
                                pTimer = 26
                                tProg = 0f
                                SoundFx.playBossRoar(context)
                            } else if (bDist in 45f..145f && slamCd <= 0) {
                                pattern = BossPattern.TELEGRAPH_SLAM
                                pTimer = 24
                                tProg = 0f
                            } else if (bDist > 160f && chargeCd <= 0) {
                                pattern = BossPattern.TELEGRAPH_CHARGE
                                pTimer = 28
                                tProg = 0f
                                chargeDir = bDir
                            } else if (bDist <= 55f && atkCd <= 0) {
                                // Basic melee punch
                                playerDamageTaken += 16
                                atkCd = 35
                                GameSettings.triggerVibration(context, 45)
                            }
                        }
                        BossPattern.TELEGRAPH_SLAM -> {
                            pTimer--
                            tProg = (24 - pTimer) / 24f
                            if (pTimer <= 0) {
                                pattern = BossPattern.EXECUTE_SLAM
                                pTimer = 10
                                SoundFx.playBossSlam(context)
                                newShakeIntensity = 18f
                                val slamCenter = bPos + bDir * 28f
                                newShockwaves.add(
                                    Shockwave(
                                        position = slamCenter,
                                        radius = 16f,
                                        maxRadius = 110f,
                                        ttl = 18,
                                        color = Color(0xFFFF1744)
                                    )
                                )
                                // Damage if player inside warning radius
                                if ((currentPlayer.position - slamCenter).getDistance() <= 100f) {
                                    playerDamageTaken += 32
                                    GameSettings.triggerVibration(context, 80)
                                }
                                slamCd = 110
                            }
                        }
                        BossPattern.EXECUTE_SLAM -> {
                            pTimer--
                            if (pTimer <= 0) pattern = BossPattern.IDLE
                        }
                        BossPattern.TELEGRAPH_CHARGE -> {
                            pTimer--
                            tProg = (28 - pTimer) / 28f
                            if (pTimer <= 0) {
                                pattern = BossPattern.EXECUTE_CHARGE
                                pTimer = 22
                                hasDamagedInCharge = false
                                SoundFx.playBossRoar(context)
                            }
                        }
                        BossPattern.EXECUTE_CHARGE -> {
                            pTimer--
                            bPos = Offset(
                                (bPos.x + chargeDir.x * 9.5f).coerceIn(60f, 980f),
                                (bPos.y + chargeDir.y * 9.5f).coerceIn(180f, 820f)
                            )
                            // Single hit damage safeguard
                            if (!hasDamagedInCharge && (currentPlayer.position - bPos).getDistance() <= (b.size + 20f)) {
                                playerDamageTaken += 36
                                hasDamagedInCharge = true
                                newShakeIntensity = 15f
                                GameSettings.triggerVibration(context, 90)
                            }
                            if (pTimer <= 0) {
                                pattern = BossPattern.IDLE
                                chargeCd = 160
                            }
                        }
                        BossPattern.TELEGRAPH_SUMMON -> {
                            pTimer--
                            tProg = (26 - pTimer) / 26f
                            if (pTimer <= 0) {
                                pattern = BossPattern.EXECUTE_SUMMON
                                pTimer = 10
                                // Spawn reinforcements
                                val s1 = Zombie((0..99999).random(), Offset(1050f, 320f), 90, 90, "Runner", 4.7f, 12, 13f)
                                val s2 = Zombie((0..99999).random(), Offset(1050f, 620f), 110, 110, "Walker", 2.3f, 12, 16f)
                                currentZombies = currentZombies + listOf(s1, s2)
                                summonCd = 240
                            }
                        }
                        BossPattern.EXECUTE_SUMMON -> {
                            pTimer--
                            if (pTimer <= 0) pattern = BossPattern.IDLE
                        }
                    }

                    val nextFlash = (b.hitFlash - 1).coerceAtLeast(0)
                    updatedBoss = b.copy(
                        position = bPos,
                        facingAngle = bFacing,
                        currentPattern = pattern,
                        patternTimer = pTimer,
                        telegraphProgress = tProg,
                        slamCooldown = slamCd,
                        chargeCooldown = chargeCd,
                        summonCooldown = summonCd,
                        attackCooldown = atkCd,
                        chargeDirection = chargeDir,
                        hasDamagedInCharge = hasDamagedInCharge,
                        hitFlash = nextFlash
                    )
                }
            }

            // 6. Update Regular Zombies (Movement, Attacks, Dying)
            val updatedZombies = mutableListOf<Zombie>()
            val newBloodDecals = combatState.bloodDecals.toMutableList()

            for (z in currentZombies) {
                if (z.isDying) {
                    val nextDeathTimer = z.deathTimer - 1
                    if (nextDeathTimer > 0) {
                        updatedZombies.add(z.copy(deathTimer = nextDeathTimer))
                    } else {
                        newBloodDecals.add(
                            BloodDecal(
                                position = z.position,
                                radius = z.size * 1.3f,
                                color = if (z.type == "Brute") Color(0x994A148C) else Color(0x998B0000),
                                alpha = 0.65f,
                                seed = (0..100).random()
                            )
                        )
                    }
                    continue
                }

                // Normal zombie behavior
                val diff = currentPlayer.position - z.position
                val dist = diff.getDistance()
                val dir = if (dist > 0.1f) diff / dist else Offset(-1f, 0f)
                val zFacing = atan2(dir.y, dir.x)

                val animSpeedMult = when (z.type) {
                    "Runner" -> 0.32f
                    "Brute" -> 0.12f
                    else -> 0.20f
                }
                val nextAnimPhase = z.animPhase + animSpeedMult
                val currentKnockback = z.knockback * 0.68f

                var isLunging = z.isLunging
                var lungeTimer = z.lungeTimer
                var actualSpeed = z.speed

                if (z.type == "Runner") {
                    if (!isLunging && dist in 45f..95f && z.attackCooldown <= 5) {
                        isLunging = true
                        lungeTimer = 10
                    }
                    if (isLunging) {
                        actualSpeed = z.speed * 1.8f
                        lungeTimer--
                        if (lungeTimer <= 0) isLunging = false
                    }
                }

                val targetMovePos = if (dist > 36f) {
                    z.position + dir * actualSpeed + currentKnockback
                } else {
                    z.position + currentKnockback
                }

                var isAttacking = z.isAttacking
                var attackProg = z.attackProgress
                var cd = z.attackCooldown

                if (!isWaveBreak && dist <= 44f) {
                    if (cd <= 0) {
                        isAttacking = true
                        attackProg = 0f
                        cd = when (z.type) {
                            "Runner" -> 22
                            "Brute" -> 55
                            else -> 36
                        }
                    }
                }

                if (isAttacking) {
                    attackProg += when (z.type) {
                        "Runner" -> 0.25f
                        "Brute" -> 0.08f
                        else -> 0.14f
                    }
                    if (attackProg >= 0.5f && attackProg - (if (z.type == "Brute") 0.08f else 0.14f) < 0.5f) {
                        if (!isWaveBreak && dist <= 48f) {
                            playerDamageTaken += z.damage
                            if (z.type == "Brute") {
                                newShakeIntensity = 12f
                                newShockwaves.add(
                                    Shockwave(
                                        position = z.position,
                                        radius = 12f,
                                        maxRadius = 70f,
                                        ttl = 15,
                                        color = Color(0xCCFF5722)
                                    )
                                )
                            }
                        }
                    }
                    if (attackProg >= 1f) {
                        isAttacking = false
                        attackProg = 0f
                    }
                }

                if (cd > 0) cd--
                val nextFlash = (z.hitFlash - 1).coerceAtLeast(0)

                updatedZombies.add(
                    z.copy(
                        position = targetMovePos,
                        facingAngle = zFacing,
                        animPhase = nextAnimPhase,
                        knockback = currentKnockback,
                        attackCooldown = cd,
                        isAttacking = isAttacking,
                        attackProgress = attackProg,
                        isLunging = isLunging,
                        lungeTimer = lungeTimer,
                        hitFlash = nextFlash
                    )
                )
            }

            // 7. Bullet-Zombie & Bullet-Boss Collisions
            val survivingProjectiles = movedProjectiles.toMutableList()
            val newDamages = combatState.floatingDamages.map {
                it.copy(
                    ttl = it.ttl - 1,
                    position = it.position + Offset(0f, -1.3f)
                )
            }.filter { it.ttl > 0 }.toMutableList()

            val newHitMarkers = combatState.hitMarkers.map {
                it.copy(ttl = it.ttl - 1)
            }.filter { it.ttl > 0 }.toMutableList()

            var additionalKills = 0
            var additionalHits = 0
            val remainingZombies = updatedZombies.toMutableList()

            val projIterator = survivingProjectiles.iterator()
            while (projIterator.hasNext()) {
                val bullet = projIterator.next()
                var bulletConsumed = false

                // 7A. Check Boss Collision
                if (updatedBoss != null && !updatedBoss.isDying) {
                    val bossDist = (updatedBoss.position - bullet.position).getDistance()
                    if (bossDist < (updatedBoss.size + bullet.size + 4f)) {
                        bulletConsumed = true
                        additionalHits++
                        val newHp = updatedBoss.health - bullet.damage
                        SoundFx.playZombieHit(context)
                        newHitMarkers.add(HitMarker(bullet.position, ttl = 5))

                        newEffects.add(
                            Effect(
                                position = bullet.position,
                                type = "BloodSpray",
                                ttl = 8,
                                color = Color(0xFF6A1B9A),
                                size = 12f,
                                velocity = bullet.velocity * 0.2f
                            )
                        )

                        if (showDamageNumbers) {
                            newDamages.add(
                                FloatingDamage(
                                    text = if (bullet.isCritical) "CRIT -${bullet.damage}!" else "-${bullet.damage}",
                                    position = updatedBoss.position + Offset((-15..15).random().toFloat(), -32f),
                                    color = if (bullet.isCritical) Color(0xFFFF1744) else Color(0xFFFFEE58),
                                    ttl = 24,
                                    isCritical = bullet.isCritical
                                )
                            )
                        }

                        if (newHp <= 0) {
                            additionalKills++
                            SoundFx.playZombieDeath(context)
                            updatedBoss = updatedBoss.copy(
                                health = 0,
                                isDying = true,
                                deathTimer = 45,
                                hitFlash = 8
                            )
                        } else {
                            updatedBoss = updatedBoss.copy(
                                health = newHp,
                                hitFlash = 5
                            )
                        }
                    }
                }

                // 7B. Check Regular Zombie Collisions
                if (!bulletConsumed) {
                    val zombieIterator = remainingZombies.iterator()
                    while (zombieIterator.hasNext()) {
                        val zombie = zombieIterator.next()
                        if (zombie.isDying) continue

                        val hitDist = (zombie.position - bullet.position).getDistance()
                        if (hitDist < (zombie.size + bullet.size + 4f)) {
                            bulletConsumed = true
                            additionalHits++
                            val newHp = zombie.health - bullet.damage

                            SoundFx.playZombieHit(context)
                            newHitMarkers.add(HitMarker(bullet.position, ttl = 5))

                            newEffects.add(
                                Effect(
                                    position = bullet.position,
                                    type = "BloodSpray",
                                    ttl = 8,
                                    color = Color(0xFFD32F2F),
                                    size = 10f,
                                    velocity = bullet.velocity * 0.25f
                                )
                            )

                            if (showDamageNumbers) {
                                newDamages.add(
                                    FloatingDamage(
                                        text = if (bullet.isCritical) "CRIT -${bullet.damage}!" else "-${bullet.damage}",
                                        position = zombie.position + Offset((-10..10).random().toFloat(), -18f),
                                        color = if (bullet.isCritical) Color(0xFFFF1744) else Color(0xFFFFEE58),
                                        ttl = 24,
                                        isCritical = bullet.isCritical
                                    )
                                )
                            }

                            val bulletDir = if (bullet.velocity.getDistance() > 0.01f) bullet.velocity / bullet.velocity.getDistance() else Offset(1f, 0f)
                            val knockbackPower = when (weaponDef.id) {
                                3 -> 16f
                                4 -> 20f
                                else -> 8f
                            }
                            val resistance = if (zombie.type == "Brute") 0.25f else 1.0f
                            val actualKnockback = bulletDir * (knockbackPower * resistance)

                            if (newHp <= 0) {
                                additionalKills++
                                SoundFx.playZombieDeath(context)
                                val idx = remainingZombies.indexOf(zombie)
                                if (idx != -1) {
                                    remainingZombies[idx] = zombie.copy(
                                        health = 0,
                                        isDying = true,
                                        deathTimer = 16,
                                        hitFlash = 6,
                                        knockback = actualKnockback * 1.5f
                                    )
                                }
                            } else {
                                val idx = remainingZombies.indexOf(zombie)
                                if (idx != -1) {
                                    remainingZombies[idx] = zombie.copy(
                                        health = newHp,
                                        hitFlash = 5,
                                        knockback = actualKnockback
                                    )
                                }
                            }
                            break
                        }
                    }
                }

                if (bulletConsumed) {
                    projIterator.remove()
                }
            }

            // 8. Combo Streaks
            var comboStreak = combatState.comboStreak
            var comboBanner = combatState.comboBanner?.let {
                if (it.ttl - 1 > 0) it.copy(ttl = it.ttl - 1) else null
            }

            if (additionalKills > 0) {
                val timeSinceLastKill = now - combatState.lastKillTimeMs
                if (timeSinceLastKill < 2800L) {
                    comboStreak += additionalKills
                } else {
                    comboStreak = additionalKills
                }

                if (comboStreak >= 2) {
                    val (title, color) = when (comboStreak) {
                        2 -> Pair("DOUBLE KILL!", Color(0xFF4CAF50))
                        3 -> Pair("TRIPLE KILL!", Color(0xFF00E5FF))
                        4 -> Pair("RAMPAGE!", Color(0xFFFF9100))
                        else -> Pair("MASSACRE!", Color(0xFFE040FB))
                    }
                    comboBanner = ComboBanner(
                        title = title,
                        subtitle = "$comboStreak KILLS IN A ROW",
                        color = color,
                        ttl = 45
                    )
                    SoundFx.playCombo(context)
                    GameSettings.triggerVibration(context, 35)
                }
            }

            // 9. Player Damage Feedback
            val newPlayerHealth = (currentPlayer.health - playerDamageTaken).coerceAtLeast(0)
            if (playerDamageTaken > 0) {
                SoundFx.playPlayerHurt(context)
                GameSettings.triggerVibration(context, 70)
                newShakeIntensity = (newShakeIntensity + 8f).coerceAtMost(18f)
                newEffects.add(
                    Effect(
                        position = currentPlayer.position,
                        type = "PlayerHit",
                        ttl = 7,
                        color = Color(0x66FF0000),
                        size = 55f
                    )
                )
            }

            // 10. Screen Shake Offset
            val screenShakeOffset = if (newShakeIntensity > 0.5f) {
                val rx = (Random.nextFloat() * 2f - 1f) * newShakeIntensity * 0.7f
                val ry = (Random.nextFloat() * 2f - 1f) * newShakeIntensity * 0.7f
                Offset(rx, ry)
            } else Offset.Zero

            // 11. Shell Casings Update
            val newShells = combatState.shellCasings.map {
                it.copy(
                    position = it.position + it.velocity,
                    velocity = it.velocity * 0.88f,
                    rotation = it.rotation + 18f,
                    ttl = it.ttl - 1
                )
            }.filter { it.ttl > 0 }

            // 12. Check Objectives, Waves, and Victories
            val totalKills = combatState.killCount + additionalKills
            val totalShotsHit = combatState.totalShotsHit + additionalHits
            var wavesClearedInRun = combatState.wavesClearedInRun
            var isVictoryNow = false
            var isGameOverNow = (newPlayerHealth <= 0)

            if (isSurvivalMode) {
                val aliveCount = remainingZombies.count { !it.isDying } + (if (updatedBoss != null && !updatedBoss.isDying) 1 else 0)
                if (waveEnemiesSpawned >= waveEnemiesTotal && aliveCount == 0 && !isWaveBreak) {
                    if (currentWave < 5) {
                        // Start wave break
                        isWaveBreak = true
                        waveBreakTimer = 180 // 3 seconds
                        wavesClearedInRun = currentWave
                        waveAnnouncement = "WAVE $currentWave COMPLETED! RECOVERING..."
                        waveAnnouncementTtl = 65
                        SoundFx.playCombo(context)
                        // Health recovery bonus
                        currentPlayer = currentPlayer.copy(
                            health = (currentPlayer.health + 15).coerceAtMost(currentPlayer.maxHealth)
                        )
                    } else if (currentWave == 5) {
                        // Defeated Wave 5 Boss!
                        wavesClearedInRun = 5
                        isVictoryNow = true
                    }
                }
            } else if (isBossMission) {
                // Boss Mission victory condition: The Abomination is dead
                if (updatedBoss != null && updatedBoss.isDying) {
                    isVictoryNow = true
                }
            } else {
                // Regular Mission Objective & Extraction
                val extractionActive = totalKills >= combatState.targetKills
                val playerInExtraction = extractionActive &&
                        currentPlayer.position.x >= 880f &&
                        currentPlayer.position.y in 200f..780f
                isVictoryNow = playerInExtraction && newPlayerHealth > 0
            }

            // Award Rewards on Game End
            var earnedCoins = combatState.survivalCoinsEarned
            var earnedFood = combatState.survivalFoodEarned

            if (isVictoryNow && !combatState.isVictory) {
                GameSettings.triggerVibration(context, 100)
                if (isSurvivalMode) {
                    viewModel.claimSurvivalRewards(5, totalKills.toLong()) { c, f ->
                        earnedCoins = c
                        earnedFood = f
                    }
                } else {
                    viewModel.completeMission(missionId, totalKills.toLong())
                }
            }

            if (isGameOverNow && !combatState.isGameOver) {
                GameSettings.triggerVibration(context, 140)
                if (isSurvivalMode && wavesClearedInRun > 0) {
                    viewModel.claimSurvivalRewards(wavesClearedInRun, totalKills.toLong()) { c, f ->
                        earnedCoins = c
                        earnedFood = f
                    }
                }
            }

            val extractionActive = if (!isSurvivalMode && !isBossMission) totalKills >= combatState.targetKills else false

            combatState = combatState.copy(
                player = currentPlayer.copy(health = newPlayerHealth),
                zombies = remainingZombies,
                boss = updatedBoss,
                projectiles = survivingProjectiles,
                effects = newEffects,
                floatingDamages = newDamages,
                bloodDecals = newBloodDecals.takeLast(25),
                shellCasings = newShells,
                hitMarkers = newHitMarkers,
                shockwaves = newShockwaves,
                comboBanner = comboBanner,
                comboStreak = comboStreak,
                lastKillTimeMs = if (additionalKills > 0) now else combatState.lastKillTimeMs,
                screenShake = screenShakeOffset,
                screenShakeIntensity = newShakeIntensity,
                killCount = totalKills,
                totalShotsHit = totalShotsHit,
                currentWave = currentWave,
                waveEnemiesTotal = waveEnemiesTotal,
                waveEnemiesSpawned = waveEnemiesSpawned,
                isWaveBreak = isWaveBreak,
                waveBreakTimer = waveBreakTimer,
                waveAnnouncement = waveAnnouncement,
                waveAnnouncementTtl = waveAnnouncementTtl,
                wavesClearedInRun = wavesClearedInRun,
                survivalCoinsEarned = earnedCoins,
                survivalFoodEarned = earnedFood,
                extractionActive = extractionActive,
                isVictory = isVictoryNow,
                isGameOver = isGameOverNow
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0C0E11))) {
        // Main Battle Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { _, dragAmount ->
                            combatState = combatState.copy(joystickInput = dragAmount / 12f)
                        },
                        onDragEnd = {
                            combatState = combatState.copy(joystickInput = Offset.Zero)
                        }
                    )
                }
        ) {
            withTransform({
                translate(combatState.screenShake.x, combatState.screenShake.y)
            }) {
                // 1. Draw Battlefield
                drawBattlefield(missionId, size)

                // 2. Draw Blood Decals
                combatState.bloodDecals.forEach { b ->
                    drawCircle(
                        color = b.color.copy(alpha = b.alpha),
                        radius = b.radius,
                        center = b.position
                    )
                    drawCircle(
                        color = b.color.copy(alpha = b.alpha * 0.8f),
                        radius = b.radius * 0.35f,
                        center = b.position + Offset(b.radius * 0.8f, b.radius * 0.4f)
                    )
                }

                // 3. Draw Shell Casings
                combatState.shellCasings.forEach { s ->
                    rotate(s.rotation, s.position) {
                        drawRect(
                            color = Color(0xFFFFD54F),
                            topLeft = s.position - Offset(4f, 1.5f),
                            size = Size(8f, 3f)
                        )
                    }
                }

                // 4. Draw Obstacles
                combatState.obstacles.forEach { obs ->
                    drawRect(
                        color = Color(0x66000000),
                        topLeft = obs.topLeft + Offset(6f, 6f),
                        size = Size(obs.width, obs.height)
                    )
                    drawRect(
                        color = Color(0xFF2B343D),
                        topLeft = obs.topLeft,
                        size = Size(obs.width, obs.height)
                    )
                    drawRect(
                        color = Color(0xFF455A64),
                        topLeft = obs.topLeft,
                        size = Size(obs.width, obs.height),
                        style = Stroke(width = 2.5f)
                    )
                    drawLine(
                        color = Color(0xFFFFB300),
                        start = obs.topLeft + Offset(8f, 8f),
                        end = obs.topLeft + Offset(obs.width - 8f, obs.height - 8f),
                        strokeWidth = 2f
                    )
                }

                // 5. Draw Extraction Zone (Campaign missions only)
                if (combatState.extractionActive) {
                    val extractionRect = Size(110f, 580f)
                    val extractionTopLeft = Offset(880f, 200f)

                    drawRect(
                        color = Color(0x334CAF50),
                        topLeft = extractionTopLeft,
                        size = extractionRect
                    )
                    drawRect(
                        color = Color(0xFF4CAF50),
                        topLeft = extractionTopLeft,
                        size = extractionRect,
                        style = Stroke(width = 3f)
                    )
                    drawCircle(
                        color = Color(0xFF81C784),
                        radius = 28f,
                        center = Offset(935f, 490f)
                    )
                }

                // 6. Draw Boss Telegraph Warning Indicators
                combatState.boss?.let { b ->
                    if (!b.isDying) {
                        when (b.currentPattern) {
                            BossPattern.TELEGRAPH_SLAM -> {
                                // Red pulsing circular danger zone
                                val slamTarget = b.position + Offset(cos(b.facingAngle) * 30f, sin(b.facingAngle) * 30f)
                                drawCircle(
                                    color = Color(0x44FF1744),
                                    radius = 100f * b.telegraphProgress,
                                    center = slamTarget
                                )
                                drawCircle(
                                    color = Color(0xFFFF1744),
                                    radius = 100f,
                                    center = slamTarget,
                                    style = Stroke(width = 3f)
                                )
                            }
                            BossPattern.TELEGRAPH_CHARGE -> {
                                // Directional danger lane
                                val laneLength = 380f * b.telegraphProgress
                                val start = b.position
                                val end = b.position + b.chargeDirection * laneLength
                                drawLine(
                                    color = Color(0x88FF1744),
                                    start = start,
                                    end = end,
                                    strokeWidth = 50f
                                )
                                drawLine(
                                    color = Color(0xFFFF5252),
                                    start = start,
                                    end = end,
                                    strokeWidth = 4f
                                )
                            }
                            BossPattern.TELEGRAPH_SUMMON -> {
                                // Toxic summon aura
                                drawCircle(
                                    color = Color(0x3300E676),
                                    radius = 90f * b.telegraphProgress,
                                    center = b.position
                                )
                                drawCircle(
                                    color = Color(0xFF00E676),
                                    radius = 90f,
                                    center = b.position,
                                    style = Stroke(width = 2.5f)
                                )
                            }
                            else -> {}
                        }
                    }
                }

                // 7. Draw Shockwaves
                combatState.shockwaves.forEach { sw ->
                    val alpha = (sw.ttl / 18f).coerceIn(0f, 1f)
                    drawCircle(
                        color = sw.color.copy(alpha = alpha),
                        radius = sw.radius,
                        center = sw.position,
                        style = Stroke(width = 5f)
                    )
                }

                // 8. Draw Regular Zombies
                combatState.zombies.forEach { z ->
                    drawZombieCharacter(z)
                }

                // 9. Draw The Abomination Boss
                combatState.boss?.let { b ->
                    drawBossCharacter(b)
                }

                // 10. Draw Projectiles with Luminous Bullet Trails
                combatState.projectiles.forEach { p ->
                    val trailLength = 0.75f
                    val trailStart = p.position - p.velocity * trailLength
                    drawLine(
                        color = p.color.copy(alpha = 0.45f),
                        start = trailStart,
                        end = p.position,
                        strokeWidth = p.size * 1.2f
                    )
                    drawCircle(
                        color = Color.White,
                        radius = p.size * 0.65f,
                        center = p.position
                    )
                    drawCircle(
                        color = p.color,
                        radius = p.size,
                        center = p.position
                    )
                }

                // 11. Draw Player Character
                drawPlayerCharacter(combatState.player, weaponDef)

                // 12. Draw Visual Effects
                combatState.effects.forEach { eff ->
                    when (eff.type) {
                        "Flash" -> {
                            drawCircle(eff.color, radius = eff.size, center = eff.position)
                            drawCircle(Color.White, radius = eff.size * 0.4f, center = eff.position)
                        }
                        "BloodSpray" -> {
                            val alpha = (eff.ttl / 8f).coerceIn(0f, 1f)
                            drawCircle(eff.color.copy(alpha = alpha), radius = eff.size, center = eff.position)
                        }
                        "Spark" -> {
                            val alpha = (eff.ttl / 8f).coerceIn(0f, 1f)
                            drawCircle(eff.color.copy(alpha = alpha), radius = eff.size * 0.7f, center = eff.position)
                        }
                        "PlayerHit" -> {
                            drawCircle(eff.color, radius = eff.size, center = eff.position)
                        }
                    }
                }

                // 13. Draw Hit Markers
                combatState.hitMarkers.forEach { hm ->
                    val markerSize = 6f
                    drawLine(
                        color = Color.White,
                        start = hm.position - Offset(markerSize, markerSize),
                        end = hm.position + Offset(markerSize, markerSize),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color.White,
                        start = hm.position - Offset(-markerSize, markerSize),
                        end = hm.position + Offset(-markerSize, markerSize),
                        strokeWidth = 2f
                    )
                }

                // 14. Draw Floating Damage Numbers
                if (showDamageNumbers) {
                    combatState.floatingDamages.forEach { dmg ->
                        val paint = android.graphics.Paint().apply {
                            color = if (dmg.isCritical) android.graphics.Color.RED else android.graphics.Color.YELLOW
                            textSize = if (dmg.isCritical) 32f else 26f
                            isFakeBoldText = true
                            alpha = (dmg.ttl * 10).coerceIn(0, 255)
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            dmg.text,
                            dmg.position.x,
                            dmg.position.y,
                            paint
                        )
                    }
                }
            }
        }

        // Low Health Vignette Overlay
        if (combatState.player.health < 35 && !combatState.isGameOver) {
            val pulse = (sin(System.currentTimeMillis() / 150.0) * 0.15 + 0.25).toFloat()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(16.dp, Color.Red.copy(alpha = pulse))
            )
        }

        // Inter-Wave / Combo Banner Announcements
        combatState.waveAnnouncement?.let { announcement ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 115.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color(0xEE12161B),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.border(2.dp, Color(0xFFFFB300), RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = announcement,
                        color = Color(0xFFFFB300),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }
        }

        combatState.comboBanner?.let { banner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 160.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color(0xEE12161B),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.border(2.dp, banner.color, RoundedCornerShape(8.dp))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = banner.title,
                            color = banner.color,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = banner.subtitle,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // PROFESSIONAL COMBAT TOP HUD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Top Status Bar
            Surface(
                color = Color(0xEE11151A),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF28323D), RoundedCornerShape(8.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (isSurvivalMode) Color(0xFF4A148C) else Color(0xFF1B3B2B),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (isSurvivalMode) "SURVIVAL MODE" else "OPERATION 0$missionId",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSurvivalMode) Color(0xFFE040FB) else Color(0xFF81C784),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = missionTitle.uppercase(),
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        // Wave / Objective info
                        if (isSurvivalMode) {
                            val alive = combatState.zombies.count { !it.isDying } + (if (combatState.boss != null && !combatState.boss!!.isDying) 1 else 0)
                            val remaining = (combatState.waveEnemiesTotal - combatState.waveEnemiesSpawned + alive).coerceAtLeast(0)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "WAVE ${combatState.currentWave} / ${combatState.maxWaves}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = Color(0xFFFFB300)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = if (combatState.isWaveBreak) "RECOVERING..." else "ENEMIES: $remaining LEFT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (combatState.isWaveBreak) Color(0xFF81C784) else Color(0xFF4DD0E1)
                                )
                            }
                        } else if (isBossMission) {
                            Text(
                                text = "OBJECTIVE: DESTROY THE ABOMINATION",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFFEF5350)
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "KILLS: ${combatState.killCount}/${combatState.targetKills}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4DD0E1)
                                )
                                if (combatState.extractionActive) {
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = "EXTRACTION OPEN ➔",
                                        color = Color(0xFF81C784),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }

                    // Tactical Pause Button
                    IconButton(
                        onClick = { combatState = combatState.copy(isPaused = !combatState.isPaused) }
                    ) {
                        Icon(
                            imageVector = if (combatState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = Color.White
                        )
                    }
                }
            }

            // DEDICATED BOSS HEALTH BAR (When Boss is Active)
            combatState.boss?.let { b ->
                if (!b.isDying) {
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        color = Color(0xEE1A121E),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, Color(0xFFAB47BC), RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⚠️", fontSize = 13.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = b.name,
                                        color = Color(0xFFE040FB),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        letterSpacing = 1.sp
                                    )
                                }

                                val patternLabel = when (b.currentPattern) {
                                    BossPattern.TELEGRAPH_SLAM -> "⚠️ PREPARING SLAM"
                                    BossPattern.EXECUTE_SLAM -> "💥 SLAM IMPACT!"
                                    BossPattern.TELEGRAPH_CHARGE -> "⚡ CHARGE WINDUP"
                                    BossPattern.EXECUTE_CHARGE -> "🏃 CHARGING!"
                                    BossPattern.TELEGRAPH_SUMMON -> "📢 CALLING HORDE"
                                    else -> "MUTANT ALPHA"
                                }

                                Surface(
                                    color = Color(0xFF38153F),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = patternLabel,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFD54F),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            // Boss Bar
                            val bossHpRatio = (b.health.toFloat() / b.maxHealth).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .background(Color(0xFF261D2B), RoundedCornerShape(5.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(bossHpRatio)
                                        .background(
                                            if (bossHpRatio > 0.4f) Color(0xFFAB47BC) else Color(0xFFE53935),
                                            RoundedCornerShape(5.dp)
                                        )
                                )
                            }

                            Spacer(Modifier.height(3.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${b.health} / ${b.maxHealth} HP",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${(bossHpRatio * 100).toInt()}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFCE93D8)
                                )
                            }
                        }
                    }
                }
            }
        }

        // BOTTOM TACTICAL CONTROLS & HUD
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Left: Virtual Joystick
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(Color(0x33263238), CircleShape)
                    .border(2.dp, Color(0x6678909C), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val thumbOffset = combatState.joystickInput * 30f
                Box(
                    modifier = Modifier
                        .offset(x = thumbOffset.x.dp, y = thumbOffset.y.dp)
                        .size(46.dp)
                        .background(Color(0xAA4CAF50), CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                )
                Text("MOVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0x88FFFFFF))
            }

            // Center: Compact Player Health HUD
            Surface(
                color = Color(0xDD11161B),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .border(1.dp, Color(0xFF28343E), RoundedCornerShape(8.dp))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val hpRatio = (combatState.player.health.toFloat() / combatState.player.maxHealth).coerceIn(0f, 1f)
                    Text(
                        text = "HP: ${combatState.player.health} / ${combatState.player.maxHealth}",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = if (hpRatio > 0.35f) Color(0xFF81C784) else Color(0xFFFF5252)
                    )
                    Spacer(Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .width(84.dp)
                            .height(6.dp)
                            .background(Color(0xFF263238), RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(hpRatio)
                                .background(
                                    if (hpRatio > 0.35f) Color(0xFF4CAF50) else Color(0xFFE53935),
                                    RoundedCornerShape(3.dp)
                                )
                        )
                    }
                }
            }

            // Right: Weapon & Ammunition Armory Controls
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tactical Reload Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { triggerReload() },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xDD263238), CircleShape)
                            .border(1.5.dp, Color(0xFF78909C), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reload",
                            tint = if (combatState.player.isReloading) Color(0xFFFFB300) else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text("RELOAD", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB0BEC5))
                }

                // Fire Button & Ammo Status
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = Color(0xDD191E24),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.border(1.dp, Color(0xFF333E48), RoundedCornerShape(4.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${weaponDef.codeName} LVL $weaponLevel",
                                color = Color(weaponDef.colorHex),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (combatState.player.isReloading) "RELOADING..." else "${combatState.player.ammoCurrent} / ${combatState.player.ammoMax}",
                                color = if (combatState.player.isReloading) Color(0xFFFFB300) else if (combatState.player.ammoCurrent <= 5) Color(0xFFFF5252) else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .background(if (isHoldingFire) Color(0xFFB71C1C) else Color(0xFFD32F2F), CircleShape)
                            .border(2.5.dp, Color(0xFFFF5252), CircleShape)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isHoldingFire = true
                                        tryAwaitRelease()
                                        isHoldingFire = false
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("FIRE", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                            Text(
                                "${weaponDef.calculateDamage(weaponLevel)} DMG",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD54F)
                            )
                        }
                    }
                }
            }
        }

        // PAUSE DIALOG
        if (combatState.isPaused && !combatState.isGameOver && !combatState.isVictory) {
            AlertDialog(
                onDismissRequest = { combatState = combatState.copy(isPaused = false) },
                title = { Text("MISSION PAUSED", fontWeight = FontWeight.Bold, color = Color(0xFFFFB300)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isSurvivalMode) "Mode: Wave Survival (Wave ${combatState.currentWave}/5)" else "Objective: $missionTitle",
                            color = Color(0xFFB0BEC5),
                            fontSize = 13.sp
                        )
                        Text("Current Kills: ${combatState.killCount}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Equipped: ${weaponDef.name} (LVL $weaponLevel)", color = Color(weaponDef.colorHex), fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { combatState = combatState.copy(isPaused = false) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("RESUME", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { combatState = createInitialState() }) {
                            Text("RETRY", color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { navController.popBackStack() }) {
                            Text("BASE", color = Color(0xFF90A4AE))
                        }
                    }
                },
                containerColor = Color(0xFF1A1F24),
                shape = RoundedCornerShape(8.dp)
            )
        }

        // RESULTS OVERLAY: SURVIVAL MODE OR CAMPAIGN
        if (isSurvivalMode && (combatState.isGameOver || combatState.isVictory)) {
            val accuracy = if (combatState.totalShotsFired > 0) {
                (combatState.totalShotsHit.toFloat() / combatState.totalShotsFired * 100f).coerceIn(0f, 100f)
            } else 0f

            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text(
                        text = if (combatState.isVictory) "SURVIVAL VICTORY!" else "SURVIVAL RUN OVER",
                        fontWeight = FontWeight.Black,
                        color = if (combatState.isVictory) Color(0xFF4CAF50) else Color(0xFFE53935)
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (combatState.isVictory) "You purged all 5 waves and eliminated THE ABOMINATION!" else "Squad succumbed on Wave ${combatState.currentWave}.",
                            color = Color(0xFFB0BEC5),
                            fontSize = 13.sp
                        )

                        Surface(
                            color = Color(0xFF14191F),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(1.dp, Color(0xFF263238), RoundedCornerShape(6.dp))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Waves Cleared:", fontSize = 11.sp, color = Color(0xFF90A4AE))
                                    Text("${combatState.wavesClearedInRun} / 5 Waves", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Zombies Slain:", fontSize = 11.sp, color = Color(0xFF90A4AE))
                                    Text("${combatState.killCount}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4DD0E1))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Weapon Accuracy:", fontSize = 11.sp, color = Color(0xFF90A4AE))
                                    Text("${String.format("%.1f", accuracy)}% (${combatState.totalShotsHit}/${combatState.totalShotsFired})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
                                }
                            }
                        }

                        if (combatState.survivalCoinsEarned > 0 || combatState.survivalFoodEarned > 0) {
                            Surface(
                                color = Color(0xFF1B2E1D),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("MILESTONE REWARDS: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                                    Text("+${combatState.survivalCoinsEarned}🪙  +${combatState.survivalFoodEarned}🍞", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300))
                                }
                            }
                        } else {
                            Text(
                                "No new wave milestones reached. Try clearing higher waves to earn more rewards!",
                                fontSize = 10.sp,
                                color = Color(0xFF78909C)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { combatState = createInitialState() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0277BD))
                    ) {
                        Text("RETRY SURVIVAL", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { navController.popBackStack() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F))
                    ) {
                        Text("RETURN TO BASE")
                    }
                },
                containerColor = Color(0xFF191E23),
                shape = RoundedCornerShape(8.dp)
            )
        } else if (!isSurvivalMode) {
            // REGULAR MISSION DEFEAT
            if (combatState.isGameOver) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("MISSION FAILED", fontWeight = FontWeight.Bold, color = Color(0xFFE53935)) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Your squad was overwhelmed by the horde. Player health reached zero.", color = Color(0xFFB0BEC5), fontSize = 13.sp)
                            Text("Final Kills: ${combatState.killCount}/$targetKills", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Rewards: None awarded on defeat.", color = Color(0xFFEF5350), fontSize = 11.sp)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { combatState = createInitialState() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0277BD))
                        ) {
                            Text("RETRY MISSION", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = { navController.popBackStack() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F))
                        ) {
                            Text("RETURN TO BASE")
                        }
                    },
                    containerColor = Color(0xFF1E1719),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            // REGULAR MISSION VICTORY
            if (combatState.isVictory) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("MISSION COMPLETE!", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50)) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                if (isBossMission) "THE ABOMINATION has been eradicated! Regional contamination contained." else "Squad successfully evacuated through the extraction point!",
                                color = Color(0xFFB0BEC5),
                                fontSize = 13.sp
                            )
                            Text("Zombies Eliminated: ${combatState.killCount}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                color = Color(0xFF1B2E1D),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("REWARDS EARNED: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                                    Text("+$rewardCoins 🪙  +$rewardFood 🍞", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300))
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { combatState = createInitialState() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Text("PLAY AGAIN", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = { navController.popBackStack() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8F00))
                        ) {
                            Text("RETURN TO BASE", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = Color(0xFF172019),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}

// Procedural environment rendering
private fun DrawScope.drawBattlefield(missionId: Int, size: Size) {
    when (missionId) {
        100 -> {
            // Dark Industrial Siege Zone
            drawRect(Color(0xFF191D21))
            // Cross-hatch hazard warning markings
            for (x in 60..size.width.toInt() step 100) {
                drawLine(Color(0xFF262D35), start = Offset(x.toFloat(), 180f), end = Offset(x.toFloat(), 820f), strokeWidth = 2f)
            }
            // Perimeter defense borders
            drawRect(Color(0xFF4527A0), topLeft = Offset(0f, 160f), size = Size(size.width, 8f))
            drawRect(Color(0xFF4527A0), topLeft = Offset(0f, 840f), size = Size(size.width, 8f))
        }
        4 -> {
            // Bio-Hazard Containment Lab
            drawRect(Color(0xFF15181C))
            // Lab floor grid tile pattern
            for (x in 80..size.width.toInt() step 120) {
                drawLine(Color(0xFF1F262E), start = Offset(x.toFloat(), 160f), end = Offset(x.toFloat(), 840f), strokeWidth = 2f)
            }
            for (y in 200..800 step 100) {
                drawLine(Color(0xFF1F262E), start = Offset(0f, y.toFloat()), end = Offset(size.width, y.toFloat()), strokeWidth = 2f)
            }
            // Bio-hazard yellow/black barrier warning line
            drawRect(Color(0xFFFFB300), topLeft = Offset(0f, 160f), size = Size(size.width, 6f))
            drawRect(Color(0xFFFFB300), topLeft = Offset(0f, 840f), size = Size(size.width, 6f))
        }
        1 -> {
            // Damaged Asphalt Highway
            drawRect(Color(0xFF1F2327))
            drawLine(Color(0xFF181B1E), start = Offset(200f, 250f), end = Offset(240f, 320f), strokeWidth = 2f)
            drawLine(Color(0xFF181B1E), start = Offset(520f, 620f), end = Offset(580f, 660f), strokeWidth = 2f)

            for (x in 60..size.width.toInt() step 90) {
                drawRect(
                    color = Color(0x66FFD54F),
                    topLeft = Offset(x.toFloat(), size.height / 2f - 4f),
                    size = Size(50f, 8f)
                )
            }
            drawRect(Color(0xFF37474F), topLeft = Offset(0f, 150f), size = Size(size.width, 8f))
            drawRect(Color(0xFF37474F), topLeft = Offset(0f, 850f), size = Size(size.width, 8f))
        }
        2 -> {
            // Ruined City Streets
            drawRect(Color(0xFF1A1C1F))
            for (x in 100..size.width.toInt() step 140) {
                drawLine(Color(0xFF262A2F), start = Offset(x.toFloat(), 150f), end = Offset(x.toFloat(), 850f), strokeWidth = 2f)
            }
            for (y in 200..800 step 120) {
                drawLine(Color(0xFF262A2F), start = Offset(0f, y.toFloat()), end = Offset(size.width, y.toFloat()), strokeWidth = 2f)
            }
        }
        else -> {
            // Military Outpost Sand & Metal Ground
            drawRect(Color(0xFF22241F))
            drawRect(Color(0xFF3A382C), topLeft = Offset(0f, 160f), size = Size(size.width, 14f))
            drawRect(Color(0xFF3A382C), topLeft = Offset(0f, 830f), size = Size(size.width, 14f))
            for (x in 80..size.width.toInt() step 120) {
                drawLine(Color(0xFF424233), start = Offset(x.toFloat(), 480f), end = Offset(x + 30f, 520f), strokeWidth = 3f)
            }
        }
    }
}

// Draw Player Character with Tactical Armor & Moving Legs
private fun DrawScope.drawPlayerCharacter(player: PlayerState, weaponDef: WeaponDef) {
    val angleDeg = Math.toDegrees(player.facingAngle.toDouble()).toFloat()

    rotate(angleDeg, player.position) {
        val stride = sin(player.walkAnimPhase.toDouble()).toFloat() * 6f
        drawCircle(
            color = Color(0xFF1E2832),
            radius = 5.5f,
            center = player.position + Offset(-6f + stride, -10f)
        )
        drawCircle(
            color = Color(0xFF1E2832),
            radius = 5.5f,
            center = player.position + Offset(-6f - stride, 10f)
        )

        drawRect(
            color = Color(0xFF2C3E50),
            topLeft = player.position + Offset(-18f, -7f),
            size = Size(8f, 14f)
        )

        drawCircle(
            color = Color(0xFF1565C0),
            radius = 16f,
            center = player.position
        )
        drawCircle(
            color = Color(0xFF0D47A1),
            radius = 11f,
            center = player.position
        )

        drawCircle(
            color = Color(0xFF37474F),
            radius = 9f,
            center = player.position + Offset(3f, 0f)
        )
        drawRect(
            color = Color(0xFF00E5FF),
            topLeft = player.position + Offset(7f, -4f),
            size = Size(3.5f, 8f)
        )

        drawCircle(
            color = Color(0xFFE0E0E0),
            radius = 4f,
            center = player.position + Offset(14f, -7f)
        )
        drawCircle(
            color = Color(0xFFE0E0E0),
            radius = 4f,
            center = player.position + Offset(14f, 7f)
        )

        val weaponColor = Color(weaponDef.colorHex)
        when (weaponDef.id) {
            1 -> {
                drawLine(
                    color = Color(0xFF212121),
                    start = player.position + Offset(10f, 3f),
                    end = player.position + Offset(26f, 3f),
                    strokeWidth = 5f
                )
                drawLine(
                    color = weaponColor,
                    start = player.position + Offset(20f, 3f),
                    end = player.position + Offset(27f, 3f),
                    strokeWidth = 3f
                )
            }
            2 -> {
                drawLine(
                    color = Color(0xFF263238),
                    start = player.position + Offset(10f, 3f),
                    end = player.position + Offset(21f, 3f),
                    strokeWidth = 6f
                )
                drawLine(
                    color = weaponColor,
                    start = player.position + Offset(16f, 3f),
                    end = player.position + Offset(22f, 3f),
                    strokeWidth = 3.5f
                )
            }
            3 -> {
                drawLine(
                    color = Color(0xFF3E2723),
                    start = player.position + Offset(8f, 3f),
                    end = player.position + Offset(18f, 3f),
                    strokeWidth = 6.5f
                )
                drawLine(
                    color = weaponColor,
                    start = player.position + Offset(18f, 3f),
                    end = player.position + Offset(25f, 3f),
                    strokeWidth = 5f
                )
            }
            else -> {
                drawLine(
                    color = Color(0xFF212121),
                    start = player.position + Offset(10f, 3f),
                    end = player.position + Offset(32f, 3f),
                    strokeWidth = 4.5f
                )
                drawRect(
                    color = weaponColor,
                    topLeft = player.position + Offset(15f, 0f),
                    size = Size(8f, 3f)
                )
            }
        }
    }
}

// Draw Zombie Character
private fun DrawScope.drawZombieCharacter(z: Zombie) {
    val angleDeg = Math.toDegrees(z.facingAngle.toDouble()).toFloat()

    if (z.isDying) {
        val deathRatio = (z.deathTimer / z.deathDuration.toFloat()).coerceIn(0f, 1f)
        rotate(angleDeg + (1f - deathRatio) * 45f, z.position) {
            drawCircle(
                color = Color(0xFF3E2723).copy(alpha = deathRatio * 0.7f),
                radius = z.size * deathRatio,
                center = z.position
            )
        }
        return
    }

    rotate(angleDeg, z.position) {
        val isFlashing = z.hitFlash > 0

        val (skinColor, clothesColor) = when (z.type) {
            "Runner" -> Pair(Color(0xFFA93226), Color(0xFF424242))
            "Brute" -> Pair(Color(0xFF4A148C), Color(0xFF212121))
            else -> Pair(Color(0xFF4E6B48), Color(0xFF37474F))
        }

        val renderSkinColor = if (isFlashing) Color.White else skinColor

        val legStride = sin(z.animPhase.toDouble()).toFloat() * (if (z.type == "Runner") 7f else 4f)
        drawCircle(
            color = clothesColor,
            radius = z.size * 0.28f,
            center = z.position + Offset(-z.size * 0.4f + legStride, -z.size * 0.5f)
        )
        drawCircle(
            color = clothesColor,
            radius = z.size * 0.28f,
            center = z.position + Offset(-z.size * 0.4f - legStride, z.size * 0.5f)
        )

        drawCircle(
            color = renderSkinColor,
            radius = z.size,
            center = z.position
        )
        drawCircle(
            color = if (isFlashing) Color.White else clothesColor,
            radius = z.size * 0.65f,
            center = z.position - Offset(z.size * 0.2f, 0f)
        )

        if (z.type == "Brute") {
            drawRect(
                color = Color(0xFF616161),
                topLeft = z.position + Offset(-z.size * 0.8f, -z.size * 0.9f),
                size = Size(z.size * 0.5f, z.size * 0.4f)
            )
            drawRect(
                color = Color(0xFF616161),
                topLeft = z.position + Offset(-z.size * 0.8f, z.size * 0.5f),
                size = Size(z.size * 0.5f, z.size * 0.4f)
            )
        }

        val armSwing = cos(z.animPhase.toDouble()).toFloat() * 3f
        val attackReach = if (z.isAttacking) z.attackProgress * z.size * 0.8f else 0f

        drawCircle(
            color = renderSkinColor,
            radius = z.size * 0.25f,
            center = z.position + Offset(z.size * 0.7f + attackReach, -z.size * 0.5f + armSwing)
        )
        drawCircle(
            color = renderSkinColor,
            radius = z.size * 0.25f,
            center = z.position + Offset(z.size * 0.7f + attackReach, z.size * 0.5f - armSwing)
        )

        drawCircle(
            color = renderSkinColor,
            radius = z.size * 0.55f,
            center = z.position + Offset(z.size * 0.3f, 0f)
        )
        val eyeColor = if (z.type == "Runner") Color(0xFFFFEB3B) else Color(0xFFFF1744)
        drawCircle(
            color = eyeColor,
            radius = 2.2f,
            center = z.position + Offset(z.size * 0.65f, -z.size * 0.22f)
        )
        drawCircle(
            color = eyeColor,
            radius = 2.2f,
            center = z.position + Offset(z.size * 0.65f, z.size * 0.22f)
        )
    }

    if (z.health < z.maxHealth || z.type == "Brute") {
        val barWidth = (z.size * 2.2f).coerceAtLeast(32f)
        val barHeight = if (z.type == "Brute") 6f else 4.5f
        val barTopLeft = Offset(z.position.x - barWidth / 2f, z.position.y - z.size - 12f)
        val hpRatio = (z.health.toFloat() / z.maxHealth).coerceIn(0f, 1f)

        drawRect(Color(0xFF1B1F24), topLeft = barTopLeft, size = Size(barWidth, barHeight))
        drawRect(
            color = Color(0xFF37474F),
            topLeft = barTopLeft,
            size = Size(barWidth, barHeight),
            style = Stroke(width = 1f)
        )
        drawRect(
            color = if (hpRatio > 0.5f) Color(0xFF4CAF50) else if (hpRatio > 0.25f) Color(0xFFFFB300) else Color(0xFFFF1744),
            topLeft = barTopLeft,
            size = Size(barWidth * hpRatio, barHeight)
        )

        if (z.type == "Brute") {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 18f
                isFakeBoldText = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                "ELITE BRUTE ${z.health}/${z.maxHealth}",
                barTopLeft.x - 10f,
                barTopLeft.y - 4f,
                paint
            )
        }
    }
}

// Draw The Abomination Boss with Massive Mutant Silhouette & Visual Effects
private fun DrawScope.drawBossCharacter(b: BossState) {
    val angleDeg = Math.toDegrees(b.facingAngle.toDouble()).toFloat()

    if (b.isDying) {
        val deathRatio = (b.deathTimer / b.deathDuration.toFloat()).coerceIn(0f, 1f)
        rotate(angleDeg + (1f - deathRatio) * 60f, b.position) {
            drawCircle(
                color = Color(0xFF4A148C).copy(alpha = deathRatio * 0.8f),
                radius = b.size * (0.8f + (1f - deathRatio) * 0.4f),
                center = b.position
            )
            drawCircle(
                color = Color(0xFF00E676).copy(alpha = deathRatio * 0.6f),
                radius = b.size * 0.5f * deathRatio,
                center = b.position
            )
        }
        return
    }

    rotate(angleDeg, b.position) {
        val isFlashing = b.hitFlash > 0
        val baseColor = if (isFlashing) Color.White else Color(0xFF2C0B3A)
        val mutantAccent = if (isFlashing) Color(0xFFFFFF8D) else Color(0xFF7B1FA2)
        val toxicVeins = Color(0xFF00E676)

        // 1. Heavy Mutant Footprints / Stomp Strides
        drawCircle(
            color = Color(0xFF1E1424),
            radius = b.size * 0.35f,
            center = b.position + Offset(-b.size * 0.5f, -b.size * 0.6f)
        )
        drawCircle(
            color = Color(0xFF1E1424),
            radius = b.size * 0.35f,
            center = b.position + Offset(-b.size * 0.5f, b.size * 0.6f)
        )

        // 2. Colossal Back Shell / Spines
        drawCircle(
            color = Color(0xFF190622),
            radius = b.size * 0.95f,
            center = b.position - Offset(b.size * 0.3f, 0f)
        )
        // Jagged mutated back spikes
        for (i in -2..2) {
            drawRect(
                color = Color(0xFF512DA8),
                topLeft = b.position + Offset(-b.size * 0.9f, i * 14f - 6f),
                size = Size(14f, 12f)
            )
        }

        // 3. Titanic Torso
        drawCircle(
            color = baseColor,
            radius = b.size,
            center = b.position
        )
        // Mutated chest carapace
        drawCircle(
            color = mutantAccent,
            radius = b.size * 0.68f,
            center = b.position
        )
        // Glowing bio-hazard vein fissures
        drawLine(
            color = toxicVeins,
            start = b.position - Offset(b.size * 0.4f, 0f),
            end = b.position + Offset(b.size * 0.3f, 0f),
            strokeWidth = 3.5f
        )
        drawLine(
            color = toxicVeins,
            start = b.position + Offset(-b.size * 0.2f, -b.size * 0.3f),
            end = b.position + Offset(b.size * 0.2f, b.size * 0.3f),
            strokeWidth = 3f
        )

        // 4. Asymmetrical Titanic Arms
        val slamLift = if (b.currentPattern == BossPattern.TELEGRAPH_SLAM) b.telegraphProgress * 18f else 0f
        // Left arm: Massive bone club fist
        drawCircle(
            color = mutantAccent,
            radius = b.size * 0.4f,
            center = b.position + Offset(b.size * 0.6f + slamLift, -b.size * 0.7f)
        )
        drawCircle(
            color = Color(0xFF212121),
            radius = b.size * 0.25f,
            center = b.position + Offset(b.size * 0.95f + slamLift, -b.size * 0.7f)
        )

        // Right arm: Mutated giant claw blade
        drawCircle(
            color = mutantAccent,
            radius = b.size * 0.35f,
            center = b.position + Offset(b.size * 0.6f + slamLift, b.size * 0.7f)
        )
        drawLine(
            color = Color(0xFF673AB7),
            start = b.position + Offset(b.size * 0.6f + slamLift, b.size * 0.7f),
            end = b.position + Offset(b.size * 1.25f + slamLift, b.size * 0.85f),
            strokeWidth = 9f
        )

        // 5. Mutated Head & Triple Glowing Eye Cluster
        drawCircle(
            color = baseColor,
            radius = b.size * 0.48f,
            center = b.position + Offset(b.size * 0.45f, 0f)
        )
        // 3 Glowing bio-luminescent toxic eyes
        drawCircle(
            color = toxicVeins,
            radius = 3.8f,
            center = b.position + Offset(b.size * 0.82f, -7f)
        )
        drawCircle(
            color = toxicVeins,
            radius = 3.8f,
            center = b.position + Offset(b.size * 0.82f, 7f)
        )
        drawCircle(
            color = Color(0xFFFF1744),
            radius = 4.2f,
            center = b.position + Offset(b.size * 0.92f, 0f)
        )
    }
}
