package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DatabaseProvider
import com.example.data.PlayerProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = DatabaseProvider.getDatabase(application).gameDao()

    val playerProfile: StateFlow<PlayerProfile?> = dao.getPlayerProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val buildings = dao.getBuildings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getBuildings(): kotlinx.coroutines.flow.Flow<List<com.example.data.BaseBuilding>> = dao.getBuildings()
    fun getSurvivors(): kotlinx.coroutines.flow.Flow<List<com.example.data.Survivor>> = dao.getSurvivors()

    fun upgradeBuilding(buildingId: Int) {
        viewModelScope.launch {
            val profile = dao.getPlayerProfile().first() ?: return@launch
            val buildings = dao.getBuildings().first()
            val building = buildings.find { it.id == buildingId } ?: return@launch
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
    fun completeMission(missionId: Int, finalKills: Long) {
        viewModelScope.launch {
            val profile = dao.getPlayerProfile().first() ?: return@launch
            val missions = dao.getMissions().first()
            val mission = missions.find { it.missionId == missionId } ?: return@launch
            
            if (!mission.isRewardClaimed) {
                profile.coins += 250
                profile.food += 100
                mission.isCompleted = true
                mission.isRewardClaimed = true
                mission.bestScore = maxOf(mission.bestScore, finalKills)
                
                dao.updatePlayerProfile(profile)
                dao.updateMission(mission)
            }
        }
    }
}
