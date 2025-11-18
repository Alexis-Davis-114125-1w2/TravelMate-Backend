package TravelMate_Backend.demo.service;

import TravelMate_Backend.demo.dto.UserStatsResponse;
import TravelMate_Backend.demo.model.*;
import TravelMate_Backend.demo.repository.PurchaseRepository;
import TravelMate_Backend.demo.repository.TripRepository;
import TravelMate_Backend.demo.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class StatsService {

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public UserStatsResponse getUserStats(Long userId) {
        // Verificar que el usuario existe
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Obtener todos los viajes del usuario
        List<Trip> userTrips = tripRepository.findByUsersId(userId);

        if (userTrips.isEmpty()) {
            return createEmptyStats();
        }

        // Inicializar relaciones lazy para evitar problemas
        for (Trip trip : userTrips) {
            try {
                Hibernate.initialize(trip.getTripDestinations());
                if (trip.getTripDestinations() != null) {
                    for (TripDestination td : trip.getTripDestinations()) {
                        Hibernate.initialize(td.getDestination());
                    }
                }
            } catch (Exception e) {
                System.out.println("Error al inicializar relaciones del viaje " + trip.getId() + ": " + e.getMessage());
            }
        }

        UserStatsResponse stats = new UserStatsResponse();

        // Estadísticas generales de viajes
        stats.setTotalTrips((long) userTrips.size());
        stats.setCompletedTrips(userTrips.stream()
                .filter(t -> "completed".equalsIgnoreCase(determineStatus(t)))
                .count());
        stats.setPlanningTrips(userTrips.stream()
                .filter(t -> "planning".equalsIgnoreCase(determineStatus(t)))
                .count());
        stats.setActiveTrips(userTrips.stream()
                .filter(t -> "active".equalsIgnoreCase(determineStatus(t)))
                .count());

        // Calcular días totales viajados
        long totalDays = userTrips.stream()
                .mapToLong(trip -> {
                    if (trip.getDateI() != null && trip.getDateF() != null) {
                        return java.time.temporal.ChronoUnit.DAYS.between(trip.getDateI(), trip.getDateF()) + 1;
                    }
                    return 0;
                })
                .sum();
        stats.setTotalDaysTraveled(totalDays);

        // Estadísticas de gastos
        calculateExpenseStats(userTrips, userId, stats);

        // Estadísticas de destinos
        calculateDestinationStats(userTrips, stats);

        // Estadísticas mensuales
        calculateMonthlyStats(userTrips, userId, stats);

        // Top viajes más costosos
        calculateTopExpensiveTrips(userTrips, userId, stats);

        // Total de participantes (suma de todos los participantes de todos los viajes)
        stats.setTotalParticipants(userTrips.stream()
                .mapToLong(trip -> {
                    try {
                        return trip.getUsers() != null ? trip.getUsers().size() : 0;
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .sum());

        return stats;
    }

    private void calculateExpenseStats(List<Trip> trips, Long userId, UserStatsResponse stats) {
        BigDecimal totalSpent = BigDecimal.ZERO;
        Map<Long, BigDecimal> tripExpenses = new HashMap<>();
        Map<Long, String> tripNames = new HashMap<>();
        Map<Long, TravelMate_Backend.demo.model.Currency> tripCurrencies = new HashMap<>();

        for (Trip trip : trips) {
            // Obtener todas las compras del viaje (generales e individuales del usuario)
            List<Purchase> generalPurchases = purchaseRepository.findByTripIdAndIsGeneralTrue(trip.getId());
            List<Purchase> individualPurchases = purchaseRepository.findByTripIdAndUserIdAndIsGeneralFalse(trip.getId(), userId);

            BigDecimal tripTotal = BigDecimal.ZERO;
            TravelMate_Backend.demo.model.Currency tripCurrency = TravelMate_Backend.demo.model.Currency.PESOS; // Default

            // Sumar compras generales
            for (Purchase purchase : generalPurchases) {
                tripTotal = tripTotal.add(purchase.getPrice());
                if (purchase.getCurrency() != null) {
                    tripCurrency = purchase.getCurrency();
                }
            }

            // Sumar compras individuales del usuario
            for (Purchase purchase : individualPurchases) {
                tripTotal = tripTotal.add(purchase.getPrice());
                if (purchase.getCurrency() != null) {
                    tripCurrency = purchase.getCurrency();
                }
            }

            tripExpenses.put(trip.getId(), tripTotal);
            tripNames.put(trip.getId(), trip.getName());
            tripCurrencies.put(trip.getId(), tripCurrency);
            totalSpent = totalSpent.add(tripTotal);
        }

        stats.setTotalSpent(totalSpent);
        
        if (trips.size() > 0) {
            stats.setAverageSpentPerTrip(totalSpent.divide(BigDecimal.valueOf(trips.size()), 2, RoundingMode.HALF_UP));
        } else {
            stats.setAverageSpentPerTrip(BigDecimal.ZERO);
        }

        // Encontrar el viaje más costoso
        Optional<Map.Entry<Long, BigDecimal>> mostExpensive = tripExpenses.entrySet().stream()
                .max(Map.Entry.comparingByValue());

        if (mostExpensive.isPresent()) {
            Long tripId = mostExpensive.get().getKey();
            UserStatsResponse.TripExpense tripExpense = new UserStatsResponse.TripExpense();
            tripExpense.setTripId(tripId);
            tripExpense.setTripName(tripNames.get(tripId));
            tripExpense.setTotalExpense(mostExpensive.get().getValue());
            tripExpense.setCurrency(tripCurrencies.get(tripId) != null ? tripCurrencies.get(tripId).name() : "PESOS");
            stats.setMostExpensiveTrip(tripExpense);
        }
    }

    private void calculateDestinationStats(List<Trip> trips, UserStatsResponse stats) {
        Map<String, Long> destinationCount = new HashMap<>();

        for (Trip trip : trips) {
            String destination = null;
            
            // Intentar obtener el destino desde TripDestination
            try {
                if (trip.getTripDestinations() != null && !trip.getTripDestinations().isEmpty()) {
                    // Obtener el primer destino (puede haber múltiples)
                    TripDestination tripDestination = trip.getTripDestinations().iterator().next();
                    if (tripDestination != null && tripDestination.getDestination() != null) {
                        destination = tripDestination.getDestination().getName();
                    } else if (tripDestination != null && tripDestination.getDestinationAddress() != null) {
                        destination = tripDestination.getDestinationAddress();
                    }
                }
            } catch (Exception e) {
                // Si hay error al acceder a la relación, usar el nombre del viaje
                System.out.println("Error al obtener destino del viaje " + trip.getId() + ": " + e.getMessage());
            }
            
            // Fallback al nombre del viaje si no se encontró destino
            if (destination == null || destination.isEmpty()) {
                destination = trip.getName();
            }
            
            if (destination != null && !destination.isEmpty()) {
                destinationCount.put(destination, destinationCount.getOrDefault(destination, 0L) + 1);
            }
        }

        if (!destinationCount.isEmpty()) {
            Map.Entry<String, Long> mostTraveled = destinationCount.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);

            if (mostTraveled != null) {
                stats.setMostTraveledLocation(mostTraveled.getKey());
                stats.setMostTraveledLocationCount(mostTraveled.getValue());
            }
        }
    }

    private void calculateMonthlyStats(List<Trip> trips, Long userId, UserStatsResponse stats) {
        Map<String, Long> monthlyTripCount = new HashMap<>();
        Map<String, BigDecimal> monthlyExpense = new HashMap<>();
        Map<String, TravelMate_Backend.demo.model.Currency> monthlyCurrency = new HashMap<>();

        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("es", "ES"));

        for (Trip trip : trips) {
            if (trip.getDateI() != null) {
                String monthKey = trip.getDateI().format(monthFormatter);
                String monthDisplay = trip.getDateI().format(displayFormatter);
                
                monthlyTripCount.put(monthKey, monthlyTripCount.getOrDefault(monthKey, 0L) + 1);

                // Calcular gastos del mes
                List<Purchase> generalPurchases = purchaseRepository.findByTripIdAndIsGeneralTrue(trip.getId());
                List<Purchase> individualPurchases = purchaseRepository.findByTripIdAndUserIdAndIsGeneralFalse(trip.getId(), userId);

                BigDecimal monthTotal = monthlyExpense.getOrDefault(monthKey, BigDecimal.ZERO);
                TravelMate_Backend.demo.model.Currency currentMonthCurrency = monthlyCurrency.getOrDefault(monthKey, TravelMate_Backend.demo.model.Currency.PESOS);

                for (Purchase purchase : generalPurchases) {
                    monthTotal = monthTotal.add(purchase.getPrice());
                    if (purchase.getCurrency() != null) {
                        currentMonthCurrency = purchase.getCurrency();
                    }
                }

                for (Purchase purchase : individualPurchases) {
                    monthTotal = monthTotal.add(purchase.getPrice());
                    if (purchase.getCurrency() != null) {
                        currentMonthCurrency = purchase.getCurrency();
                    }
                }

                monthlyExpense.put(monthKey, monthTotal);
                monthlyCurrency.put(monthKey, currentMonthCurrency);
            }
        }

        // Convertir a listas ordenadas
        List<UserStatsResponse.MonthlyTripStats> monthlyTrips = monthlyTripCount.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    UserStatsResponse.MonthlyTripStats monthly = new UserStatsResponse.MonthlyTripStats();
                    monthly.setMonth(entry.getKey());
                    try {
                        LocalDate date = LocalDate.parse(entry.getKey() + "-01");
                        monthly.setMonthName(date.format(displayFormatter));
                    } catch (Exception e) {
                        monthly.setMonthName(entry.getKey());
                    }
                    monthly.setTripCount(entry.getValue());
                    return monthly;
                })
                .collect(Collectors.toList());

        List<UserStatsResponse.MonthlyExpenseStats> monthlyExpenses = monthlyExpense.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    UserStatsResponse.MonthlyExpenseStats monthly = new UserStatsResponse.MonthlyExpenseStats();
                    monthly.setMonth(entry.getKey());
                    try {
                        LocalDate date = LocalDate.parse(entry.getKey() + "-01");
                        monthly.setMonthName(date.format(displayFormatter));
                    } catch (Exception e) {
                        monthly.setMonthName(entry.getKey());
                    }
                    monthly.setTotalExpense(entry.getValue());
                    monthly.setCurrency(monthlyCurrency.getOrDefault(entry.getKey(), TravelMate_Backend.demo.model.Currency.PESOS).name());
                    return monthly;
                })
                .collect(Collectors.toList());

        stats.setMonthlyTrips(monthlyTrips);
        stats.setMonthlyExpenses(monthlyExpenses);
    }

    private void calculateTopExpensiveTrips(List<Trip> trips, Long userId, UserStatsResponse stats) {
        List<UserStatsResponse.TripExpense> tripExpenses = new ArrayList<>();

        for (Trip trip : trips) {
            List<Purchase> generalPurchases = purchaseRepository.findByTripIdAndIsGeneralTrue(trip.getId());
            List<Purchase> individualPurchases = purchaseRepository.findByTripIdAndUserIdAndIsGeneralFalse(trip.getId(), userId);

            BigDecimal tripTotal = BigDecimal.ZERO;
            TravelMate_Backend.demo.model.Currency tripCurrency = TravelMate_Backend.demo.model.Currency.PESOS;

            for (Purchase purchase : generalPurchases) {
                tripTotal = tripTotal.add(purchase.getPrice());
                if (purchase.getCurrency() != null) {
                    tripCurrency = purchase.getCurrency();
                }
            }

            for (Purchase purchase : individualPurchases) {
                tripTotal = tripTotal.add(purchase.getPrice());
                if (purchase.getCurrency() != null) {
                    tripCurrency = purchase.getCurrency();
                }
            }

            if (tripTotal.compareTo(BigDecimal.ZERO) > 0) {
                UserStatsResponse.TripExpense tripExpense = new UserStatsResponse.TripExpense();
                tripExpense.setTripId(trip.getId());
                tripExpense.setTripName(trip.getName());
                tripExpense.setTotalExpense(tripTotal);
                tripExpense.setCurrency(tripCurrency.name());
                tripExpenses.add(tripExpense);
            }
        }

        // Ordenar por gasto descendente y tomar top 10
        tripExpenses.sort((a, b) -> b.getTotalExpense().compareTo(a.getTotalExpense()));
        stats.setTopExpensiveTrips(tripExpenses.stream().limit(10).collect(Collectors.toList()));
    }

    private String determineStatus(Trip trip) {
        if (trip.getDateI() == null || trip.getDateF() == null) {
            return "planning";
        }

        LocalDate today = LocalDate.now();
        if (today.isBefore(trip.getDateI())) {
            return "planning";
        } else if (today.isAfter(trip.getDateF())) {
            return "completed";
        } else {
            return "active";
        }
    }

    private UserStatsResponse createEmptyStats() {
        UserStatsResponse stats = new UserStatsResponse();
        stats.setTotalTrips(0L);
        stats.setCompletedTrips(0L);
        stats.setPlanningTrips(0L);
        stats.setActiveTrips(0L);
        stats.setTotalDaysTraveled(0L);
        stats.setTotalSpent(BigDecimal.ZERO);
        stats.setAverageSpentPerTrip(BigDecimal.ZERO);
        stats.setTotalParticipants(0L);
        stats.setMonthlyTrips(new ArrayList<>());
        stats.setMonthlyExpenses(new ArrayList<>());
        stats.setTopExpensiveTrips(new ArrayList<>());
        return stats;
    }
}

