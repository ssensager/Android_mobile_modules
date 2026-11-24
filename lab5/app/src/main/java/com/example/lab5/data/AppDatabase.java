package com.example.lab5.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.lab5.data.dao.ActiveSubstanceDao;
import com.example.lab5.data.dao.ConflictRuleDao;
import com.example.lab5.data.dao.IntakeDao;
import com.example.lab5.data.dao.MedicineDao;
import com.example.lab5.data.entity.ActiveSubstance;
import com.example.lab5.data.entity.ConflictRule;
import com.example.lab5.data.entity.Intake;
import com.example.lab5.data.entity.Medicine;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Собрал единую базу Room из четырёх связанных таблиц и вынес запись в отдельный пул потоков.
@Database(
        entities = {
                ActiveSubstance.class,
                Medicine.class,
                ConflictRule.class,
                Intake.class
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "medcontrol_db";
    private static volatile AppDatabase instance;

    // Все операции записи выполняю через этот ExecutorService, чтобы не блокировать главный поток.
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(4);

    public abstract ActiveSubstanceDao activeSubstanceDao();

    public abstract MedicineDao medicineDao();

    public abstract ConflictRuleDao conflictRuleDao();

    public abstract IntakeDao intakeDao();

    public static AppDatabase getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    Context appContext = context.getApplicationContext();
                    instance = Room.databaseBuilder(appContext, AppDatabase.class, DATABASE_NAME)
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    // Заполняю начальные данные в фоне сразу после первого создания файла базы.
                                    databaseWriteExecutor.execute(() -> seedInitialData(getInstance(appContext)));
                                }
                            })
                            .build();
                }
            }
        }
        return instance;
    }

    // Наполняю справочники и стартовую аптечку в одной транзакции, чтобы экран обновился целиком.
    private static void seedInitialData(@NonNull AppDatabase db) {
        if (db.activeSubstanceDao().countSync() > 0) {
            return;
        }
        db.runInTransaction(() -> {
            ActiveSubstanceDao substanceDao = db.activeSubstanceDao();
            ConflictRuleDao ruleDao = db.conflictRuleDao();
            MedicineDao medicineDao = db.medicineDao();
            IntakeDao intakeDao = db.intakeDao();

            int ibuprofenId = (int) substanceDao.insert(new ActiveSubstance(
                    "Ибупрофен",
                    "Нестероидное противовоспалительное средство (НПВП), снижает боль и температуру"
            ));
            int aspirinId = (int) substanceDao.insert(new ActiveSubstance(
                    "Аспирин",
                    "Ацетилсалициловая кислота, разжижает кровь и уменьшает воспаление"
            ));
            int warfarinId = (int) substanceDao.insert(new ActiveSubstance(
                    "Варфарин",
                    "Антикоагулянт непрямого действия, снижает свёртываемость крови"
            ));
            int paracetamolId = (int) substanceDao.insert(new ActiveSubstance(
                    "Парацетамол",
                    "Жаропонижающее и обезболивающее средство с мягким действием на желудок"
            ));
            int caffeineId = (int) substanceDao.insert(new ActiveSubstance(
                    "Кофеин",
                    "Стимулятор нервной системы, часто встречается в комбинированных препаратах от боли"
            ));
            int omeprazoleId = (int) substanceDao.insert(new ActiveSubstance(
                    "Омепразол",
                    "Ингибитор протонной помпы, снижает кислотность и защищает слизистую желудка"
            ));

            ruleDao.insertAll(Arrays.asList(
                    new ConflictRule(
                            ibuprofenId,
                            aspirinId,
                            ConflictRule.SEVERITY_HIGH,
                            "Риск желудочного кровотечения",
                            "Ибупрофен ослабляет защитный эффект аспирина и усиливает раздражение слизистой желудка. Обсудите замену с врачом."
                    ),
                    new ConflictRule(
                            warfarinId,
                            ibuprofenId,
                            ConflictRule.SEVERITY_HIGH,
                            "Критическое усиление кровоточивости",
                            "Совместный приём резко повышает риск внутренних кровотечений. Не совмещайте без врачебного контроля МНО."
                    ),
                    new ConflictRule(
                            warfarinId,
                            aspirinId,
                            ConflictRule.SEVERITY_HIGH,
                            "Двойное разжижение крови",
                            "Одновременный приём антикоагулянта и аспирина опасен тяжёлыми кровотечениями. Требуется срочная консультация врача."
                    ),
                    new ConflictRule(
                            paracetamolId,
                            caffeineId,
                            ConflictRule.SEVERITY_MEDIUM,
                            "Повышенная нагрузка на сердце и печень",
                            "Кофеин усиливает действие парацетамола, но может вызывать тахикардию и повышать риск передозировки. Контролируйте суточную дозу."
                    ),
                    new ConflictRule(
                            ibuprofenId,
                            omeprazoleId,
                            ConflictRule.SEVERITY_LOW,
                            "Замедление всасывания препарата",
                            "Омепразол защищает желудок при приёме НПВП, но может замедлять начало действия. Разносите приём на 30–60 минут."
                    )
            ));

            long now = System.currentTimeMillis();
            long hourMs = 60L * 60L * 1000L;

            int nurofenId = (int) medicineDao.insert(new Medicine(
                    "Нурофен",
                    "400 мг",
                    ibuprofenId,
                    "09:00 и 21:00",
                    "Принимать после еды, запивая стаканом воды"
            ));
            int cardiomagnylId = (int) medicineDao.insert(new Medicine(
                    "Кардиомагнил",
                    "75 мг",
                    aspirinId,
                    "20:00",
                    "Ежедневный вечерний приём по назначению терапевта"
            ));
            int panadolId = (int) medicineDao.insert(new Medicine(
                    "Панадол",
                    "500 мг",
                    paracetamolId,
                    "14:00",
                    "Принимать при температуре выше 38 °C или головной боли"
            ));

            intakeDao.insertAll(Arrays.asList(
                    new Intake(nurofenId, now - 4 * hourMs, true),
                    new Intake(nurofenId, now + 2 * hourMs, false),
                    new Intake(cardiomagnylId, now + 3 * hourMs, false),
                    new Intake(panadolId, now - 2 * hourMs, true)
            ));
        });
    }
}
