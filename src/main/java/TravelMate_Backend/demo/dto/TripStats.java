package TravelMate_Backend.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TripStats {
    private Long tripId;
    private String tripName;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDays;
    private String status;

    // Participantes
    private Integer totalParticipants;
    private List<TripDetailsResponse.ParticipantInfo> participantsList;

    // Gastos generales
    private BigDecimal totalSpent;
    private BigDecimal initialGeneralBudget;
    private BigDecimal currentGeneralBalance;
    private Double generalBudgetUsagePercent;

    // Gastos personales del usuario
    private BigDecimal userPersonalSpent;
    private BigDecimal userInitialPersonalBudget;
    private BigDecimal userCurrentPersonalBalance;
    private Double userPersonalBudgetUsagePercent;

    // Gastos por día
    private List<DailyExpense> dailyExpenses;

    // Días más gastados
    private List<DailyExpense> topExpensiveDays;

    // Gastos por categoría
    private List<CategoryExpense> expensesByCategory;

    // Gastos por participante
    private List<ParticipantExpense> expensesByParticipant;

    // Promedio diario
    private BigDecimal averageDailyExpense;

    private String currency;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyExpense {
        private LocalDate date;
        private Integer dayNumber;
        private BigDecimal totalExpense;
        private Integer expenseCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryExpense {
        private String category;
        private BigDecimal totalAmount;
        private Integer expenseCount;
        private Double percentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantExpense {
        private Long userId;
        private String userName;
        private BigDecimal totalSpent;
        private Integer expenseCount;
    }
}

