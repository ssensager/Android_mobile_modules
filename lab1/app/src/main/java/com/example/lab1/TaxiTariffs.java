package com.example.lab1;

/**
 * Здесь хранится весь прайс-лист такси в виде простых структур данных и массивов.
 * Вынесли тарифы сюда, а не в цепочки if/else внутри ViewModel, как требует ТЗ:
 * так цены легко менять в одном месте, и они не дублируются между экранами.
 */
public final class TaxiTariffs {

    // Стоимость дополнительных опций в рублях (базовая, до коэффициента спроса)
    public static final int PRICE_CHILD_SEAT = 150;
    public static final int PRICE_LUGGAGE = 100;
    public static final int PRICE_WAITING = 120;
    // Доплата за каждого дополнительного пассажира сверх первого (страхование и комфорт посадки)
    public static final int PRICE_EXTRA_PASSENGER = 40;

    // Описание одного класса автомобиля
    public static final class CarClass {
        public final String name;
        public final String shortDescription;
        public final int baseFare;       // Подача авто, руб
        public final int pricePerKm;     // Цена за 1 км, руб
        public final int pricePerMin;    // Цена за 1 минуту, руб
        public final int maxPassengers;  // Максимальное число пассажиров

        public CarClass(String name, String shortDescription, int baseFare,
                        int pricePerKm, int pricePerMin, int maxPassengers) {
            this.name = name;
            this.shortDescription = shortDescription;
            this.baseFare = baseFare;
            this.pricePerKm = pricePerKm;
            this.pricePerMin = pricePerMin;
            this.maxPassengers = maxPassengers;
        }
    }

    // Описание тарифной зоны маршрута (расстояние и среднее время в пути)
    public static final class TariffZone {
        public final String name;
        public final String subtitle;
        public final int distanceKm;
        public final int durationMin;

        public TariffZone(String name, String subtitle, int distanceKm, int durationMin) {
            this.name = name;
            this.subtitle = subtitle;
            this.distanceKm = distanceKm;
            this.durationMin = durationMin;
        }
    }

    // Описание уровня спроса (коэффициент умножает стоимость поездки)
    public static final class DemandLevel {
        public final String badge;
        public final String label;
        public final float multiplier;

        public DemandLevel(String badge, String label, float multiplier) {
            this.badge = badge;
            this.label = label;
            this.multiplier = multiplier;
        }
    }

    // 5 классов автомобилей — используем обычные массивы, потому что список фиксирован
    // и доступ по индексу работает максимально просто и быстро
    public static final CarClass[] CAR_CLASSES = new CarClass[]{
            new CarClass("Эконом", "Подача 99 ₽ · до 4 мест", 99, 18, 6, 4),
            new CarClass("Комфорт", "Подача 149 ₽ · до 4 мест", 149, 24, 8, 4),
            new CarClass("Комфорт+", "Подача 199 ₽ · до 4 мест", 199, 32, 11, 4),
            new CarClass("Бизнес", "Подача 349 ₽ · до 4 мест", 349, 48, 16, 4),
            new CarClass("Минивэн", "Подача 249 ₽ · до 6 мест", 249, 36, 12, 6)
    };

    // 4 тарифные зоны
    public static final TariffZone[] TARIFF_ZONES = new TariffZone[]{
            new TariffZone("Центр", "4 км · ~12 мин", 4, 12),
            new TariffZone("Город", "9 км · ~22 мин", 9, 22),
            new TariffZone("Пригород", "18 км · ~35 мин", 18, 35),
            new TariffZone("Аэропорт", "32 км · ~48 мин", 32, 48)
    };

    // 4 уровня коэффициента спроса
    public static final DemandLevel[] DEMAND_LEVELS = new DemandLevel[]{
            new DemandLevel("1.0x", "Обычный", 1.0f),
            new DemandLevel("1.3x", "Повышенный", 1.3f),
            new DemandLevel("1.6x", "Час пик", 1.6f),
            new DemandLevel("2.0x", "Непогода", 2.0f)
    };

    private TaxiTariffs() {
        // Запрещаем создавать экземпляры служебного класса с таблицами тарифов
    }
}
