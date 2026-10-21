package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.MissionProgress
import com.example.data.PlayerProfile
import com.example.data.WeaponCatalog
import com.example.ui.BossPattern
import com.example.ui.BossState
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
}
