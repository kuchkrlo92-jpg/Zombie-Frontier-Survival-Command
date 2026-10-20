package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DatabaseProvider {
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "zombie_frontier_db"
            ).addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    CoroutineScope(Dispatchers.IO).launch {
                        INSTANCE?.let { populateDatabase(it) }
                    }
                }
            }).build()
            INSTANCE = instance
            instance
        }
    }

    private suspend fun populateDatabase(db: AppDatabase) {
        val dao = db.gameDao()
        dao.updatePlayerProfile(PlayerProfile(id = 1))
        
        // Initial Buildings
        val buildings = listOf(
            BaseBuilding(1, "Command Center"),
            BaseBuilding(2, "Resource Generator"),
            BaseBuilding(3, "Barracks"),
            BaseBuilding(4, "Weapon Workshop"),
            BaseBuilding(5, "Medical Station")
        )
        buildings.forEach { dao.insertBuilding(it) }

        // Initial Survivors
        val survivors = listOf(
            Survivor(1, "Assault Soldier", isRecruited = true),
            Survivor(2, "Heavy Gunner"),
            Survivor(3, "Medic"),
            Survivor(4, "Scout")
        )
        survivors.forEach { dao.insertSurvivor(it) }
        
        // Initial Missions
        val missions = listOf(
            MissionProgress(1, isUnlocked = true),
            MissionProgress(2),
            MissionProgress(3)
        )
        missions.forEach { dao.insertMission(it) }
    }
}
