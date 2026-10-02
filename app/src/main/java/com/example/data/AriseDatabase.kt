package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.ChatDao
import com.example.data.dao.ConsumableDao
import com.example.data.dao.EquipmentDao
import com.example.data.dao.MilestoneDao
import com.example.data.dao.PendingOfferDao
import com.example.data.dao.PlayerDao
import com.example.data.dao.PurchaseLogDao
import com.example.data.dao.QuestDao
import com.example.data.dao.ShadowDao
import com.example.data.dao.TaskDao
import com.example.data.dao.VowDao
import com.example.data.model.ChatMessage
import com.example.data.model.Consumable
import com.example.data.model.Equipment
import com.example.data.model.MilestoneLog
import com.example.data.model.PendingOffer
import com.example.data.model.PlayerProfile
import com.example.data.model.PurchaseLog
import com.example.data.model.Quest
import com.example.data.model.ShadowUnit
import com.example.data.model.TaskItem
import com.example.data.model.VowState

@Database(
    entities = [
        PlayerProfile::class,
        Quest::class,
        ShadowUnit::class,
        Equipment::class,
        TaskItem::class,
        ChatMessage::class,
        VowState::class,
        Consumable::class,
        PurchaseLog::class,
        MilestoneLog::class,
        PendingOffer::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AriseDatabase : RoomDatabase() {

    abstract fun playerDao(): PlayerDao
    abstract fun questDao(): QuestDao
    abstract fun shadowDao(): ShadowDao
    abstract fun equipmentDao(): EquipmentDao
    abstract fun taskDao(): TaskDao
    abstract fun chatDao(): ChatDao
    abstract fun vowDao(): VowDao
    abstract fun consumableDao(): ConsumableDao
    abstract fun purchaseLogDao(): PurchaseLogDao
    abstract fun milestoneDao(): MilestoneDao
    abstract fun pendingOfferDao(): PendingOfferDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS vow_state (
                    id INTEGER NOT NULL PRIMARY KEY,
                    isActive INTEGER NOT NULL,
                    currentStreak INTEGER NOT NULL,
                    longestStreak INTEGER NOT NULL,
                    totalRewards INTEGER NOT NULL,
                    lastResetAt INTEGER NOT NULL,
                    lastRewardDate INTEGER NOT NULL,
                    vowStartedAt INTEGER NOT NULL,
                    totalXpEarned INTEGER NOT NULL
                )""")
                db.execSQL("""CREATE TABLE IF NOT EXISTS consumables (
                    itemId TEXT NOT NULL PRIMARY KEY,
                    count INTEGER NOT NULL
                )""")
                db.execSQL("""CREATE TABLE IF NOT EXISTS purchase_log (
                    itemId TEXT NOT NULL PRIMARY KEY,
                    purchasedAt INTEGER NOT NULL
                )""")
                db.execSQL("""CREATE TABLE IF NOT EXISTS milestone_log (
                    milestoneKey TEXT NOT NULL PRIMARY KEY,
                    firedAt INTEGER NOT NULL
                )""")
                db.execSQL("""CREATE TABLE IF NOT EXISTS pending_offers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    milestoneKey TEXT NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    goldCost INTEGER NOT NULL,
                    crystalCost INTEGER NOT NULL,
                    rewardType TEXT NOT NULL,
                    rewardPayload TEXT NOT NULL,
                    generatedAt INTEGER NOT NULL,
                    expiresAt INTEGER NOT NULL,
                    isClaimed INTEGER NOT NULL,
                    isDeclined INTEGER NOT NULL
                )""")
            }
        }

        fun getDatabase(context: Context): AriseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AriseDatabase::class.java,
                    "arise_rpg_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
