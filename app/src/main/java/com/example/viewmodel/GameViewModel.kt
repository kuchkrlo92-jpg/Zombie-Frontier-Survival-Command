package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = DatabaseProvider.getDatabase(application).gameDao()

    val playerProfile: StateFlow<PlayerProfile?> = dao.getPlayerProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val buildings: StateFlow<List<BaseBuilding>> = dao.getBuildings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weaponProgress: StateFlow<List<WeaponProgress>> = dao.getWeaponProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val missions: StateFlow<List<MissionProgress>> = dao.getMissions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val equippedWeapon: StateFlow<Pair<WeaponDef, Int>> = dao.getWeaponProgress()
        .map { list ->
            val equipped = list.find { it.isEquipped } ?: list.find { it.isUnlocked } ?: WeaponProgress(1, 1, true, true)
            val def = WeaponCatalog.getById(equipped.weaponId)
            Pair(def, equipped.level)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            Pair(WeaponCatalog.getById(1), 1)
        )

    init {
        // Guarantee initial records exist even if DB was created prior to seeding
        viewModelScope.launch {
            ensureInitialRecords()
        }
    }

    private suspend fun ensureInitialRecords() {
        val existingProfile = dao.getPlayerProfile().first()
        if (existingProfile == null) {
            dao.updatePlayerProfile(PlayerProfile(id = 1))
        }

        val existingWeapons = dao.getWeaponProgress().first()
        for (def in WeaponCatalog.WEAPONS) {
            if (existingWeapons.none { it.weaponId == def.id }) {
                dao.insertWeaponProgress(
                    WeaponProgress(
                        weaponId = def.id,
                        level = 1,
                        isUnlocked = (def.id == 1),
                        isEquipped = (def.id == 1)
                    )
                )
            }
        }

        val existingMissions = dao.getMissions().first()
        val defaultMissions = listOf(
            MissionProgress(1, isUnlocked = true),
            MissionProgress(2, isUnlocked = false),
            MissionProgress(3, isUnlocked = false),
            MissionProgress(4, isUnlocked = false), // Boss Raid: The Abomination
            MissionProgress(100, isUnlocked = true)  // Survival Mode: Wave Defense
        )
        for (m in defaultMissions) {
            if (existingMissions.none { it.missionId == m.missionId }) {
                dao.insertMission(m)
            }
        }

        // Guarantee that completed campaign missions unlock the next chapter
        val updatedMissions = dao.getMissions().first()
        val m1 = updatedMissions.find { it.missionId == 1 }
        val m2 = updatedMissions.find { it.missionId == 2 }
        val m3 = updatedMissions.find { it.missionId == 3 }
        val m4 = updatedMissions.find { it.missionId == 4 }
        if (m1?.isCompleted == true && m2 != null && !m2.isUnlocked) {
            m2.isUnlocked = true
            dao.updateMission(m2)
        }
        if (m2?.isCompleted == true && m3 != null && !m3.isUnlocked) {
            m3.isUnlocked = true
            dao.updateMission(m3)
        }
        if (m3?.isCompleted == true && m4 != null && !m4.isUnlocked) {
            m4.isUnlocked = true
            dao.updateMission(m4)
        }
    }

    fun getBuildings(): kotlinx.coroutines.flow.Flow<List<BaseBuilding>> = dao.getBuildings()
    fun getSurvivors(): kotlinx.coroutines.flow.Flow<List<Survivor>> = dao.getSurvivors()
    fun getMissions(): kotlinx.coroutines.flow.Flow<List<MissionProgress>> = dao.getMissions()
    fun getWeaponProgress(): kotlinx.coroutines.flow.Flow<List<WeaponProgress>> = dao.getWeaponProgress()

    fun upgradeBuilding(buildingId: Int) {
        viewModelScope.launch {
            val profile = dao.getPlayerProfile().first() ?: return@launch
            val bList = dao.getBuildings().first()
            val building = bList.find { it.id == buildingId } ?: return@launch
            val costCoins = building.level * 100L
            val costMetal = building.level * 50L
            
            if (profile.coins >= costCoins && profile.metal >= costMetal) {
                profile.coins -= costCoins
                profile.metal -= costMetal
                building.level += 1
                dao.updatePlayerProfile(profile)
                dao.updateBuilding(building)
            }
        }
    }

    fun recruitSurvivor(survivorId: Int) {
        viewModelScope.launch {
            val profile = dao.getPlayerProfile().first() ?: return@launch
            val survivors = dao.getSurvivors().first()
            val survivor = survivors.find { it.id == survivorId } ?: return@launch
            val costCoins = 500L
            val costFood = 200L

            if (!survivor.isRecruited && profile.coins >= costCoins && profile.food >= costFood) {
                profile.coins -= costCoins
                profile.food -= costFood
                survivor.isRecruited = true
                dao.updatePlayerProfile(profile)
                dao.updateSurvivor(survivor)
            }
        }
    }

    fun upgradeSurvivor(survivorId: Int) {
        viewModelScope.launch {
            val profile = dao.getPlayerProfile().first() ?: return@launch
            val survivors = dao.getSurvivors().first()
            val survivor = survivors.find { it.id == survivorId } ?: return@launch
            val costCoins = survivor.level * 200L

            if (survivor.isRecruited && profile.coins >= costCoins) {
                profile.coins -= costCoins
                survivor.level += 1
                survivor.damage += 5
                survivor.health += 20
                dao.updatePlayerProfile(profile)
                dao.updateSurvivor(survivor)
            }
        }
    }

    fun unlockWeapon(weaponId: Int) {
        viewModelScope.launch {
            val profile = dao.getPlayerProfile().first() ?: return@launch
            val weapons = dao.getWeaponProgress().first()
            val weapon = weapons.find { it.weaponId == weaponId } ?: return@launch
            val def = WeaponCatalog.getById(weaponId)
            val cost = def.unlockCost
            
            if (!weapon.isUnlocked && profile.coins >= cost) {
                profile.coins -= cost
                weapon.isUnlocked = true
                dao.updatePlayerProfile(profile)
                dao.updateWeaponProgress(weapon)
            }
        }
    }

    fun upgradeWeapon(weaponId: Int) {
        viewModelScope.launch {
            val profile = dao.getPlayerProfile().first() ?: return@launch
            val weapons = dao.getWeaponProgress().first()
            val weapon = weapons.find { it.weaponId == weaponId } ?: return@launch
            val def = WeaponCatalog.getById(weaponId)
            val cost = def.calculateUpgradeCost(weapon.level)
            
            if (weapon.isUnlocked && profile.coins >= cost) {
                profile.coins -= cost
                weapon.level += 1
                dao.updatePlayerProfile(profile)
                dao.updateWeaponProgress(weapon)
            }
        }
    }

    fun equipWeapon(weaponId: Int) {
        viewModelScope.launch {
            val weapons = dao.getWeaponProgress().first()
            val target = weapons.find { it.weaponId == weaponId } ?: return@launch
            if (!target.isUnlocked) return@launch

            weapons.forEach {
                it.isEquipped = (it.weaponId == weaponId)
                dao.updateWeaponProgress(it)
            }
        }
    }

    fun completeMission(missionId: Int, finalKills: Long) {
        viewModelScope.launch {
            val profile = dao.getPlayerProfile().first() ?: return@launch
            val missionsList = dao.getMissions().first()
            val currentMission = missionsList.find { it.missionId == missionId } ?: return@launch
            
            // Rewards based on mission level
            val (coinReward, foodReward) = when (missionId) {
                1 -> Pair(250L, 100L)
                2 -> Pair(500L, 200L)
                3 -> Pair(1000L, 400L)
                4 -> Pair(1500L, 600L) // Boss Raid: The Abomination
                else -> Pair(250L, 100L)
            }

            if (!currentMission.isRewardClaimed) {
                profile.coins += coinReward
                profile.food += foodReward
                currentMission.isRewardClaimed = true
            }

            currentMission.isCompleted = true
            currentMission.bestScore = maxOf(currentMission.bestScore, finalKills)

            dao.updatePlayerProfile(profile)
            dao.updateMission(currentMission)

            // Unlock the next campaign mission if available (1 -> 2, 2 -> 3, 3 -> 4)
            if (missionId in 1..3) {
                val nextMissionId = missionId + 1
                val nextMission = missionsList.find { it.missionId == nextMissionId }
                if (nextMission != null && !nextMission.isUnlocked) {
                    nextMission.isUnlocked = true
                    dao.updateMission(nextMission)
                }
            }
        }
    }

    /**
     * Idempotent Survival Mode reward claim.
     * Rewards are granted strictly for newly cleared wave milestones beyond the user's bestScore.
     * Retrying a wave already cleared grants 0 coins, ensuring no duplicate claims.
     */
    fun claimSurvivalRewards(
        wavesCleared: Int,
        finalKills: Long,
        onAwarded: (coins: Long, food: Long) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val profile = dao.getPlayerProfile().first() ?: return@launch
            val missionsList = dao.getMissions().first()
            var survivalRecord = missionsList.find { it.missionId == 100 }
            if (survivalRecord == null) {
                survivalRecord = MissionProgress(100, isUnlocked = true)
                dao.insertMission(survivalRecord)
            }

            val previousBestWave = survivalRecord.bestScore.toInt()
            var earnedCoins = 0L
            var earnedFood = 0L

            if (wavesCleared > previousBestWave) {
                for (w in (previousBestWave + 1)..wavesCleared) {
                    val (wCoins, wFood) = when (w) {
                        1 -> Pair(150L, 50L)
                        2 -> Pair(300L, 100L)
                        3 -> Pair(500L, 200L)
                        4 -> Pair(750L, 300L)
                        5 -> Pair(1200L, 500L)
                        else -> Pair(200L, 50L)
                    }
                    earnedCoins += wCoins
                    earnedFood += wFood
                }
                profile.coins += earnedCoins
                profile.food += earnedFood
                survivalRecord.bestScore = wavesCleared.toLong()
            }

            if (wavesCleared >= 5) {
                survivalRecord.isCompleted = true
                survivalRecord.isRewardClaimed = true
            }

            dao.updatePlayerProfile(profile)
            dao.updateMission(survivalRecord)
            onAwarded(earnedCoins, earnedFood)
        }
    }

    fun addSupplies(coins: Long, metal: Long, food: Long = 0L) {
        viewModelScope.launch {
            val profile = dao.getPlayerProfile().first() ?: return@launch
            profile.coins += coins
            profile.metal += metal
            profile.food += food
            dao.updatePlayerProfile(profile)
        }
    }
}
