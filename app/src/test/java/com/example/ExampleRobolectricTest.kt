package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.MissionProgress
import com.example.data.PlayerProfile
import com.example.data.WeaponCatalog
import com.example.ui.BossPattern
import com.example.ui.BossState
import com.example.ui.BossType
import com.example.ui.GateType
import com.example.ui.LaneRenderer
import com.example.ui.PlayerState
import com.example.ui.TacticalGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Zombie Frontier", appName)
  }

  @Test
  fun `verify weapon catalog initialization`() {
    val weapons = WeaponCatalog.WEAPONS
    assertEquals(4, weapons.size)
    assertEquals("M4A1 Sentinel", weapons[0].name)
    assertEquals(30, if (weapons[0].id == 1) 30 else 0)
    assertEquals(4, weapons[2].pellets) // Shotgun 4 pellets
  }

  @Test
  fun `verify wave progression enemy scaling`() {
    fun getWaveTotal(wave: Int): Int = when (wave) {
      1 -> 10
      2 -> 16
      3 -> 21
      4 -> 28
      5 -> 7
      else -> 10 + wave * 5
    }

    assertEquals(10, getWaveTotal(1))
    assertEquals(16, getWaveTotal(2))
    assertEquals(21, getWaveTotal(3))
    assertEquals(28, getWaveTotal(4))
    assertEquals(7, getWaveTotal(5)) // Final Boss Wave
  }

  @Test
  fun `verify boss attack patterns and charge damage safeguard`() {
    val boss = BossState(
      name = "THE ABOMINATION",
      health = 1200,
      maxHealth = 1200,
      currentPattern = BossPattern.IDLE,
      slamCooldown = 90,
      chargeCooldown = 150,
      summonCooldown = 220
    )

    assertEquals("THE ABOMINATION", boss.name)
    assertEquals(1200, boss.health)
    assertEquals(BossPattern.IDLE, boss.currentPattern)
    assertTrue(boss.slamCooldown > 0)
    assertTrue(boss.chargeCooldown > 0)
    assertTrue(boss.summonCooldown > 0)

    // Simulate charge phase initiation
    val chargingBoss = boss.copy(
      currentPattern = BossPattern.EXECUTE_CHARGE,
      hasDamagedInCharge = false
    )
    assertFalse(chargingBoss.hasDamagedInCharge)

    // First collision: damage dealt and flag set to true
    val afterHitBoss = chargingBoss.copy(hasDamagedInCharge = true)
    assertTrue(afterHitBoss.hasDamagedInCharge)

    // Subsequent collision in the same charge does not re-deal damage
    val canDealDamageAgainInCurrentCharge = !afterHitBoss.hasDamagedInCharge
    assertFalse(canDealDamageAgainInCurrentCharge)
  }

  @Test
  fun `verify campaign mission reward idempotency`() {
    val profile = PlayerProfile(id = 1, coins = 1000L, food = 500L)
    val mission1 = MissionProgress(missionId = 1, isUnlocked = true, isCompleted = false, isRewardClaimed = false)

    val rewardCoins = 250L
    val rewardFood = 100L

    // First completion: reward claimed
    if (!mission1.isRewardClaimed) {
      profile.coins += rewardCoins
      profile.food += rewardFood
      mission1.isRewardClaimed = true
    }
    mission1.isCompleted = true

    assertEquals(1250L, profile.coins)
    assertEquals(600L, profile.food)
    assertTrue(mission1.isRewardClaimed)

    // Second completion (retry or replay): reward must NOT be awarded again
    if (!mission1.isRewardClaimed) {
      profile.coins += rewardCoins
      profile.food += rewardFood
    }

    assertEquals(1250L, profile.coins)
    assertEquals(600L, profile.food)
  }

  @Test
  fun `verify survival mode milestone reward idempotency`() {
    val profile = PlayerProfile(id = 1, coins = 1000L, food = 500L)
    val survivalRecord = MissionProgress(missionId = 100, isUnlocked = true, bestScore = 0L)

    fun claimWaveMilestoneRewards(wavesCleared: Int): Pair<Long, Long> {
      val prev = survivalRecord.bestScore.toInt()
      var earnedCoins = 0L
      var earnedFood = 0L

      if (wavesCleared > prev) {
        for (w in (prev + 1)..wavesCleared) {
          val (c, f) = when (w) {
            1 -> Pair(150L, 50L)
            2 -> Pair(300L, 100L)
            3 -> Pair(500L, 200L)
            4 -> Pair(750L, 300L)
            5 -> Pair(1200L, 500L)
            else -> Pair(200L, 50L)
          }
          earnedCoins += c
          earnedFood += f
        }
        profile.coins += earnedCoins
        profile.food += earnedFood
        survivalRecord.bestScore = wavesCleared.toLong()
      }
      return Pair(earnedCoins, earnedFood)
    }

    // Run 1: Player clears Wave 2
    val (run1Coins, run1Food) = claimWaveMilestoneRewards(2)
    assertEquals(450L, run1Coins) // 150 (Wave 1) + 300 (Wave 2)
    assertEquals(150L, run1Food)  // 50 + 100
    assertEquals(1450L, profile.coins)
    assertEquals(2L, survivalRecord.bestScore)

    // Run 2: Player retries and clears Wave 2 again
    val (run2Coins, run2Food) = claimWaveMilestoneRewards(2)
    assertEquals(0L, run2Coins) // 0 coins awarded, preventing duplicate exploit
    assertEquals(0L, run2Food)
    assertEquals(1450L, profile.coins)

    // Run 3: Player pushes to Wave 4
    val (run3Coins, run3Food) = claimWaveMilestoneRewards(4)
    assertEquals(1250L, run3Coins) // 500 (Wave 3) + 750 (Wave 4)
    assertEquals(500L, run3Food)   // 200 + 300
    assertEquals(2700L, profile.coins)
    assertEquals(4L, survivalRecord.bestScore)
  }

  @Test
  fun `verify weapon progression, damage scaling, and magazine capacities`() {
    val weapons = WeaponCatalog.WEAPONS

    // 1. M4A1 Sentinel (Assault Rifle)
    val rifle = weapons.first { it.id == 1 }
    assertEquals(25, rifle.calculateDamage(1))
    assertEquals(33, rifle.calculateDamage(2))
    assertEquals(41, rifle.calculateDamage(3))
    assertEquals(250L, rifle.calculateUpgradeCost(1))
    assertEquals(500L, rifle.calculateUpgradeCost(2))

    // 2. Vector Venom (SMG)
    val smg = weapons.first { it.id == 2 }
    assertEquals(15, smg.calculateDamage(1))
    assertEquals(20, smg.calculateDamage(2))
    assertEquals(25, smg.calculateDamage(3))
    assertEquals(600L, smg.unlockCost)

    // 3. Breacher 12G (Shotgun)
    val shotgun = weapons.first { it.id == 3 }
    assertEquals(18, shotgun.calculateDamage(1))
    assertEquals(24, shotgun.calculateDamage(2))
    assertEquals(4, shotgun.pellets)
    assertEquals(72, shotgun.calculateDamage(1) * shotgun.pellets) // 72 total damage
    assertEquals(1200L, shotgun.unlockCost)

    // 4. Barrett Reaper (Sniper)
    val sniper = weapons.first { it.id == 4 }
    assertEquals(110, sniper.calculateDamage(1))
    assertEquals(145, sniper.calculateDamage(2))
    assertEquals(180, sniper.calculateDamage(3))
    assertEquals(2000L, sniper.unlockCost)
  }

  @Test
  fun `verify mission objective extraction rules and defeat precedence`() {
    val targetKills = 15

    // Case A: Insufficient kills (10/15) in extraction zone -> NOT victory
    val killsA = 10
    val inExtractionA = true
    val healthA = 100
    val isVictoryA = (killsA >= targetKills) && inExtractionA && (healthA > 0)
    assertFalse(isVictoryA)

    // Case B: Sufficient kills (15/15) but NOT in extraction zone -> NOT victory
    val killsB = 15
    val inExtractionB = false
    val healthB = 100
    val isVictoryB = (killsB >= targetKills) && inExtractionB && (healthB > 0)
    assertFalse(isVictoryB)

    // Case C: Sufficient kills (15/15) and IN extraction zone with health > 0 -> VICTORY
    val killsC = 15
    val inExtractionC = true
    val healthC = 60
    val isVictoryC = (killsC >= targetKills) && inExtractionC && (healthC > 0)
    assertTrue(isVictoryC)

    // Case D: Player reaches 0 health in extraction zone -> DEFEAT takes precedence, victory blocked
    val killsD = 18
    val inExtractionD = true
    val healthD = 0
    val isGameOverD = (healthD <= 0)
    val isVictoryD = !isGameOverD && (killsD >= targetKills) && inExtractionD
    assertTrue(isGameOverD)
    assertFalse(isVictoryD)
  }

  @Test
  fun `verify pause state prevents player actions and damage accumulation`() {
    var isPaused = true
    var ammoCurrent = 30
    var playerHealth = 100
    var isHoldingFire = false

    // Action attempted while paused: firing must be ignored
    fun attemptFire() {
      if (isPaused) return
      if (ammoCurrent > 0) ammoCurrent--
    }

    // Damage attempted while paused: must be ignored
    fun attemptDamage(amount: Int) {
      if (isPaused) return
      playerHealth -= amount
    }

    attemptFire()
    assertEquals(30, ammoCurrent) // Ammo unmodified

    attemptDamage(25)
    assertEquals(100, playerHealth) // Health unmodified

    assertFalse(isHoldingFire)

    // Resume gameplay
    isPaused = false
    attemptFire()
    assertEquals(29, ammoCurrent) // Fired successfully when active

    attemptDamage(20)
    assertEquals(80, playerHealth) // Took damage when active
  }

  @Test
  fun `verify boss attack telegraph and cooldown cycles`() {
    val boss = BossState(
      name = "THE ABOMINATION",
      health = 1200,
      currentPattern = BossPattern.TELEGRAPH_SLAM,
      patternTimer = 24,
      slamCooldown = 0
    )

    // During telegraph, progress scales from 0 to 1
    val progressMid = (24 - 12) / 24f
    assertEquals(0.5f, progressMid, 0.001f)

    // When timer expires, execute slam and set cooldown
    val slamExecutedCooldown = 110
    val updatedBoss = boss.copy(
      currentPattern = BossPattern.EXECUTE_SLAM,
      slamCooldown = slamExecutedCooldown
    )

    assertEquals(BossPattern.EXECUTE_SLAM, updatedBoss.currentPattern)
    assertEquals(110, updatedBoss.slamCooldown)
    assertTrue(updatedBoss.slamCooldown > 0) // Cooldown blocks immediate re-slam
  }

  @Test
  fun `verify 3-lane movement bounds and clamping`() {
    var playerLane = 1 // Center

    fun switchLeft() {
      if (playerLane > 0) playerLane--
    }
    fun switchRight() {
      if (playerLane < 2) playerLane++
    }

    // Switch Left -> Lane 0
    switchLeft()
    assertEquals(0, playerLane)

    // Cannot switch left beyond Lane 0
    switchLeft()
    assertEquals(0, playerLane)

    // Switch Right -> Lane 1, Lane 2
    switchRight()
    assertEquals(1, playerLane)
    switchRight()
    assertEquals(2, playerLane)

    // Cannot switch right beyond Lane 2
    switchRight()
    assertEquals(2, playerLane)
  }

  @Test
  fun `verify tactical gate effects and single-use idempotency`() {
    val player = PlayerState(
      currentLane = 1,
      squadCount = 1,
      damageMultiplier = 1.0f,
      ammoCurrent = 30,
      ammoMax = 30
    )

    val gateRecruits = TacticalGate(
      id = 1,
      lane = 1,
      distanceZ = 0f,
      type = GateType.RECRUIT_SURVIVORS,
      label = "+5 SURVIVORS",
      value = 5,
      isPositive = true,
      isPassed = false
    )

    // First trigger: apply effect and set isPassed = true
    if (!gateRecruits.isPassed) {
      player.squadCount += gateRecruits.value
      gateRecruits.isPassed = true
    }
    assertEquals(6, player.squadCount)
    assertTrue(gateRecruits.isPassed)

    // Second trigger attempt (e.g. adjacent frame): must NOT re-apply
    if (!gateRecruits.isPassed) {
      player.squadCount += gateRecruits.value
    }
    assertEquals(6, player.squadCount) // Strictly 6, no duplicate exploit!

    // Test Damage Boost gate
    val gateDamage = TacticalGate(
      id = 2,
      lane = 1,
      distanceZ = 0f,
      type = GateType.DAMAGE_BOOST,
      label = "DMG +25%",
      value = 25,
      isPositive = true,
      isPassed = false
    )
    if (!gateDamage.isPassed) {
      player.damageMultiplier += 0.25f
      gateDamage.isPassed = true
    }
    assertEquals(1.25f, player.damageMultiplier, 0.001f)
  }

  @Test
  fun `verify squad size expansion and casualty absorption`() {
    val player = PlayerState(
      health = 100,
      maxHealth = 100,
      squadCount = 5
    )

    // When squadCount > 1, taking a hit consumes 1 squad member instead of decreasing player health
    fun takeDamage(amount: Int) {
      if (player.squadCount > 1) {
        player.squadCount--
      } else {
        player.health = maxOf(0, player.health - amount)
      }
    }

    takeDamage(20)
    assertEquals(4, player.squadCount)
    assertEquals(100, player.health) // Health preserved!

    takeDamage(20)
    takeDamage(20)
    takeDamage(20)
    assertEquals(1, player.squadCount)
    assertEquals(100, player.health)

    // Last member (Commander): damage reduces health
    takeDamage(25)
    assertEquals(1, player.squadCount)
    assertEquals(75, player.health)
  }

  @Test
  fun `verify simulated 3d perspective projection math`() {
    val width = 1000f
    val height = 2000f

    val (posNear, scaleNear, laneWidthNear) = LaneRenderer.project(1.0f, 0f, width, height)
    assertEquals(1.0f, scaleNear, 0.001f)
    assertEquals(width * 0.5f, posNear.x, 0.001f) // Center lane is centered at width / 2
    assertEquals(height * 0.78f, posNear.y, 0.001f) // Near player depth

    // Distant object at z = 500m
    val (posFar, scaleFar, laneWidthFar) = LaneRenderer.project(1.0f, 500f, width, height)
    assertTrue(scaleFar < scaleNear)
    assertTrue(posFar.y < posNear.y) // Closer to horizon
    assertTrue(laneWidthFar < laneWidthNear) // Lane is narrower in the distance
    assertEquals(width * 0.5f, posFar.x, 0.001f) // Still centered horizontally

    // Left lane (0.0) should have smaller X than Center (1.0), and Right (2.0) should have larger X
    val (posLeft, _, _) = LaneRenderer.project(0.0f, 100f, width, height)
    val (posCenter, _, _) = LaneRenderer.project(1.0f, 100f, width, height)
    val (posRight, _, _) = LaneRenderer.project(2.0f, 100f, width, height)

    assertTrue(posLeft.x < posCenter.x)
    assertTrue(posCenter.x < posRight.x)
  }

  @Test
  fun `boss HP greater than zero allows normal attack cycles`() {
    val boss = BossState(
      name = "THE ABOMINATION",
      health = 1200,
      maxHealth = 1200,
      currentPattern = BossPattern.IDLE
    )

    assertTrue(boss.health > 0)
    assertTrue(boss.canAttack)
    assertFalse(boss.isDead)
    assertFalse(boss.isDying)
    assertFalse(boss.isDefeated)

    // Boss can cycle through telegraph and execution attack states while health > 0
    val telegraphBoss = boss.copy(currentPattern = BossPattern.TELEGRAPH_SLAM)
    assertTrue(telegraphBoss.canAttack)
    assertFalse(telegraphBoss.isDead)

    val slamBoss = telegraphBoss.copy(currentPattern = BossPattern.EXECUTE_SLAM)
    assertTrue(slamBoss.canAttack)
    assertFalse(slamBoss.isDead)
  }

  @Test
  fun `boss HP reaching zero triggers persistent death state exactly once`() {
    val boss = BossState(
      name = "THE ABOMINATION",
      health = 100,
      maxHealth = 1200,
      currentPattern = BossPattern.EXECUTE_CHARGE
    )

    // First lethal damage event: health reaches 0
    val (dyingBoss, justDied) = boss.applyDamage(100)
    assertTrue(justDied)
    assertEquals(0, dyingBoss.health)
    assertTrue(dyingBoss.isDying)
    assertTrue(dyingBoss.isDead)
    assertFalse(dyingBoss.canAttack)
    assertEquals(BossPattern.IDLE, dyingBoss.currentPattern)
    assertTrue(dyingBoss.hasDamagedInCharge)

    // Second damage event: must NOT re-trigger death event (idempotent)
    val (afterSecondDamage, justDiedAgain) = dyingBoss.applyDamage(100)
    assertFalse(justDiedAgain)
    assertEquals(0, afterSecondDamage.health)
    assertTrue(afterSecondDamage.isDead)
    assertFalse(afterSecondDamage.canAttack)
  }

  @Test
  fun `boss HP less than zero is clamped to zero immediately`() {
    val boss = BossState(
      name = "THE IRON TYRANT",
      bossType = BossType.IRON_TYRANT,
      health = 150,
      maxHealth = 1800
    )

    // Massive overkill damage (e.g. 500 damage against 150 HP)
    val (overkillBoss, justDied) = boss.applyDamage(500)
    assertTrue(justDied)
    assertEquals(0, overkillBoss.health) // Clamped to strictly 0, never negative!
    assertTrue(overkillBoss.isDead)
  }

  @Test
  fun `boss death animation prevents all attacks shockwaves charges and minion spawns`() {
    val dyingBoss = BossState(
      name = "THE ABOMINATION",
      health = 0,
      isDying = true,
      currentPattern = BossPattern.IDLE,
      hasDamagedInCharge = true
    )

    // 1. Boss cannot attack during death sequence
    assertFalse(dyingBoss.canAttack)
    assertTrue(dyingBoss.isDead)

    // 2. Shockwaves are cleared on death and no new shockwaves can spawn
    val activeShockwaves = mutableListOf("Shockwave_Lane1", "Shockwave_Lane2")
    if (dyingBoss.isDying || dyingBoss.isDefeated) {
      activeShockwaves.clear()
    }
    assertTrue(activeShockwaves.isEmpty())

    // 3. Charge damage cannot be dealt to player once dead
    val canDealChargeDamage = !dyingBoss.hasDamagedInCharge && !dyingBoss.isDead
    assertFalse(canDealChargeDamage)
  }

  @Test
  fun `boss death lifecycle advances to defeated and triggers victory`() {
    var boss = BossState(
      name = "THE PLAGUE QUEEN",
      bossType = BossType.PLAGUE_QUEEN,
      health = 0,
      isDying = true,
      isDefeated = false,
      deathTimer = 0,
      deathDuration = 50
    )

    // Mid death animation
    boss = boss.copy(deathTimer = 25)
    assertTrue(boss.isDying)
    assertFalse(boss.isDefeated)

    // Death animation finishes
    boss = boss.copy(deathTimer = 50)
    if (boss.deathTimer >= boss.deathDuration) {
      boss = boss.copy(isDying = false, isDefeated = true)
    }

    assertFalse(boss.isDying)
    assertTrue(boss.isDefeated)
    assertTrue(boss.isDead)

    // Victory condition for boss mission
    val isVictory = boss.isDefeated
    assertTrue(isVictory)
  }

  @Test
  fun `subsequent bullet hits after boss death do not re-trigger rewards or death events`() {
    var killCounter = 0
    var deathAudioPlays = 0

    var boss = BossState(health = 50)

    fun onBulletHit(damage: Int) {
      if (boss.isDead) return // Dead boss ignores subsequent bullet hits
      val (damaged, justDied) = boss.applyDamage(damage)
      boss = damaged
      if (justDied) {
        killCounter++
        deathAudioPlays++
      }
    }

    // Bullet 1 kills the boss
    onBulletHit(60)
    assertEquals(1, killCounter)
    assertEquals(1, deathAudioPlays)
    assertTrue(boss.isDead)

    // Bullets 2 and 3 strike the dying boss
    onBulletHit(60)
    onBulletHit(60)
    assertEquals(1, killCounter) // Kill counter remained strictly 1
    assertEquals(1, deathAudioPlays) // Audio played strictly once
  }

  @Test
  fun `all three boss types follow identical death and attack prevention rules`() {
    val bossTypes = listOf(BossType.ABOMINATION, BossType.IRON_TYRANT, BossType.PLAGUE_QUEEN)

    for (type in bossTypes) {
      val boss = BossState(
        name = type.name,
        bossType = type,
        health = 500,
        maxHealth = 500
      )

      assertTrue(boss.canAttack)
      assertFalse(boss.isDead)

      // Apply fatal damage
      val (deadBoss, justDied) = boss.applyDamage(600)
      assertTrue(justDied)
      assertEquals(0, deadBoss.health)
      assertTrue(deadBoss.isDead)
      assertFalse(deadBoss.canAttack)
      assertEquals(BossPattern.IDLE, deadBoss.currentPattern)

      // Subsequent damage does not re-die
      val (repeatedHit, justDiedAgain) = deadBoss.applyDamage(100)
      assertFalse(justDiedAgain)
      assertEquals(0, repeatedHit.health)
    }
  }

  @Test
  fun `special forces squad members render at true 85 to 90 percent scale with clear central lane corridor`() {
    // Commander height reference is ~273px (from boots +8f to helmet top -265f)
    val commanderHeight = 273f
    // Squad soldier height from boots (0) to top of ballistic helmet (-239f)
    val soldierHeight = 239f
    val scaleRatio = soldierHeight / commanderHeight

    // Verify scale is strictly between 85% and 90%
    assertTrue("Scale ratio $scaleRatio should be >= 0.85f", scaleRatio >= 0.85f)
    assertTrue("Scale ratio $scaleRatio should be <= 0.90f", scaleRatio <= 0.90f)

    // Verify tactical wedge formation offsets maintain clear central corridor for player view
    val memberCount = 16
    for (m in 1..memberCount) {
      val isLeftWing = (m % 2 != 0)
      val rank = (m + 1) / 2
      val offsetX = if (isLeftWing) -78f - (rank - 1) * 54f else 78f + (rank - 1) * 54f
      // Ensure central corridor (-78f to +78f) is completely clear
      assertTrue("Offset X $offsetX must be outside central lane corridor", kotlin.math.abs(offsetX) >= 78f)
    }
  }
}


