package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun getPlayerProfile(): Flow<PlayerProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePlayerProfile(profile: PlayerProfile)

    @Query("SELECT * FROM base_buildings")
    fun getBuildings(): Flow<List<BaseBuilding>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuilding(building: BaseBuilding)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurvivor(survivor: Survivor)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMission(mission: MissionProgress)

    @Update
    suspend fun updateBuilding(building: BaseBuilding)

    @Query("SELECT * FROM survivors")
    fun getSurvivors(): Flow<List<Survivor>>

    @Update
    suspend fun updateSurvivor(survivor: Survivor)

    @Query("SELECT * FROM mission_progress")
    fun getMissions(): Flow<List<MissionProgress>>

    @Update
    suspend fun updateMission(mission: MissionProgress)
}

@Database(
    entities = [PlayerProfile::class, BaseBuilding::class, Survivor::class, MissionProgress::class, WeaponProgress::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}
