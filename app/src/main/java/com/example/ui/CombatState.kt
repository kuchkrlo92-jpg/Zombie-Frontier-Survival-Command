package com.example.ui

import androidx.compose.ui.geometry.Offset

data class PlayerState(
    var position: Offset = Offset(100f, 500f),
    var health: Int = 100,
    var damage: Int = 10
)

data class Zombie(
    val id: Int,
    var position: Offset,
    var health: Int,
    val type: String, // "Walker", "Runner", "Brute"
    var speed: Float,
    var damage: Int = 10,
    var size: Float = 15f
)

data class Projectile(
    val position: Offset,
    val velocity: Offset
)

data class Effect(
    val position: Offset,
    val type: String, // "Flash", "Impact", "Death"
    var ttl: Int = 10
)

data class CombatState(
    val player: PlayerState = PlayerState(),
    val zombies: List<Zombie> = emptyList(),
    val projectiles: List<Projectile> = emptyList(),
    val effects: List<Effect> = emptyList(),
    val score: Int = 0,
    val killCount: Int = 0,
    val isPaused: Boolean = false,
    val isGameOver: Boolean = false,
    val isVictory: Boolean = false,
    val joystickInput: Offset = Offset.Zero
)
