package com.example.data

data class WeaponDef(
    val id: Int,
    val name: String,
    val codeName: String,
    val type: String,
    val description: String,
    val baseDamage: Int,
    val damagePerLevel: Int,
    val fireRateMs: Long,
    val range: Float,
    val unlockCost: Long,
    val pellets: Int = 1,
    val spreadAngle: Float = 0f,
    val projectileSpeed: Float = 14f,
    val projectileSize: Float = 5f,
    val colorHex: Long = 0xFFFFD700
) {
    fun calculateDamage(level: Int): Int = baseDamage + (level - 1) * damagePerLevel
    fun calculateUpgradeCost(level: Int): Long = level * 250L
}

object WeaponCatalog {
    val WEAPONS: List<WeaponDef> = listOf(
        WeaponDef(
            id = 1,
            name = "M4A1 Sentinel",
            codeName = "ASSAULT RIFLE",
            type = "Assault Rifle",
            description = "Reliable standard-issue assault rifle with balanced fire rate, damage, and effective range.",
            baseDamage = 25,
            damagePerLevel = 8,
            fireRateMs = 220L,
            range = 650f,
            unlockCost = 0L, // Starter
            pellets = 1,
            spreadAngle = 0f,
            projectileSpeed = 15f,
            projectileSize = 5f,
            colorHex = 0xFFFFEB3B
        ),
        WeaponDef(
            id = 2,
            name = "Vector Venom",
            codeName = "SMG",
            type = "Submachine Gun",
            description = "High-RPM tactical submachine gun that shreds through incoming swarms at close to medium range.",
            baseDamage = 15,
            damagePerLevel = 5,
            fireRateMs = 110L, // Rapid fire
            range = 480f,
            unlockCost = 600L,
            pellets = 1,
            spreadAngle = 0.08f,
            projectileSpeed = 16f,
            projectileSize = 4f,
            colorHex = 0xFF64FFDA
        ),
        WeaponDef(
            id = 3,
            name = "Breacher 12G",
            codeName = "SHOTGUN",
            type = "Combat Shotgun",
            description = "Pump-action devastation firing 4 buckshot pellets per shot. Deadly against approaching Brutes.",
            baseDamage = 18, // 18 x 4 = 72 total per blast
            damagePerLevel = 6,
            fireRateMs = 520L,
            range = 380f,
            unlockCost = 1200L,
            pellets = 4,
            spreadAngle = 0.22f,
            projectileSpeed = 13f,
            projectileSize = 6f,
            colorHex = 0xFFFF7043
        ),
        WeaponDef(
            id = 4,
            name = "Barrett Reaper",
            codeName = "SNIPER RIFLE",
            type = "Heavy Sniper Rifle",
            description = "Anti-materiel precision rifle delivering devastating single-shot penetrations at long distance.",
            baseDamage = 110,
            damagePerLevel = 35,
            fireRateMs = 900L, // Slow but massive damage
            range = 1000f,
            unlockCost = 2000L,
            pellets = 1,
            spreadAngle = 0f,
            projectileSpeed = 22f,
            projectileSize = 7f,
            colorHex = 0xFFE040FB
        )
    )

    fun getById(id: Int): WeaponDef = WEAPONS.find { it.id == id } ?: WEAPONS.first()
}
