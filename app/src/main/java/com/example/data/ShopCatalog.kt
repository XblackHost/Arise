package com.example.data

import com.example.data.model.Equipment
import com.example.data.model.EquipmentSlot
import com.example.data.model.ItemRarity

enum class ShopCategory(val label: String, val icon: String) {
    WEAPONS("Weapons", "⚔️"),
    ARMOR("Armor", "🛡️"),
    ACCESSORIES("Accessories", "💍"),
    CONSUMABLES("Consumables", "🧪"),
    COSMETICS("Cosmetics", "✨"),
    CLASS_EXCLUSIVE("Class Exclusive", "👑"),
    FEATURED("Featured", "🏆")
}

data class ShopItem(
    val id: String,
    val equipment: Equipment,
    val goldPrice: Int = 0,
    val crystalPrice: Int = 0,
    val requiredLevel: Int = 1,
    val category: ShopCategory,
    val isCosmetic: Boolean = false,
    val isConsumable: Boolean = false,
    val consumableId: String? = null,
    val className: String? = null
)

object ShopCatalog {

    val allItems: List<ShopItem> = listOf(
        // WEAPONS
        ShopItem("shop_iron_dagger", Equipment(name = "Iron Dagger", slot = EquipmentSlot.WEAPON,
            rarity = ItemRarity.COMMON, attackBonus = 8, speedBonus = 4,
            description = "Cheap and quick. Favoured by novice assassins.", valueGold = 120),
            goldPrice = 120, requiredLevel = 1, category = ShopCategory.WEAPONS),
        ShopItem("shop_silver_rapier", Equipment(name = "Silver Rapier", slot = EquipmentSlot.WEAPON,
            rarity = ItemRarity.UNCOMMON, attackBonus = 22, speedBonus = 6,
            description = "Forged for precision duelists. Cuts through light armor.", valueGold = 400),
            goldPrice = 400, requiredLevel = 3, category = ShopCategory.WEAPONS),
        ShopItem("shop_moonsilver_blade", Equipment(name = "Moonsilver Blade", slot = EquipmentSlot.WEAPON,
            rarity = ItemRarity.RARE, attackBonus = 45, hpBonus = 20,
            description = "Blessed under a blood moon.", valueGold = 1100),
            goldPrice = 1100, requiredLevel = 6, category = ShopCategory.WEAPONS),
        ShopItem("shop_bloodrose_edge", Equipment(name = "Bloodrose Edge", slot = EquipmentSlot.WEAPON,
            rarity = ItemRarity.EPIC, attackBonus = 70, speedBonus = 10,
            specialEffect = "+15% crit on enemies below 40% HP",
            description = "A cursed blade that feeds on desperation.", valueGold = 1800),
            goldPrice = 1800, crystalPrice = 2, requiredLevel = 10, category = ShopCategory.WEAPONS),
        ShopItem("shop_igris_gauntlet", Equipment(name = "Igris' Bloodred Gauntlet", slot = EquipmentSlot.WEAPON,
            rarity = ItemRarity.LEGENDARY, attackBonus = 110, hpBonus = 50,
            specialEffect = "SHADOW_SUMMON deals +25% damage",
            description = "Salvaged from the throne-room floor. Still warm.", valueGold = 0),
            crystalPrice = 6, requiredLevel = 15, category = ShopCategory.WEAPONS),

        // ARMOR
        ShopItem("shop_leather_cap", Equipment(name = "Hunter Leather Cap", slot = EquipmentSlot.HELMET,
            rarity = ItemRarity.COMMON, defenseBonus = 5, hpBonus = 15,
            description = "Standard-issue headgear.", valueGold = 150),
            goldPrice = 150, requiredLevel = 1, category = ShopCategory.ARMOR),
        ShopItem("shop_reinforced_greaves", Equipment(name = "Reinforced Greaves", slot = EquipmentSlot.BOOTS,
            rarity = ItemRarity.UNCOMMON, defenseBonus = 10, speedBonus = 4,
            description = "Heavy plating for lower-body protection.", valueGold = 380),
            goldPrice = 380, requiredLevel = 3, category = ShopCategory.ARMOR),
        ShopItem("shop_warden_plate", Equipment(name = "Warden's Chestplate", slot = EquipmentSlot.CHEST_ARMOR,
            rarity = ItemRarity.RARE, defenseBonus = 25, hpBonus = 60,
            description = "Frontline armor tempered through a hundred gates.", valueGold = 1300),
            goldPrice = 1300, requiredLevel = 6, category = ShopCategory.ARMOR),
        ShopItem("shop_monarch_mantle", Equipment(name = "Monarch's Mantle", slot = EquipmentSlot.CHEST_ARMOR,
            rarity = ItemRarity.EPIC, defenseBonus = 35, hpBonus = 100, mpBonus = 50,
            specialEffect = "Shadow Army gains +10% ATK",
            description = "Woven from shades that once served a fallen sovereign.", valueGold = 1800),
            goldPrice = 1800, crystalPrice = 3, requiredLevel = 10, category = ShopCategory.ARMOR),
        ShopItem("shop_kamish_scale", Equipment(name = "Kamish's Aegis Scale", slot = EquipmentSlot.CHEST_ARMOR,
            rarity = ItemRarity.MYTHIC, attackBonus = 60, defenseBonus = 55, hpBonus = 200, mpBonus = 80,
            specialEffect = "All Shadow synergy bonuses +15%",
            description = "The final scale of the calamity.", valueGold = 0),
            crystalPrice = 15, requiredLevel = 20, category = ShopCategory.ARMOR),

        // ACCESSORIES
        ShopItem("shop_mana_ring", Equipment(name = "Ring of Mana Reserves", slot = EquipmentSlot.ACCESSORY,
            rarity = ItemRarity.UNCOMMON, mpBonus = 35,
            description = "Stores a reservoir of ambient mana.", valueGold = 450),
            goldPrice = 450, requiredLevel = 3, category = ShopCategory.ACCESSORIES),
        ShopItem("shop_focus_amulet", Equipment(name = "Amulet of Focus", slot = EquipmentSlot.ACCESSORY,
            rarity = ItemRarity.RARE, attackBonus = 12, mpBonus = 40,
            specialEffect = "+5% MP regen on DEFEND",
            description = "Sharpens concentration.", valueGold = 950),
            goldPrice = 950, requiredLevel = 6, category = ShopCategory.ACCESSORIES),
        ShopItem("shop_swiftstride", Equipment(name = "Swiftstride Boots", slot = EquipmentSlot.BOOTS,
            rarity = ItemRarity.RARE, speedBonus = 8, hpBonus = 20,
            description = "Featherlight boots for extreme mobility.", valueGold = 900),
            goldPrice = 900, requiredLevel = 6, category = ShopCategory.ACCESSORIES),
        ShopItem("shop_silum_signet", Equipment(name = "Silum's Frost Signet", slot = EquipmentSlot.ACCESSORY,
            rarity = ItemRarity.LEGENDARY, attackBonus = 55, mpBonus = 80, speedBonus = 12,
            specialEffect = "DEFEND also slows boss charge timer",
            description = "Cold enough to chill the System's notifications.", valueGold = 0),
            crystalPrice = 8, requiredLevel = 15, category = ShopCategory.ACCESSORIES),

        // CONSUMABLES
        ShopItem("cons_elixir", Equipment(name = "Elixir of Life", slot = EquipmentSlot.ACCESSORY,
            rarity = ItemRarity.COMMON, description = "+60 HP mid-raid."),
            goldPrice = 150, category = ShopCategory.CONSUMABLES, isConsumable = true, consumableId = "cons_elixir"),
        ShopItem("cons_tonic", Equipment(name = "Mana Tonic", slot = EquipmentSlot.ACCESSORY,
            rarity = ItemRarity.COMMON, description = "+30 MP mid-raid."),
            goldPrice = 150, category = ShopCategory.CONSUMABLES, isConsumable = true, consumableId = "cons_tonic"),
        ShopItem("cons_beacon", Equipment(name = "Gate Beacon", slot = EquipmentSlot.ACCESSORY,
            rarity = ItemRarity.UNCOMMON, description = "Teleport to any spawned gate."),
            goldPrice = 200, category = ShopCategory.CONSUMABLES, isConsumable = true, consumableId = "cons_beacon"),
        ShopItem("cons_timerskip", Equipment(name = "Timer Skip", slot = EquipmentSlot.ACCESSORY,
            rarity = ItemRarity.COMMON, description = "Instantly complete any TIMER quest."),
            goldPrice = 80, category = ShopCategory.CONSUMABLES, isConsumable = true, consumableId = "cons_timerskip"),
        ShopItem("cons_reroll", Equipment(name = "Reroll Gate", slot = EquipmentSlot.ACCESSORY,
            rarity = ItemRarity.UNCOMMON, description = "Replace spawned gates with a fresh set."),
            goldPrice = 250, category = ShopCategory.CONSUMABLES, isConsumable = true, consumableId = "cons_reroll"),
        ShopItem("cons_revive", Equipment(name = "Shadow Revive Charm", slot = EquipmentSlot.ACCESSORY,
            rarity = ItemRarity.UNCOMMON, description = "Free shadow reconstitution, bypasses MP cost."),
            goldPrice = 200, category = ShopCategory.CONSUMABLES, isConsumable = true, consumableId = "cons_revive"),
        ShopItem("cons_questboost", Equipment(name = "Focused Vigor", slot = EquipmentSlot.ACCESSORY,
            rarity = ItemRarity.RARE, description = "+50% XP from next 5 completed quests."),
            goldPrice = 300, category = ShopCategory.CONSUMABLES, isConsumable = true, consumableId = "cons_questboost"),

        // COSMETICS
        ShopItem("cos_aura_cyan", Equipment(name = "Aura: Cyan", slot = EquipmentSlot.ACCESSORY,
            rarity = ItemRarity.RARE, description = "Cyan profile glow."),
            goldPrice = 300, category = ShopCategory.COSMETICS, isCosmetic = true),
        ShopItem("cos_aura_violet", Equipment(name = "Aura: Violet", slot = EquipmentSlot.ACCESSORY,
            rarity = ItemRarity.RARE, description = "Violet profile glow."),
            goldPrice = 300, category = ShopCategory.COSMETICS, isCosmetic = true),
        ShopItem("cos_aura_bloodred", Equipment(name = "Aura: Bloodred", slot = EquipmentSlot.ACCESSORY,
            rarity = ItemRarity.EPIC, description = "Bloodred profile glow."),
            goldPrice = 300, category = ShopCategory.COSMETICS, isCosmetic = true),
        ShopItem("cos_aura_gold", Equipment(name = "Aura: Gold", slot = EquipmentSlot.ACCESSORY,
            rarity = ItemRarity.EPIC, description = "Golden profile glow."),
            goldPrice = 300, category = ShopCategory.COSMETICS, isCosmetic = true),
        ShopItem("cos_shadow_tint", Equipment(name = "Shadow Tint", slot = EquipmentSlot.ACCESSORY,
            rarity = ItemRarity.EPIC, description = "Recolors the whole Shadow Army."),
            crystalPrice = 1, category = ShopCategory.COSMETICS, isCosmetic = true),
        ShopItem("cos_summon_vfx", Equipment(name = "Summon VFX", slot = EquipmentSlot.ACCESSORY,
            rarity = ItemRarity.LEGENDARY, description = "New ARISE extraction effect."),
            crystalPrice = 2, category = ShopCategory.COSMETICS, isCosmetic = true),
    )

