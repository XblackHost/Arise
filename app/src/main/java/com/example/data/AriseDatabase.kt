package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.ChatDao
import com.example.data.dao.EquipmentDao
import com.example.data.dao.PlayerDao
import com.example.data.dao.QuestDao
import com.example.data.dao.ShadowDao
import com.example.data.dao.TaskDao
import com.example.data.model.ChatMessage
import com.example.data.model.Equipment
import com.example.data.model.EquipmentSlot
import com.example.data.model.ItemRarity
import com.example.data.model.PlayerProfile
import com.example.data.model.Quest
import com.example.data.model.QuestCategory
import com.example.data.model.QuestDifficulty
import com.example.data.model.ShadowUnit
import com.example.data.model.TaskItem
import com.example.data.model.VerificationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Database(
    entities = [
        PlayerProfile::class,
        Quest::class,
        ShadowUnit::class,
        Equipment::class,
        TaskItem::class,
        ChatMessage::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AriseDatabase : RoomDatabase() {

    abstract fun playerDao(): PlayerDao
    abstract fun questDao(): QuestDao
    abstract fun shadowDao(): ShadowDao
    abstract fun equipmentDao(): EquipmentDao
    abstract fun taskDao(): TaskDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AriseDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                fun addColumnIfNotExists(table: String, column: String, typeWithDef: String) {
                    val cursor = db.query("PRAGMA table_info($table)")
                    var exists = false
                    val nameIdx = cursor.getColumnIndex("name")
                    while (cursor.moveToNext()) {
                        if (nameIdx >= 0 && cursor.getString(nameIdx) == column) {
                            exists = true
                            break
                        }
                    }
                    cursor.close()
                    if (!exists) {
                        db.execSQL("ALTER TABLE $table ADD COLUMN $column $typeWithDef")
                    }
                }

                addColumnIfNotExists("shadow_units", "originRank", "TEXT NOT NULL DEFAULT 'E-Rank'")
                addColumnIfNotExists("shadow_units", "mpUpkeepCost", "INTEGER NOT NULL DEFAULT 15")
                addColumnIfNotExists("shadow_units", "isDeployed", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfNotExists("shadow_units", "maxHp", "INTEGER NOT NULL DEFAULT 350")
            }
        }

        fun getDatabase(context: Context): AriseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AriseDatabase::class.java,
                    "arise_rpg_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedInitialDataDirect(database)
                    }
                }
            }
        }

        suspend fun seedInitialDataDirect(db: AriseDatabase) {
            // Initial Player Profile
            if (db.playerDao().getPlayerProfileOnce() != null) return
            db.playerDao().insertProfile(
                    PlayerProfile(
                        id = 1,
                        name = "Awakened Hunter",
                        title = "The Awakened",
                        rank = "E-Rank",
                        level = 1,
                        currentXp = 0,
                        requiredXp = 100,
                        hp = 120,
                        maxHp = 120,
                        mp = 60,
                        maxMp = 60,
                        gold = 300,
                        manaCrystals = 15,
                        unallocatedStatPoints = 5,
                        strength = 12,
                        endurance = 10,
                        agility = 11,
                        intelligence = 10,
                        focus = 10,
                        discipline = 10,
                        vitality = 12,
                        selectedClass = "Warrior",
                        streakDays = 1,
                        totalQuestsCompleted = 0,
                        totalWorkouts = 0,
                        shadowArmyCount = 1,
                        isOnboardingComplete = false
                    )
                )

                // Initial Quests across categories
                val starterQuests = listOf(
                    Quest(
                        title = "Daily Quest: Physical Conditioning",
                        description = "Complete 20 push-ups, 20 sit-ups, and 20 squats to strengthen your mortal vessel.",
                        category = QuestCategory.FITNESS,
                        difficulty = QuestDifficulty.E,
                        xpReward = 60,
                        goldReward = 40,
                        targetAttribute = "STRENGTH",
                        attributeGain = 1,
                        durationMinutes = 15,
                        verificationType = VerificationType.TIMER,
                        isDaily = true
                    ),
                    Quest(
                        title = "Deep Focus: Academic Protocol",
                        description = "Conduct an uninterrupted 25-minute Pomodoro study or reading session without digital distractions.",
                        category = QuestCategory.PRODUCTIVITY,
                        difficulty = QuestDifficulty.E,
                        xpReward = 50,
                        goldReward = 35,
                        targetAttribute = "FOCUS",
                        attributeGain = 1,
                        durationMinutes = 25,
                        verificationType = VerificationType.TIMER,
                        isDaily = true
                    ),
                    Quest(
                        title = "Scouting the Perimeter",
                        description = "Walk or jog at least 1.5 km (or 2,000 steps) to scout physical territory and locate dormant dungeon gates.",
                        category = QuestCategory.EXPLORATION,
                        difficulty = QuestDifficulty.D,
                        xpReward = 75,
                        goldReward = 50,
                        targetAttribute = "ENDURANCE",
                        attributeGain = 1,
                        durationMinutes = 20,
                        verificationType = VerificationType.STEPS,
                        isDaily = true
                    ),
                    Quest(
                        title = "Mental Fortitude: Cold Shower / Hydration",
                        description = "Drink 1L of water upon awakening and practice conscious breathing to steel your willpower.",
                        category = QuestCategory.PERSONAL_DEVELOPMENT,
                        difficulty = QuestDifficulty.E,
                        xpReward = 40,
                        goldReward = 25,
                        targetAttribute = "DISCIPLINE",
                        attributeGain = 1,
                        durationMinutes = 5,
                        verificationType = VerificationType.SELF_CONFIRMATION,
                        isDaily = true
                    ),
                    Quest(
                        title = "Class Awakening: First Stance Drill",
                        description = "Practice stance control, core balance, and dynamic stretching suited for your chosen archetype.",
                        category = QuestCategory.CLASS,
                        difficulty = QuestDifficulty.E,
                        xpReward = 55,
                        goldReward = 30,
                        targetAttribute = "AGILITY",
                        attributeGain = 1,
                        durationMinutes = 10,
                        verificationType = VerificationType.TIMER,
                        isDaily = false
                    )
                )
                db.questDao().insertQuests(starterQuests)

                // Starter Shadow Unit
                db.shadowDao().insertShadow(
                    ShadowUnit(
                        name = "Shadow Infantry",
                        title = "Loyal Footman",
                        rank = "Infantry",
                        level = 1,
                        attackPower = 25,
                        defense = 15,
                        speed = 12,
                        loyalty = 100,
                        signatureSkill = "Shadow Strike",
                        skillDescription = "Lurks within shadows to deliver a swift dark puncture.",
                        isSummoned = true,
                        lore = "The first soul answering the Monarch's nascent call. Unwavering loyalty."
                    )
                )

                // Starter Equipment
                val starterGear = listOf(
                    Equipment(
                        name = "Steel Broadsword",
                        slot = EquipmentSlot.WEAPON,
                        rarity = ItemRarity.COMMON,
                        attackBonus = 12,
                        defenseBonus = 0,
                        isEquipped = true,
                        description = "A standard hunter-grade steel sword forged for early dungeon incursions."
                    ),
                    Equipment(
                        name = "Reinforced Hunter Vest",
                        slot = EquipmentSlot.CHEST_ARMOR,
                        rarity = ItemRarity.COMMON,
                        attackBonus = 0,
                        defenseBonus = 8,
                        hpBonus = 20,
                        isEquipped = true,
                        description = "Tough leather interlaced with carbon fiber mesh to absorb blunt shock."
                    ),
                    Equipment(
                        name = "Swift Runner Boots",
                        slot = EquipmentSlot.BOOTS,
                        rarity = ItemRarity.UNCOMMON,
                        attackBonus = 0,
                        defenseBonus = 3,
                        speedBonus = 10,
                        isEquipped = true,
                        description = "Lightweight aerodynamic boots facilitating rapid tactical repositioning."
                    ),
                    Equipment(
                        name = "Ring of Vigor",
                        slot = EquipmentSlot.ACCESSORY,
                        rarity = ItemRarity.RARE,
                        attackBonus = 4,
                        defenseBonus = 4,
                        hpBonus = 30,
                        mpBonus = 20,
                        isEquipped = true,
                        description = "Glows with a warm turquoise pulse that accelerates natural recuperation."
                    )
                )
                db.equipmentDao().insertAll(starterGear)

                // Starter Tasks
                val starterTasks = listOf(
                    TaskItem(
                        title = "Review Daily Quest Log",
                        notes = "Check what real-life workouts and learning tasks are scheduled.",
                        deadlineText = "Morning",
                        priority = "High",
                        targetStat = "DISCIPLINE",
                        xpReward = 30,
                        goldReward = 20
                    ),
                    TaskItem(
                        title = "Read 10 pages of a book",
                        notes = "Expand knowledge and mental focus.",
                        deadlineText = "Evening",
                        priority = "Medium",
                        targetStat = "INTELLIGENCE",
                        xpReward = 45,
                        goldReward = 25
                    )
                )
                db.taskDao().insertTasks(starterTasks)

                // Starter NYX Welcome Message
                db.chatDao().insertMessage(
                    ChatMessage(
                        sender = "NYX",
                        content = "System initialized. Greetings, Hunter. I am NYX, your designated System Guide. Every real-life effort—each repetition, study hour, and step you take—now directly fuels your in-game ascension. Prepare yourself; the gates are opening."
                    )
                )
            }
        }
    }
