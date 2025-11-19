package TravelMate_Backend.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsResponse {
    // Estadísticas generales
    private Long totalTrips;
    private Long completedTrips;
    private Long planningTrips;
    private Long activeTrips;
    private Long totalDaysTraveled;
    
    // Estadísticas de gastos
    private BigDecimal totalSpent;
    private BigDecimal averageSpentPerTrip;
    private TripExpense mostExpensiveTrip;
    
    // Estadísticas de destinos
    private String mostTraveledLocation;
    private Long mostTraveledLocationCount;
    
    // Estadísticas mensuales
    private List<MonthlyTripStats> monthlyTrips;
    private List<MonthlyExpenseStats> monthlyExpenses;
    
    // Top viajes más costosos
    private List<TripExpense> topExpensiveTrips;
    
    // Estadísticas de participantes
    private Long totalParticipants;
    
    // ALTA PRIORIDAD - Nuevas estadísticas
    // 1. Distribución de gastos por moneda
    private List<CurrencyExpenseStats> expensesByCurrency;
    
    // 2. Distribución de países visitados
    private List<CountryVisitStats> countriesVisited;
    
    // 3. Estadísticas de Tips
    private TipStats tipStats;
    
    // 4. Gastos anuales
    private List<YearlyExpenseStats> yearlyExpenses;
    
    // 5. Duración de viajes
    private TripDurationStats tripDurationStats;
    
    // MEDIA PRIORIDAD - Nuevas estadísticas
    // 6. Distribución de modos de transporte
    private List<TransportModeStats> transportModeStats;
    
    // 7. Gastos generales vs individuales
    private GeneralVsIndividualExpenseStats generalVsIndividualExpenses;
    
    // 8. Top destinos extendido
    private List<DestinationVisitStats> topDestinations;
    
    // 9. Evolución temporal de gastos
    private List<TemporalExpenseStats> temporalExpenses;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripExpense {
        private Long tripId;
        private String tripName;
        private BigDecimal totalExpense;
        private String currency;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyTripStats {
        private String month; // "2024-01"
        private String monthName; // "Enero 2024"
        private Long tripCount;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyExpenseStats {
        private String month; // "2024-01"
        private String monthName; // "Enero 2024"
        private BigDecimal totalExpense;
        private String currency;
    }
    
    // ALTA PRIORIDAD - Clases de datos
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrencyExpenseStats {
        private String currency; // PESOS, DOLARES, EUROS
        private String currencyCode; // ARS, USD, EUR
        private String currencySymbol; // $, US$, €
        private BigDecimal totalExpense;
        private Long purchaseCount;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountryVisitStats {
        private String country;
        private Long visitCount;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TipStats {
        private Long totalTips;
        private List<TipTypeStats> distributionByType;
        private Double averageRating;
        private List<TipRatingByType> averageRatingByType;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TipTypeStats {
        private String tipType; // restaurant, lodging, attraction, gas_station
        private String tipIcon; // 🍽️, 🏨, 🎯, ⛽
        private Long count;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TipRatingByType {
        private String tipType;
        private String tipIcon;
        private Double averageRating;
        private Long count;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YearlyExpenseStats {
        private String year; // "2024"
        private BigDecimal totalExpense;
        private String currency;
        private Long tripCount;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripDurationStats {
        private Double averageDurationDays;
        private Long shortestTripDays;
        private Long longestTripDays;
        private List<DurationRangeStats> distributionByRange;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DurationRangeStats {
        private String range; // "1-3 días", "4-7 días", "8-14 días", "15+ días"
        private Long tripCount;
    }
    
    // MEDIA PRIORIDAD - Clases de datos
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransportModeStats {
        private String transportMode; // auto, avion
        private Long tripCount;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneralVsIndividualExpenseStats {
        private BigDecimal generalExpenses;
        private BigDecimal individualExpenses;
        private Long generalPurchaseCount;
        private Long individualPurchaseCount;
        private String currency;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DestinationVisitStats {
        private String destinationName;
        private String country;
        private Long visitCount;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemporalExpenseStats {
        private String period; // "2024-01", "2024-02", etc. (semanal o mensual)
        private String periodName; // "Enero 2024"
        private BigDecimal totalExpense;
        private String currency;
        private Long purchaseCount;
    }
}