    fun featuredOfWeek(): List<ShopItem> {
        val week = java.util.Calendar.getInstance().get(java.util.Calendar.WEEK_OF_YEAR)
        val pool = allItems.filter { it.category != ShopCategory.CONSUMABLES && it.category != ShopCategory.FEATURED }
        if (pool.isEmpty()) return emptyList()
        val pick = pool[week % pool.size]
        val discounted = if (pick.goldPrice > 0)
            pick.copy(goldPrice = pick.goldPrice * 70 / 100)
        else pick
        return listOf(discounted.copy(category = ShopCategory.FEATURED))
    }
}

object ClassExclusives {
    data class Def(val itemName: String, val effect: String, val slot: EquipmentSlot)

    private val map = mapOf(
        "Warrior" to Def("Titan's Bracer", "+20% Basic Strike damage", EquipmentSlot.ACCESSORY),
        "Assassin" to Def("Phantom Shroud", "+15% backstab window", EquipmentSlot.CHEST_ARMOR),
        "Mage" to Def("Runeweaver Orb", "Class Skill costs -5 MP", EquipmentSlot.ACCESSORY),
        "Shadow Monarch" to Def("Sovereign Ring", "+1 max deployed shadow", EquipmentSlot.ACCESSORY),
        "Paladin" to Def("Radiant Aegis", "+20% healing on HEAL", EquipmentSlot.ACCESSORY),
        "Necromancer" to Def("Bone Crown", "Shadow reconstitution costs -3 MP", EquipmentSlot.HELMET),
    )

    fun forClass(className: String): ShopItem {
        val def = map[className] ?: Def("$className Sigil", "+5% all stats", EquipmentSlot.ACCESSORY)
        return ShopItem(
            id = "class_${className.lowercase().replace(' ', '_')}",
            equipment = Equipment(
                name = def.itemName,
                slot = def.slot,
                rarity = ItemRarity.EPIC,
                attackBonus = 20, defenseBonus = 10, hpBonus = 40,
                specialEffect = def.effect,
                description = "Exclusive to $className. Cannot be dropped.",
                valueGold = 900
            ),
            goldPrice = 900,
            requiredLevel = 8,
            category = ShopCategory.CLASS_EXCLUSIVE,
            className = className
        )
    }
}
