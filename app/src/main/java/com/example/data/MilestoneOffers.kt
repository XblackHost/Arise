package com.example.data

import com.example.data.model.Equipment
import com.example.data.model.EquipmentSlot
import com.example.data.model.ItemRarity

object MilestoneOffers {
    data class Template(
        val title: String,
        val description: String,
        val goldCost: Int,
        val crystalCost: Int = 0,
        val rewardType: String,
        val payload: String
    )

    val byKey: Map<String, Template> = mapOf(
        "level_5" to Template("Rookie's Kit", "Bundle: 3× Elixir, 3× Tonic.",
            500, rewardType = "CONSUMABLE_BUNDLE", payload = "cons_elixir:3,cons_tonic:3"),
        "level_10" to Template("Awakened Hunter's Blade",
            "Milestone-exclusive weapon.", 800,
            rewardType = "EQUIPMENT", payload = "milestone_blade_10"),
        "level_25" to Template("Veteran's Raid Pack",
            "5× Elixir, 5× Tonic, 3× Beacon, 1× Revive.", 1200,
            rewardType = "CONSUMABLE_BUNDLE",
            payload = "cons_elixir:5,cons_tonic:5,cons_beacon:3,cons_revive:1"),
        "level_50" to Template("Monarch's Signet",
            "Milestone-exclusive accessory.", 0, crystalCost = 8,
            rewardType = "EQUIPMENT", payload = "milestone_signet_50"),
        "level_100" to Template("Sovereign's Crown",
            "One-of-a-kind mythic helmet.", 0, crystalCost = 25,
            rewardType = "EQUIPMENT", payload = "milestone_crown_100"),
        "streak_10" to Template("Iron-Willed Sigil",
            "Streak-exclusive accessory.", 400,
            rewardType = "EQUIPMENT", payload = "milestone_sigil_streak10"),
        "streak_30" to Template("Unbroken Band",
            "Streak-exclusive ring.", 900,
            rewardType = "EQUIPMENT", payload = "milestone_band_streak30"),
        "streak_100" to Template("Monarch of Discipline Aura",
            "Permanent cosmetic aura.", 0, crystalCost = 15,
            rewardType = "EQUIPMENT", payload = "milestone_aura_streak100"),
        "quests_50" to Template("Hunter's Tome",
            "Focused Vigor ×2, Timer Skip ×2.", 600,
            rewardType = "CONSUMABLE_BUNDLE",
            payload = "cons_questboost:2,cons_timerskip:2"),
        "shadows_5" to Template("Commander's Baton",
            "All shadows +15% ATK.", 0, crystalCost = 6,
            rewardType = "EQUIPMENT", payload = "milestone_baton_shadows5"),
        "first_boss" to Template("First Blood Trophy",
            "Commemorative banner.", 100,
            rewardType = "EQUIPMENT", payload = "milestone_trophy_firstblood"),
        "first_arise" to Template("Sovereign's Whisper",
            "Gift: 3× Mana Crystals.", 0,
            rewardType = "CRYSTALS", payload = "3")
    )
}

object MilestoneRewards {
    fun buildEquipment(payloadId: String): Equipment? = when (payloadId) {
        "milestone_blade_10" -> Equipment(name = "Awakened Hunter's Blade",
            slot = EquipmentSlot.WEAPON, rarity = ItemRarity.EPIC,
            attackBonus = 55, speedBonus = 8,
            specialEffect = "+10% XP from quests",
            description = "Forged the day you first hit Level 10.")
        "milestone_signet_50" -> Equipment(name = "Monarch's Signet",
            slot = EquipmentSlot.ACCESSORY, rarity = ItemRarity.LEGENDARY,
            attackBonus = 40, defenseBonus = 30, hpBonus = 100,
            description = "Only those who reached Level 50 may wear it.")
        "milestone_crown_100" -> Equipment(name = "Sovereign's Crown",
            slot = EquipmentSlot.HELMET, rarity = ItemRarity.MYTHIC,
            attackBonus = 80, defenseBonus = 60, hpBonus = 250, mpBonus = 100,
            specialEffect = "All shop prices -10% while equipped",
            description = "The mark of a Hunter who transcended.")
        "milestone_sigil_streak10" -> Equipment(name = "Iron-Willed Sigil",
            slot = EquipmentSlot.ACCESSORY, rarity = ItemRarity.RARE,
            hpBonus = 30, mpBonus = 30,
            specialEffect = "+1 extra Discipline per Vow reward",
            description = "Proof of ten days held.")
        "milestone_band_streak30" -> Equipment(name = "Unbroken Band",
            slot = EquipmentSlot.ACCESSORY, rarity = ItemRarity.EPIC,
            hpBonus = 60, mpBonus = 60,
            specialEffect = "Shop prices permanently -5%",
            description = "Thirty days unbroken.")
        "milestone_aura_streak100" -> Equipment(name = "Monarch of Discipline Aura",
            slot = EquipmentSlot.ACCESSORY, rarity = ItemRarity.MYTHIC,
            attackBonus = 60, defenseBonus = 60, hpBonus = 150, mpBonus = 150,
            specialEffect = "Displays unique profile aura",
            description = "One hundred days.")
        "milestone_baton_shadows5" -> Equipment(name = "Commander's Baton",
            slot = EquipmentSlot.ACCESSORY, rarity = ItemRarity.LEGENDARY,
            attackBonus = 30, specialEffect = "All shadows +15% ATK",
            description = "Commands five souls.")
        "milestone_trophy_firstblood" -> Equipment(name = "First Blood Trophy",
            slot = EquipmentSlot.ACCESSORY, rarity = ItemRarity.UNCOMMON,
            specialEffect = "Cosmetic banner",
            description = "Your first dungeon victory.")
        else -> null
    }
}
