package com.example.lab4;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

/**
 * Модель карточки комнатного растения.
 * Храню здесь локальный путь к сжатому фото и идентификатор события календаря.
 */
public class PlantItem {

    private final String id;
    private final String name;
    private final String species;
    private final String lightNeeds;
    private final int wateringIntervalDays;
    private final String temperature;
    private final String notes;
    private final String imagePath;
    private final long calendarEventId;
    private final long fileSizeKb;

    public PlantItem(
            String id,
            String name,
            String species,
            String lightNeeds,
            int wateringIntervalDays,
            String temperature,
            String notes,
            String imagePath,
            long calendarEventId,
            long fileSizeKb
    ) {
        this.id = id;
        this.name = name;
        this.species = species;
        this.lightNeeds = lightNeeds;
        this.wateringIntervalDays = Math.max(1, wateringIntervalDays);
        this.temperature = temperature;
        this.notes = notes;
        this.imagePath = imagePath != null ? imagePath : "";
        this.calendarEventId = calendarEventId;
        this.fileSizeKb = Math.max(0, fileSizeKb);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSpecies() {
        return species;
    }

    public String getLightNeeds() {
        return lightNeeds;
    }

    public int getWateringIntervalDays() {
        return wateringIntervalDays;
    }

    public String getTemperature() {
        return temperature;
    }

    public String getNotes() {
        return notes;
    }

    public String getImagePath() {
        return imagePath;
    }

    public long getCalendarEventId() {
        return calendarEventId;
    }

    public long getFileSizeKb() {
        return fileSizeKb;
    }

    public boolean hasCalendarEvent() {
        return calendarEventId > 0L;
    }

    public boolean hasImage() {
        return imagePath != null && !imagePath.isEmpty();
    }

    // Создаю копию карточки с обновлённым идентификатором события календаря
    public PlantItem withCalendarEventId(long newEventId) {
        return new PlantItem(
                id,
                name,
                species,
                lightNeeds,
                wateringIntervalDays,
                temperature,
                notes,
                imagePath,
                newEventId,
                fileSizeKb
        );
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("name", name);
        obj.put("species", species);
        obj.put("lightNeeds", lightNeeds);
        obj.put("wateringIntervalDays", wateringIntervalDays);
        obj.put("temperature", temperature);
        obj.put("notes", notes);
        obj.put("imagePath", imagePath);
        obj.put("calendarEventId", calendarEventId);
        obj.put("fileSizeKb", fileSizeKb);
        return obj;
    }

    public static PlantItem fromJson(JSONObject obj) {
        return new PlantItem(
                obj.optString("id", ""),
                obj.optString("name", ""),
                obj.optString("species", ""),
                obj.optString("lightNeeds", "Яркий рассеянный"),
                obj.optInt("wateringIntervalDays", 3),
                obj.optString("temperature", "20–25 °C"),
                obj.optString("notes", ""),
                obj.optString("imagePath", ""),
                obj.optLong("calendarEventId", -1L),
                obj.optLong("fileSizeKb", 0L)
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PlantItem)) {
            return false;
        }
        PlantItem other = (PlantItem) o;
        return wateringIntervalDays == other.wateringIntervalDays
                && calendarEventId == other.calendarEventId
                && fileSizeKb == other.fileSizeKb
                && Objects.equals(id, other.id)
                && Objects.equals(name, other.name)
                && Objects.equals(species, other.species)
                && Objects.equals(lightNeeds, other.lightNeeds)
                && Objects.equals(temperature, other.temperature)
                && Objects.equals(notes, other.notes)
                && Objects.equals(imagePath, other.imagePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id, name, species, lightNeeds, wateringIntervalDays,
                temperature, notes, imagePath, calendarEventId, fileSizeKb
        );
    }
}
