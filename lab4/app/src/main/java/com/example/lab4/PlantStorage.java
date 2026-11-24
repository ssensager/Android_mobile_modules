package com.example.lab4;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Локальное файловое хранилище каталога растений в plants.json.
 * Храню данные во внутренней директории приложения, чтобы записи и фото спокойно переживали перезагрузку.
 */
public class PlantStorage {

    private static final String FILE_NAME = "plants.json";
    private static final String PREFS_NAME = "flora_guide_prefs";
    private static final String KEY_SEEDED = "initial_seed_done";
    private static final String KEY_GRID_MODE = "catalog_grid_mode";

    private final Context appContext;
    private final SharedPreferences prefs;

    public PlantStorage(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isGridMode() {
        return prefs.getBoolean(KEY_GRID_MODE, false);
    }

    public void setGridMode(boolean gridMode) {
        prefs.edit().putBoolean(KEY_GRID_MODE, gridMode).apply();
    }

    // При самом первом запуске добавляю 4 реалистичных растения со сжатыми JPEG-изображениями
    public synchronized List<PlantItem> loadOrSeedPlants() {
        if (!prefs.getBoolean(KEY_SEEDED, false)) {
            List<PlantItem> seeded = createInitialPlants();
            savePlants(seeded);
            prefs.edit().putBoolean(KEY_SEEDED, true).apply();
            return seeded;
        }
        return loadPlants();
    }

    public synchronized List<PlantItem> loadPlants() {
        File file = new File(appContext.getFilesDir(), FILE_NAME);
        List<PlantItem> result = new ArrayList<>();
        if (!file.exists()) {
            return result;
        }

        try {
            String rawJson = readTextFile(file);
            JSONArray array = new JSONArray(rawJson);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj != null) {
                    result.add(PlantItem.fromJson(obj));
                }
            }
        } catch (IOException | JSONException ignored) {
            // Возвращаю пустой список, если файл ещё не сформирован или повреждён
        }
        return result;
    }

    public synchronized void savePlants(List<PlantItem> plants) {
        JSONArray array = new JSONArray();
        for (PlantItem item : plants) {
            try {
                array.put(item.toJson());
            } catch (JSONException ignored) {
            }
        }

        File file = new File(appContext.getFilesDir(), FILE_NAME);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(array.toString(2).getBytes(StandardCharsets.UTF_8));
            fos.flush();
        } catch (IOException | JSONException ignored) {
        }
    }

    public synchronized void addPlant(PlantItem item) {
        List<PlantItem> plants = loadPlants();
        plants.add(0, item);
        savePlants(plants);
    }

    public synchronized void updatePlant(PlantItem updated) {
        List<PlantItem> plants = loadPlants();
        for (int i = 0; i < plants.size(); i++) {
            if (plants.get(i).getId().equals(updated.getId())) {
                plants.set(i, updated);
                savePlants(plants);
                return;
            }
        }
    }

    public synchronized PlantItem findById(String id) {
        if (id == null) {
            return null;
        }
        for (PlantItem item : loadPlants()) {
            if (id.equals(item.getId())) {
                return item;
            }
        }
        return null;
    }

    public synchronized PlantItem removeById(String id) {
        List<PlantItem> plants = loadPlants();
        for (int i = 0; i < plants.size(); i++) {
            if (plants.get(i).getId().equals(id)) {
                PlantItem removed = plants.remove(i);
                savePlants(plants);
                return removed;
            }
        }
        return null;
    }

    private List<PlantItem> createInitialPlants() {
        List<PlantItem> list = new ArrayList<>();
        list.add(buildSeededPlant(
                0,
                "Монстера деликатесная",
                "Monstera deliciosa",
                "Яркий рассеянный",
                4,
                "20–26 °C",
                "Любит опрыскивание тёплой водой и опору из кокосового волокна. Не ставить под прямые лучи.",
                "seed_monstera"
        ));
        list.add(buildSeededPlant(
                1,
                "Фикус Бенджамина",
                "Ficus benjamina",
                "Яркий рассеянный",
                3,
                "18–24 °C",
                "Не любит сквозняки и частые перестановки. Поливаю, когда верхний слой грунта подсох на 2 см.",
                "seed_ficus"
        ));
        list.add(buildSeededPlant(
                2,
                "Сансевиерия трёхполосная",
                "Sansevieria trifasciata",
                "Полутень",
                10,
                "16–28 °C",
                "Очень неприхотлива, спокойно переносит сухой воздух. Важно не заливать розетку в центре.",
                "seed_sansevieria"
        ));
        list.add(buildSeededPlant(
                3,
                "Замиокулькас замиелистный",
                "Zamioculcas zamiifolia",
                "Теневыносливое",
                12,
                "18–26 °C",
                "Накапливает влагу в клубнях, поэтому поливаю редко и умеренно. Протираю листья от пыли раз в неделю.",
                "seed_zamioculcas"
        ));
        return list;
    }

    private PlantItem buildSeededPlant(
            int artIndex,
            String name,
            String species,
            String light,
            int wateringDays,
            String temperature,
            String notes,
            String filePrefix
    ) {
        String path = "";
        long sizeKb = 0L;
        Bitmap art = SamplePlantArt.createPlantIllustration(artIndex);
        try {
            ImageCompressor.CompressionResult saved =
                    ImageCompressor.saveBitmapCompressed(appContext, art, filePrefix);
            path = saved.absolutePath;
            sizeKb = saved.fileSizeKb;
        } catch (IOException ignored) {
        } finally {
            art.recycle();
        }

        return new PlantItem(
                UUID.randomUUID().toString(),
                name,
                species,
                light,
                wateringDays,
                temperature,
                notes,
                path,
                -1L,
                sizeKb
        );
    }

    private String readTextFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            return baos.toString(StandardCharsets.UTF_8.name());
        }
    }
}
