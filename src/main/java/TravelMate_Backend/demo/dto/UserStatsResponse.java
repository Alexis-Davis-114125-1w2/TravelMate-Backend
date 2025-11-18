package TravelMate_Backend.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
}

