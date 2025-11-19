package TravelMate_Backend.demo.service;

import TravelMate_Backend.demo.dto.UserStatsResponse;
import TravelMate_Backend.demo.model.*;
import TravelMate_Backend.demo.repository.PurchaseRepository;
import TravelMate_Backend.demo.repository.TipRepository;
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

    @Autowired
    private TipRepository tipRepository;

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

        // ALTA PRIORIDAD - Nuevas estadísticas
        // 1. Distribución de gastos por moneda
        calculateExpensesByCurrency(userTrips, userId, stats);
        
        // 2. Distribución de países visitados
        calculateCountriesVisited(userTrips, stats);
        
        // 3. Estadísticas de Tips
        calculateTipStats(userTrips, stats);
        
        // 4. Gastos anuales
        calculateYearlyExpenses(userTrips, userId, stats);
        
        // 5. Duración de viajes
        calculateTripDurationStats(userTrips, stats);
        
        // MEDIA PRIORIDAD - Nuevas estadísticas
        // 6. Distribución de modos de transporte
        calculateTransportModeStats(userTrips, stats);
        
        // 7. Gastos generales vs individuales
        calculateGeneralVsIndividualExpenses(userTrips, userId, stats);
        
        // 8. Top destinos extendido
        calculateTopDestinations(userTrips, stats);
        
        // 9. Evolución temporal de gastos
        calculateTemporalExpenses(userTrips, userId, stats);

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

    // ALTA PRIORIDAD - Métodos de cálculo
    
    /**
     * 1. Distribución de gastos por moneda
     */
    private void calculateExpensesByCurrency(List<Trip> trips, Long userId, UserStatsResponse stats) {
        Map<TravelMate_Backend.demo.model.Currency, BigDecimal> expensesByCurrency = new HashMap<>();
        Map<TravelMate_Backend.demo.model.Currency, Long> purchaseCountByCurrency = new HashMap<>();
        
        for (Trip trip : trips) {
            List<Purchase> generalPurchases = purchaseRepository.findByTripIdAndIsGeneralTrue(trip.getId());
            List<Purchase> individualPurchases = purchaseRepository.findByTripIdAndUserIdAndIsGeneralFalse(trip.getId(), userId);
            
            List<Purchase> allPurchases = new ArrayList<>();
            allPurchases.addAll(generalPurchases);
            allPurchases.addAll(individualPurchases);
            
            for (Purchase purchase : allPurchases) {
                TravelMate_Backend.demo.model.Currency currency = purchase.getCurrency();
                if (currency != null) {
                    expensesByCurrency.put(currency, 
                        expensesByCurrency.getOrDefault(currency, BigDecimal.ZERO).add(purchase.getPrice()));
                    purchaseCountByCurrency.put(currency, 
                        purchaseCountByCurrency.getOrDefault(currency, 0L) + 1);
                }
            }
        }
        
        List<UserStatsResponse.CurrencyExpenseStats> currencyStats = expensesByCurrency.entrySet().stream()
                .map(entry -> {
                    TravelMate_Backend.demo.model.Currency currency = entry.getKey();
                    UserStatsResponse.CurrencyExpenseStats stat = new UserStatsResponse.CurrencyExpenseStats();
                    stat.setCurrency(currency.name());
                    stat.setCurrencyCode(currency.getCode());
                    stat.setCurrencySymbol(currency.getSymbol());
                    stat.setTotalExpense(entry.getValue());
                    stat.setPurchaseCount(purchaseCountByCurrency.getOrDefault(currency, 0L));
                    return stat;
                })
                .sorted((a, b) -> b.getTotalExpense().compareTo(a.getTotalExpense()))
                .collect(Collectors.toList());
        
        stats.setExpensesByCurrency(currencyStats);
    }
    
    /**
     * 2. Distribución de países visitados
     */
    private void calculateCountriesVisited(List<Trip> trips, UserStatsResponse stats) {
        Map<String, Long> countryCount = new HashMap<>();
        
        for (Trip trip : trips) {
            try {
                if (trip.getTripDestinations() != null && !trip.getTripDestinations().isEmpty()) {
                    for (TripDestination td : trip.getTripDestinations()) {
                        if (td.getDestination() != null && td.getDestination().getCountry() != null) {
                            String country = td.getDestination().getCountry();
                            countryCount.put(country, countryCount.getOrDefault(country, 0L) + 1);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Error al obtener países del viaje " + trip.getId() + ": " + e.getMessage());
            }
        }
        
        List<UserStatsResponse.CountryVisitStats> countries = countryCount.entrySet().stream()
                .map(entry -> {
                    UserStatsResponse.CountryVisitStats stat = new UserStatsResponse.CountryVisitStats();
                    stat.setCountry(entry.getKey());
                    stat.setVisitCount(entry.getValue());
                    return stat;
                })
                .sorted((a, b) -> b.getVisitCount().compareTo(a.getVisitCount()))
                .collect(Collectors.toList());
        
        stats.setCountriesVisited(countries);
    }
    
    /**
     * 3. Estadísticas de Tips
     */
    private void calculateTipStats(List<Trip> trips, UserStatsResponse stats) {
        List<Tip> allTips = new ArrayList<>();
        Map<String, Long> tipsByType = new HashMap<>();
        Map<String, List<Double>> ratingsByType = new HashMap<>();
        Map<String, String> iconByType = new HashMap<>();
        
        for (Trip trip : trips) {
            try {
                List<Tip> tripTips = tipRepository.findByTripIdOrderByCreatedAtDesc(trip.getId());
                allTips.addAll(tripTips);
                
                for (Tip tip : tripTips) {
                    String tipType = tip.getTipType();
                    if (tipType != null && !tipType.isEmpty()) {
                        tipsByType.put(tipType, tipsByType.getOrDefault(tipType, 0L) + 1);
                        
                        if (tip.getRating() != null) {
                            ratingsByType.computeIfAbsent(tipType, k -> new ArrayList<>()).add(tip.getRating());
                        }
                        
                        if (tip.getTipIcon() != null && !iconByType.containsKey(tipType)) {
                            iconByType.put(tipType, tip.getTipIcon());
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Error al obtener tips del viaje " + trip.getId() + ": " + e.getMessage());
            }
        }
        
        UserStatsResponse.TipStats tipStats = new UserStatsResponse.TipStats();
        tipStats.setTotalTips((long) allTips.size());
        
        // Distribución por tipo
        List<UserStatsResponse.TipTypeStats> distributionByType = tipsByType.entrySet().stream()
                .map(entry -> {
                    UserStatsResponse.TipTypeStats stat = new UserStatsResponse.TipTypeStats();
                    stat.setTipType(entry.getKey());
                    stat.setTipIcon(iconByType.getOrDefault(entry.getKey(), "📍"));
                    stat.setCount(entry.getValue());
                    return stat;
                })
                .sorted((a, b) -> b.getCount().compareTo(a.getCount()))
                .collect(Collectors.toList());
        tipStats.setDistributionByType(distributionByType);
        
        // Rating promedio general
        double totalRating = allTips.stream()
                .filter(t -> t.getRating() != null)
                .mapToDouble(Tip::getRating)
                .sum();
        long tipsWithRating = allTips.stream()
                .filter(t -> t.getRating() != null)
                .count();
        tipStats.setAverageRating(tipsWithRating > 0 ? totalRating / tipsWithRating : 0.0);
        
        // Rating promedio por tipo
        List<UserStatsResponse.TipRatingByType> averageRatingByType = ratingsByType.entrySet().stream()
                .map(entry -> {
                    String tipType = entry.getKey();
                    List<Double> ratings = entry.getValue();
                    double avg = ratings.stream().mapToDouble(Double::doubleValue).sum() / ratings.size();
                    
                    UserStatsResponse.TipRatingByType stat = new UserStatsResponse.TipRatingByType();
                    stat.setTipType(tipType);
                    stat.setTipIcon(iconByType.getOrDefault(tipType, "📍"));
                    stat.setAverageRating(avg);
                    stat.setCount((long) ratings.size());
                    return stat;
                })
                .sorted((a, b) -> b.getAverageRating().compareTo(a.getAverageRating()))
                .collect(Collectors.toList());
        tipStats.setAverageRatingByType(averageRatingByType);
        
        stats.setTipStats(tipStats);
    }
    
    /**
     * 4. Gastos anuales
     */
    private void calculateYearlyExpenses(List<Trip> trips, Long userId, UserStatsResponse stats) {
        Map<String, BigDecimal> yearlyExpense = new HashMap<>();
        Map<String, Long> yearlyTripCount = new HashMap<>();
        Map<String, TravelMate_Backend.demo.model.Currency> yearlyCurrency = new HashMap<>();
        
        DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("yyyy");
        
        for (Trip trip : trips) {
            if (trip.getDateI() != null) {
                String year = trip.getDateI().format(yearFormatter);
                
                yearlyTripCount.put(year, yearlyTripCount.getOrDefault(year, 0L) + 1);
                
                // Calcular gastos del año
                List<Purchase> generalPurchases = purchaseRepository.findByTripIdAndIsGeneralTrue(trip.getId());
                List<Purchase> individualPurchases = purchaseRepository.findByTripIdAndUserIdAndIsGeneralFalse(trip.getId(), userId);
                
                BigDecimal yearTotal = yearlyExpense.getOrDefault(year, BigDecimal.ZERO);
                TravelMate_Backend.demo.model.Currency currentYearCurrency = yearlyCurrency.getOrDefault(year, TravelMate_Backend.demo.model.Currency.PESOS);
                
                for (Purchase purchase : generalPurchases) {
                    yearTotal = yearTotal.add(purchase.getPrice());
                    if (purchase.getCurrency() != null) {
                        currentYearCurrency = purchase.getCurrency();
                    }
                }
                
                for (Purchase purchase : individualPurchases) {
                    yearTotal = yearTotal.add(purchase.getPrice());
                    if (purchase.getCurrency() != null) {
                        currentYearCurrency = purchase.getCurrency();
                    }
                }
                
                yearlyExpense.put(year, yearTotal);
                yearlyCurrency.put(year, currentYearCurrency);
            }
        }
        
        List<UserStatsResponse.YearlyExpenseStats> yearlyExpenses = yearlyExpense.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    String year = entry.getKey();
                    UserStatsResponse.YearlyExpenseStats stat = new UserStatsResponse.YearlyExpenseStats();
                    stat.setYear(year);
                    stat.setTotalExpense(entry.getValue());
                    stat.setCurrency(yearlyCurrency.getOrDefault(year, TravelMate_Backend.demo.model.Currency.PESOS).name());
                    stat.setTripCount(yearlyTripCount.getOrDefault(year, 0L));
                    return stat;
                })
                .collect(Collectors.toList());
        
        stats.setYearlyExpenses(yearlyExpenses);
    }
    
    /**
     * 5. Duración de viajes
     */
    private void calculateTripDurationStats(List<Trip> trips, UserStatsResponse stats) {
        List<Long> durations = new ArrayList<>();
        
        for (Trip trip : trips) {
            if (trip.getDateI() != null && trip.getDateF() != null) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(trip.getDateI(), trip.getDateF()) + 1;
                durations.add(days);
            }
        }
        
        if (durations.isEmpty()) {
            UserStatsResponse.TripDurationStats durationStats = new UserStatsResponse.TripDurationStats();
            durationStats.setAverageDurationDays(0.0);
            durationStats.setShortestTripDays(0L);
            durationStats.setLongestTripDays(0L);
            durationStats.setDistributionByRange(new ArrayList<>());
            stats.setTripDurationStats(durationStats);
            return;
        }
        
        UserStatsResponse.TripDurationStats durationStats = new UserStatsResponse.TripDurationStats();
        
        // Promedio
        double average = durations.stream().mapToLong(Long::longValue).average().orElse(0.0);
        durationStats.setAverageDurationDays(Math.round(average * 100.0) / 100.0);
        
        // Más corto y más largo
        durationStats.setShortestTripDays(Collections.min(durations));
        durationStats.setLongestTripDays(Collections.max(durations));
        
        // Distribución por rangos
        Map<String, Long> rangeCount = new HashMap<>();
        rangeCount.put("1-3 días", 0L);
        rangeCount.put("4-7 días", 0L);
        rangeCount.put("8-14 días", 0L);
        rangeCount.put("15+ días", 0L);
        
        for (Long days : durations) {
            if (days <= 3) {
                rangeCount.put("1-3 días", rangeCount.get("1-3 días") + 1);
            } else if (days <= 7) {
                rangeCount.put("4-7 días", rangeCount.get("4-7 días") + 1);
            } else if (days <= 14) {
                rangeCount.put("8-14 días", rangeCount.get("8-14 días") + 1);
            } else {
                rangeCount.put("15+ días", rangeCount.get("15+ días") + 1);
            }
        }
        
        List<UserStatsResponse.DurationRangeStats> distribution = rangeCount.entrySet().stream()
                .map(entry -> {
                    UserStatsResponse.DurationRangeStats stat = new UserStatsResponse.DurationRangeStats();
                    stat.setRange(entry.getKey());
                    stat.setTripCount(entry.getValue());
                    return stat;
                })
                .filter(s -> s.getTripCount() > 0) // Solo incluir rangos con viajes
                .collect(Collectors.toList());
        
        durationStats.setDistributionByRange(distribution);
        stats.setTripDurationStats(durationStats);
    }
    
    // MEDIA PRIORIDAD - Métodos de cálculo
    
    /**
     * 6. Distribución de modos de transporte
     */
    private void calculateTransportModeStats(List<Trip> trips, UserStatsResponse stats) {
        Map<String, Long> transportCount = new HashMap<>();
        
        for (Trip trip : trips) {
            try {
                if (trip.getTripDestinations() != null && !trip.getTripDestinations().isEmpty()) {
                    for (TripDestination td : trip.getTripDestinations()) {
                        String transportMode = td.getTransportMode();
                        if (transportMode != null && !transportMode.isEmpty()) {
                            transportCount.put(transportMode, transportCount.getOrDefault(transportMode, 0L) + 1);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Error al obtener modos de transporte del viaje " + trip.getId() + ": " + e.getMessage());
            }
        }
        
        List<UserStatsResponse.TransportModeStats> transportStats = transportCount.entrySet().stream()
                .map(entry -> {
                    UserStatsResponse.TransportModeStats stat = new UserStatsResponse.TransportModeStats();
                    stat.setTransportMode(entry.getKey());
                    stat.setTripCount(entry.getValue());
                    return stat;
                })
                .sorted((a, b) -> b.getTripCount().compareTo(a.getTripCount()))
                .collect(Collectors.toList());
        
        stats.setTransportModeStats(transportStats);
    }
    
    /**
     * 7. Gastos generales vs individuales
     */
    private void calculateGeneralVsIndividualExpenses(List<Trip> trips, Long userId, UserStatsResponse stats) {
        BigDecimal generalTotal = BigDecimal.ZERO;
        BigDecimal individualTotal = BigDecimal.ZERO;
        long generalCount = 0;
        long individualCount = 0;
        TravelMate_Backend.demo.model.Currency dominantCurrency = TravelMate_Backend.demo.model.Currency.PESOS;
        
        for (Trip trip : trips) {
            List<Purchase> generalPurchases = purchaseRepository.findByTripIdAndIsGeneralTrue(trip.getId());
            List<Purchase> individualPurchases = purchaseRepository.findByTripIdAndUserIdAndIsGeneralFalse(trip.getId(), userId);
            
            for (Purchase purchase : generalPurchases) {
                generalTotal = generalTotal.add(purchase.getPrice());
                generalCount++;
                if (purchase.getCurrency() != null) {
                    dominantCurrency = purchase.getCurrency();
                }
            }
            
            for (Purchase purchase : individualPurchases) {
                individualTotal = individualTotal.add(purchase.getPrice());
                individualCount++;
                if (purchase.getCurrency() != null) {
                    dominantCurrency = purchase.getCurrency();
                }
            }
        }
        
        UserStatsResponse.GeneralVsIndividualExpenseStats expenseStats = 
            new UserStatsResponse.GeneralVsIndividualExpenseStats();
        expenseStats.setGeneralExpenses(generalTotal);
        expenseStats.setIndividualExpenses(individualTotal);
        expenseStats.setGeneralPurchaseCount(generalCount);
        expenseStats.setIndividualPurchaseCount(individualCount);
        expenseStats.setCurrency(dominantCurrency.name());
        
        stats.setGeneralVsIndividualExpenses(expenseStats);
    }
    
    /**
     * 8. Top destinos extendido
     */
    private void calculateTopDestinations(List<Trip> trips, UserStatsResponse stats) {
        Map<String, DestinationInfo> destinationMap = new HashMap<>();
        
        for (Trip trip : trips) {
            try {
                if (trip.getTripDestinations() != null && !trip.getTripDestinations().isEmpty()) {
                    for (TripDestination td : trip.getTripDestinations()) {
                        String destinationName = null;
                        String country = null;
                        
                        if (td.getDestination() != null) {
                            destinationName = td.getDestination().getName();
                            country = td.getDestination().getCountry();
                        } else if (td.getDestinationAddress() != null) {
                            destinationName = td.getDestinationAddress();
                        }
                        
                        if (destinationName != null && !destinationName.isEmpty()) {
                            DestinationInfo info = destinationMap.getOrDefault(destinationName, 
                                new DestinationInfo(destinationName, country));
                            info.incrementCount();
                            destinationMap.put(destinationName, info);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Error al obtener destinos del viaje " + trip.getId() + ": " + e.getMessage());
            }
        }
        
        List<UserStatsResponse.DestinationVisitStats> topDestinations = destinationMap.values().stream()
                .map(info -> {
                    UserStatsResponse.DestinationVisitStats stat = new UserStatsResponse.DestinationVisitStats();
                    stat.setDestinationName(info.name);
                    stat.setCountry(info.country);
                    stat.setVisitCount(info.count);
                    return stat;
                })
                .sorted((a, b) -> b.getVisitCount().compareTo(a.getVisitCount()))
                .limit(10) // Top 10
                .collect(Collectors.toList());
        
        stats.setTopDestinations(topDestinations);
    }
    
    /**
     * 9. Evolución temporal de gastos
     */
    private void calculateTemporalExpenses(List<Trip> trips, Long userId, UserStatsResponse stats) {
        Map<String, BigDecimal> temporalExpense = new HashMap<>();
        Map<String, Long> temporalPurchaseCount = new HashMap<>();
        Map<String, TravelMate_Backend.demo.model.Currency> temporalCurrency = new HashMap<>();
        
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("es", "ES"));
        
        for (Trip trip : trips) {
            List<Purchase> generalPurchases = purchaseRepository.findByTripIdAndIsGeneralTrue(trip.getId());
            List<Purchase> individualPurchases = purchaseRepository.findByTripIdAndUserIdAndIsGeneralFalse(trip.getId(), userId);
            
            List<Purchase> allPurchases = new ArrayList<>();
            allPurchases.addAll(generalPurchases);
            allPurchases.addAll(individualPurchases);
            
            for (Purchase purchase : allPurchases) {
                if (purchase.getPurchaseDate() != null) {
                    String monthKey = purchase.getPurchaseDate().format(monthFormatter);
                    
                    BigDecimal monthTotal = temporalExpense.getOrDefault(monthKey, BigDecimal.ZERO);
                    monthTotal = monthTotal.add(purchase.getPrice());
                    temporalExpense.put(monthKey, monthTotal);
                    
                    temporalPurchaseCount.put(monthKey, 
                        temporalPurchaseCount.getOrDefault(monthKey, 0L) + 1);
                    
                    if (purchase.getCurrency() != null) {
                        temporalCurrency.put(monthKey, purchase.getCurrency());
                    }
                }
            }
        }
        
        List<UserStatsResponse.TemporalExpenseStats> temporalExpenses = temporalExpense.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    String monthKey = entry.getKey();
                    UserStatsResponse.TemporalExpenseStats stat = new UserStatsResponse.TemporalExpenseStats();
                    stat.setPeriod(monthKey);
                    try {
                        LocalDate date = LocalDate.parse(monthKey + "-01");
                        stat.setPeriodName(date.format(displayFormatter));
                    } catch (Exception e) {
                        stat.setPeriodName(monthKey);
                    }
                    stat.setTotalExpense(entry.getValue());
                    stat.setCurrency(temporalCurrency.getOrDefault(monthKey, TravelMate_Backend.demo.model.Currency.PESOS).name());
                    stat.setPurchaseCount(temporalPurchaseCount.getOrDefault(monthKey, 0L));
                    return stat;
                })
                .collect(Collectors.toList());
        
        stats.setTemporalExpenses(temporalExpenses);
    }
    
    // Clase auxiliar para destinos
    private static class DestinationInfo {
        String name;
        String country;
        Long count = 0L;
        
        DestinationInfo(String name, String country) {
            this.name = name;
            this.country = country;
        }
        
        void incrementCount() {
            count++;
        }
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
        
        // ALTA PRIORIDAD - Valores por defecto
        stats.setExpensesByCurrency(new ArrayList<>());
        stats.setCountriesVisited(new ArrayList<>());
        UserStatsResponse.TipStats emptyTipStats = new UserStatsResponse.TipStats();
        emptyTipStats.setTotalTips(0L);
        emptyTipStats.setDistributionByType(new ArrayList<>());
        emptyTipStats.setAverageRating(0.0);
        emptyTipStats.setAverageRatingByType(new ArrayList<>());
        stats.setTipStats(emptyTipStats);
        stats.setYearlyExpenses(new ArrayList<>());
        UserStatsResponse.TripDurationStats emptyDurationStats = new UserStatsResponse.TripDurationStats();
        emptyDurationStats.setAverageDurationDays(0.0);
        emptyDurationStats.setShortestTripDays(0L);
        emptyDurationStats.setLongestTripDays(0L);
        emptyDurationStats.setDistributionByRange(new ArrayList<>());
        stats.setTripDurationStats(emptyDurationStats);
        
        // MEDIA PRIORIDAD - Valores por defecto
        stats.setTransportModeStats(new ArrayList<>());
        UserStatsResponse.GeneralVsIndividualExpenseStats emptyGenVsInd = 
            new UserStatsResponse.GeneralVsIndividualExpenseStats();
        emptyGenVsInd.setGeneralExpenses(BigDecimal.ZERO);
        emptyGenVsInd.setIndividualExpenses(BigDecimal.ZERO);
        emptyGenVsInd.setGeneralPurchaseCount(0L);
        emptyGenVsInd.setIndividualPurchaseCount(0L);
        emptyGenVsInd.setCurrency(TravelMate_Backend.demo.model.Currency.PESOS.name());
        stats.setGeneralVsIndividualExpenses(emptyGenVsInd);
        stats.setTopDestinations(new ArrayList<>());
        stats.setTemporalExpenses(new ArrayList<>());
        
        return stats;
    }
}

