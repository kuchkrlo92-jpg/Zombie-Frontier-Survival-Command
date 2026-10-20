package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfile(
    @PrimaryKey val id: Int = 1,
    var level: Int = 1,
    var coins: Long = 1000,
    var food: Long = 500,
    var metal: Long = 200,
    var energy: Int = 50,
    var experience: Long = 0
)

@Entity(tableName = "base_buildings")
data class BaseBuilding(
    @PrimaryKey val id: Int, // 1: Command, 2: Generator, 3: Barracks, 4: Workshop, 5: Medical
    val type: String,
    var level: Int = 1
)

@Entity(tableName = "survivors")
data class Survivor(
    @PrimaryKey val id: Int, // 1: Assault, 2: Heavy, 3: Medic, 4: Scout
    val survivorClass: String,
    var level: Int = 1,
    var health: Int = 100,
    var damage: Int = 10,
    var isRecruited: Boolean = false
)

@Entity(tableName = "mission_progress")
data class MissionProgress(
    @PrimaryKey val missionId: Int, // 1: Highway, 2: District, 3: Industrial
    var isUnlocked: Boolean = false,
    var isCompleted: Boolean = false,
    var isRewardClaimed: Boolean = false,
    var bestScore: Long = 0
)

@Entity(tableName = "weapon_progress")
data class WeaponProgress(
    @PrimaryKey val weaponId: Int, // 1: Assault Rifle, 2: Machine Gun, 3: Precision
    var level: Int = 1,
    var isUnlocked: Boolean = false,
    var isEquipped: Boolean = false
)
