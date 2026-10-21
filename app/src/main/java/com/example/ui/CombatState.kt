package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.data.WeaponCatalog
import com.example.data.WeaponDef

data class PlayerState(
    var position: Offset = Offset(140f, 480f),
    var health: Int = 100,
    var maxHealth: Int = 100,
    var lastFireTimeMs: Long = 0L,
    var facingAngle: Float = 0f,
    var walkAnimPhase: Float = 0f,
    var ammoCurrent: Int = 30,
    var ammoMax: Int = 30,
    var isReloading: Boolean = false,
    var reloadTimerMs: Long = 0L,
    var reloadDurationMs: Long = 1200L
)

enum class BossPattern {
    IDLE,
    TELEGRAPH_SLAM,
    EXECUTE_SLAM,
    TELEGRAPH_CHARGE,
    EXECUTE_CHARGE,
    TELEGRAPH_SUMMON,
    EXECUTE_SUMMON
}

data class BossState(
    val name: String = "THE ABOMINATION",
    var position: Offset = Offset(880f, 480f),
    var health: Int = 1200,
    var maxHealth: Int = 1200,
    var size: Float = 44f,
    var speed: Float = 1.35f,
    var facingAngle: Float = 3.14159f,
    var hitFlash: Int = 0,
    var currentPattern: BossPattern = BossPattern.IDLE,
    var patternTimer: Int = 0,
    var attackCooldown: Int = 40,
    var slamCooldown: Int = 90,
    var chargeCooldown: Int = 150,
    var summonCooldown: Int = 220,
    var chargeDirection: Offset = Offset(-1f, 0f),
    var hasDamagedInCharge: Boolean = false,
    var telegraphProgress: Float = 0f,
    var isDying: Boolean = false,
    var deathTimer: Int = 0,
    var deathDuration: Int = 50
)

data class Zombie(
    val id: Int,
    var position: Offset,
    var health: Int,
    var maxHealth: Int,
    val type: String, // "Walker", "Runner", "Brute"
    var speed: Float,
    var damage: Int = 10,
    var size: Float = 15f,
    var attackCooldown: Int = 0,
    var animPhase: Float = 0f,
    var facingAngle: Float = 3.14159f,
    var hitFlash: Int = 0,
    var knockback: Offset = Offset.Zero,
    var isAttacking: Boolean = false,
    var attackProgress: Float = 0f,
    var isLunging: Boolean = false,
    var lungeTimer: Int = 0,
    var isDying: Boolean = false,
    var deathTimer: Int = 0,
    var deathDuration: Int = 16
)

data class Projectile(
    val position: Offset,
    val velocity: Offset,
    val damage: Int,
    val rangeRemaining: Float,
    val size: Float = 5f,
    val color: Color = Color.Yellow,
    val isCritical: Boolean = false
)

data class Effect(
    val position: Offset,
    val type: String, // "Flash", "Impact", "Death", "PlayerHit", "Spark", "BloodSpray", "BossToxic"
    var ttl: Int = 10,
    val color: Color = Color.White,
    val size: Float = 10f,
    val velocity: Offset = Offset.Zero
)

data class FloatingDamage(
    val text: String,
    var position: Offset,
    val color: Color = Color(0xFFFFD54F),
    var ttl: Int = 25,
    val isCritical: Boolean = false
)

data class Obstacle(
    val topLeft: Offset,
    val width: Float,
    val height: Float,
    val label: String
)

data class BloodDecal(
    val position: Offset,
    val radius: Float,
    val color: Color = Color(0x887F0000),
    val alpha: Float = 0.55f,
    val seed: Int = 0
)

data class ShellCasing(
    val position: Offset,
    val velocity: Offset,
    val rotation: Float = 0f,
    val ttl: Int = 30
)

data class HitMarker(
    val position: Offset,
    val ttl: Int = 5
)

data class Shockwave(
    val position: Offset,
    val radius: Float,
    val maxRadius: Float = 60f,
    val ttl: Int = 12,
    val color: Color = Color(0xAAFF5722)
)

data class ComboBanner(
    val title: String,
    val subtitle: String,
    val color: Color,
    val ttl: Int = 50
)

data class CombatState(
    val missionId: Int = 1,
    val targetKills: Int = 15,
    val missionTitle: String = "Abandoned Highway",
    val player: PlayerState = PlayerState(),
    val zombies: List<Zombie> = emptyList(),
    val projectiles: List<Projectile> = emptyList(),
    val effects: List<Effect> = emptyList(),
    val floatingDamages: List<FloatingDamage> = emptyList(),
    val obstacles: List<Obstacle> = emptyList(),
    val bloodDecals: List<BloodDecal> = emptyList(),
    val shellCasings: List<ShellCasing> = emptyList(),
    val hitMarkers: List<HitMarker> = emptyList(),
    val shockwaves: List<Shockwave> = emptyList(),
    val comboBanner: ComboBanner? = null,
    val comboStreak: Int = 0,
    val lastKillTimeMs: Long = 0L,
    val screenShake: Offset = Offset.Zero,
    val screenShakeIntensity: Float = 0f,
    val isFiringHeld: Boolean = false,
    val weaponDef: WeaponDef = WeaponCatalog.getById(1),
    val weaponLevel: Int = 1,
    val killCount: Int = 0,
    val isPaused: Boolean = false,
    val isGameOver: Boolean = false,
    val isVictory: Boolean = false,
    val joystickInput: Offset = Offset.Zero,
    val extractionActive: Boolean = false,
    val extractionBounds: Offset = Offset(880f, 200f),

    // Wave Survival System
    val isSurvivalMode: Boolean = false,
    val currentWave: Int = 1,
    val maxWaves: Int = 5,
    val waveEnemiesTotal: Int = 10,
    val waveEnemiesSpawned: Int = 0,
    val isWaveBreak: Boolean = false,
    val waveBreakTimer: Int = 0,
    val waveAnnouncement: String? = null,
    val waveAnnouncementTtl: Int = 0,
    val wavesClearedInRun: Int = 0,
    val totalShotsFired: Int = 0,
    val totalShotsHit: Int = 0,
    val survivalCoinsEarned: Long = 0L,
    val survivalFoodEarned: Long = 0L,

    // Boss Encounter
    val boss: BossState? = null
)
