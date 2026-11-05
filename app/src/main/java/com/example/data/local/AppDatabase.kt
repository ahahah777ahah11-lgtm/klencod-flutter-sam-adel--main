package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AuditLogDao
import com.example.data.local.dao.CustomerDao
import com.example.data.local.dao.DebtDao
import com.example.data.local.dao.MessageLogDao
import com.example.data.local.dao.SettingDao
import com.example.data.local.dao.TemplateDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.dao.TransferDao
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.Customer
import com.example.data.local.entity.Debt
import com.example.data.local.entity.MessageLogEntity
import com.example.data.local.entity.SettingEntity
import com.example.data.local.entity.TemplateEntity
import com.example.data.local.entity.TransferEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Customer::class,
        Debt::class,
        TransferEntity::class,
        TemplateEntity::class,
        MessageLogEntity::class,
        SettingEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun debtDao(): DebtDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transferDao(): TransferDao
    abstract fun templateDao(): TemplateDao
    abstract fun messageLogDao(): MessageLogDao
    abstract fun settingDao(): SettingDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_debt_manager.db"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }

            private suspend fun populateInitialData(database: AppDatabase) {
                // Initial Default Settings
                database.settingDao().setAll(
                    listOf(
                        SettingEntity("store_name", "متجري التجاري"),
                        SettingEntity("contact_phone", ""),
                        SettingEntity("currency", "ر.ي"),
                        SettingEntity("allow_credit_balance", "false"),
                        SettingEntity("theme_style", "blue_teal"), // blue_teal, red_white
                        SettingEntity("dark_mode", "system") // system, light, dark
                    )
                )

                // Initial Default SMS Templates
                database.templateDao().insertAll(
                    listOf(
                        TemplateEntity(
                            name = "تذكير رصيد قياسي",
                            body = "مرحبًا {اسم_العميل}، نود تذكيركم بأن الرصيد المتبقي عليكم هو {الرصيد}. إجمالي الديون {إجمالي_الديون}، وإجمالي المدفوعات {إجمالي_المدفوعات}. تاريخ الكشف {التاريخ}. شكرًا لكم.",
                            isDefault = true
                        ),
                        TemplateEntity(
                            name = "تذكير استحقاق متأخر",
                            body = "الأخ/الأخت {اسم_العميل} المحترم، نرجو التكرم بسداد المبلغ المستحق عليكم وقدره {الرصيد} لدى {اسم_المتجر}. للتواصل: {رقم_التواصل}.",
                            isDefault = false
                        ),
                        TemplateEntity(
                            name = "شكر واستلام دفعة",
                            body = "عزيزنا {اسم_العميل}، تم استلام دفعتكم بنجاح. رصيدكم الحالي هو {الرصيد}. نسعد دائمًا بخدمتكم في {اسم_المتجر}.",
                            isDefault = false
                        )
                    )
                )
            }
        }
    }
}
