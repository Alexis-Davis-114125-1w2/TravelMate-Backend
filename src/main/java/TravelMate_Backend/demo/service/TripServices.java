package TravelMate_Backend.demo.service;

import TravelMate_Backend.demo.dto.TripCreate;
import TravelMate_Backend.demo.model.*;
import TravelMate_Backend.demo.repository.TripRepository;
import TravelMate_Backend.demo.repository.UserRepository;
import TravelMate_Backend.demo.repository.DestinationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TripServices {
    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private DestinationRepository destinationRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    public Trip createTrip(TripCreate tripDto, Long userId, MultipartFile imageFile) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Trip trip = new Trip();
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                trip.setImage(imageFile.getBytes());
            } catch (IOException e) {
                throw new RuntimeException("Error al procesar la imagen", e);
            }
        }
        MockTrip(tripDto, trip);

        Trip savedTrip = tripRepository.save(trip);
        
        // Crear la relación usuario-viaje usando SQL directo para evitar ConcurrentModificationException
        createUserTripRelation(userId, savedTrip.getId());
        
        // Crear TripDestination para origen y destino si están disponibles (después de guardar el trip)
        createTripDestinations(savedTrip, tripDto);

        trip.setStatus(determineStatus(trip));

        return trip;
    }

    public List<Trip> getUserTrips(Long userId) {
        // Verificar que el usuario existe
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Query directa a los trips del usuario
        List<Trip> trips = tripRepository.findByUsersId(userId);

        return trips.stream()
                .map(trip -> {
                    trip.setStatus(determineStatus(trip));
                    return trip;
                })
                .collect(Collectors.toList());
    }
    //TODO sin probar
    public void addUserToTrip(Long newUserId, Long tripId, Long currentUserId) {
        Trip trip = getTripById(tripId, currentUserId);

        User newUser = userRepository.findById(newUserId)
                .orElseThrow(() -> new RuntimeException("Usuario a agregar no encontrado"));

        boolean alreadyParticipates = trip.getUsers().stream()
                .anyMatch(user -> user.getId().equals(newUserId));

        if (alreadyParticipates) {
            throw new RuntimeException("El usuario ya participa en este viaje");
        }

        newUser.getTrips().add(trip);
        userRepository.save(newUser);
    }
    //TODO tampoco probe
    public void removeUserFromTrip(Long userToRemoveId, Long tripId, Long currentUserId) {
        Trip trip = getTripById(tripId, currentUserId);

        User userToRemove = userRepository.findById(userToRemoveId)
                .orElseThrow(() -> new RuntimeException("Usuario a remover no encontrado"));

        if (!currentUserId.equals(userToRemoveId)) {
            boolean currentUserParticipates = trip.getUsers().stream()
                    .anyMatch(user -> user.getId().equals(currentUserId));
            if (!currentUserParticipates) {
                throw new RuntimeException("No tienes permisos para remover usuarios de este viaje");
            }
        }

        userToRemove.getTrips().remove(trip);
        userRepository.save(userToRemove);

        tripRepository.flush();
        Trip refreshedTrip = tripRepository.findById(tripId).orElse(null);

        if (refreshedTrip != null && refreshedTrip.getUsers().isEmpty()) {
            tripRepository.delete(refreshedTrip);
        }
    }
    //TODO no Probe
    public Trip updateTrip(Long tripId, TripCreate tripDto, Long userId) {
        Trip trip = getTripById(tripId, userId);
        MockTrip(tripDto, trip);
        trip = tripRepository.save(trip);
        trip.setStatus(determineStatus(trip));
        return trip;
    }

    private void MockTrip(TripCreate tripDto, Trip trip) {
        trip.setName(tripDto.getName());
        trip.setDescription(tripDto.getDescription());
        trip.setDateI(tripDto.getDateI());
        trip.setDateF(tripDto.getDateF());
        trip.setCost(tripDto.getCost() != null ? tripDto.getCost() : BigDecimal.ZERO);
        //trip.setImage(tripDto.getImage());
    }

    public Trip getTripById(Long tripId, Long userId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Viaje no encontrado"));

        boolean userParticipates = tripRepository.existsByIdAndUsersId(tripId, userId);

        if (!userParticipates) {
            throw new RuntimeException("No tienes acceso a este viaje");
        }
        Set<User> users = userRepository.findByTripsId(tripId);
        trip.setUsers(users);

        return trip;
    }

    public void deleteTrip(Long tripId, Long userId) {
        Trip trip = getTripById(tripId, userId);

        List<User> usersToUpdate = new ArrayList<>(trip.getUsers());
        for (User user : usersToUpdate) {
            user.getTrips().remove(trip);
            userRepository.save(user);
        }

        tripRepository.delete(trip);
    }

    public List<User> getTripParticipants(Long tripId, Long userId) {
        Trip trip = getTripById(tripId, userId);
        return new ArrayList<>(trip.getUsers());
    }

    private String determineStatus(Trip trip) {
        LocalDate today = LocalDate.now();

        if (trip.getDateF().isBefore(today)) {
            return "completed";
        } else if (trip.getDateI().isAfter(today)) {
            return "planning";
        } else {
            return "active";
        }
    }
    
    private void createTripDestinations(Trip trip, TripCreate tripDto) {
        try {
            // Solo crear TripDestination si tenemos tanto origen como destino
            if (tripDto.getDestination() != null && !tripDto.getDestination().trim().isEmpty() &&
                tripDto.getOrigin() != null && !tripDto.getOrigin().trim().isEmpty()) {
                
                // Crear destino principal
                Destination destination = findOrCreateDestination(tripDto.getDestination());
                
                // Crear un solo TripDestination con origen y destino
                createTripDestinationWithOriginAndDestination(trip, destination, tripDto);
            } else {
                System.out.println("No se crean TripDestinations: faltan origen o destino");
            }
        } catch (Exception e) {
            System.err.println("Error creando TripDestinations: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private Destination findOrCreateDestination(String destinationName) {
        // Buscar destino existente
        Optional<Destination> existingDestination = destinationRepository.findByName(destinationName);
        
        if (existingDestination.isPresent()) {
            return existingDestination.get();
        }
        
        // Crear nuevo destino
        Destination newDestination = new Destination();
        newDestination.setName(destinationName);
        newDestination.setCountry(extractCountry(destinationName));
        newDestination.setCost(BigDecimal.ZERO);
        
        return destinationRepository.save(newDestination);
    }
    
    private void createTripDestinationWithOriginAndDestination(Trip trip, Destination destination, TripCreate tripDto) {
        TripDestination tripDestination = new TripDestination();
        
        // Crear ID compuesto
        TripDestinationId id = new TripDestinationId();
        id.setTripId(trip.getId());
        id.setDestinationId(destination.getId());
        tripDestination.setId(id);
        
        // Configurar relaciones
        tripDestination.setTrip(trip);
        tripDestination.setDestination(destination);
        
        // Configurar modo de transporte
        String transportMode = tripDto.getVehicle() != null ? tripDto.getVehicle() : "auto";
        tripDestination.setTransportMode(transportMode);
        
        // Configurar datos de origen (obligatorio para validación)
        if (tripDto.getOriginAddress() != null) {
            tripDestination.setOriginAddress(tripDto.getOriginAddress());
        } else {
            tripDestination.setOriginAddress(tripDto.getOrigin()); // Usar el nombre como fallback
        }
        
        if (tripDto.getOriginCoords() != null) {
            tripDestination.setOriginLatitude(new BigDecimal(tripDto.getOriginCoords().getLat()));
            tripDestination.setOriginLongitude(new BigDecimal(tripDto.getOriginCoords().getLng()));
        }
        
        // Configurar datos de destino (obligatorio para validación)
        if (tripDto.getDestinationAddress() != null) {
            tripDestination.setDestinationAddress(tripDto.getDestinationAddress());
        } else {
            tripDestination.setDestinationAddress(tripDto.getDestination()); // Usar el nombre como fallback
        }
        
        if (tripDto.getDestinationCoords() != null) {
            tripDestination.setDestinationLatitude(new BigDecimal(tripDto.getDestinationCoords().getLat()));
            tripDestination.setDestinationLongitude(new BigDecimal(tripDto.getDestinationCoords().getLng()));
        }
        
        // Guardar el TripDestination directamente en la base de datos
        try {
            entityManager.persist(tripDestination);
            entityManager.flush();
            System.out.println("TripDestination creado exitosamente con origen y destino");
        } catch (Exception e) {
            System.err.println("Error guardando TripDestination: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String extractCountry(String destinationName) {
        // Extraer país del nombre del destino (última parte después de la coma)
        String[] parts = destinationName.split(",");
        if (parts.length > 1) {
            return parts[parts.length - 1].trim();
        }
        return "Unknown";
    }
    
    private void createUserTripRelation(Long userId, Long tripId) {
        try {
            // Crear la relación usuario-viaje usando SQL directo
            String sql = "INSERT INTO users_trip (user_id, trip_id) VALUES (?, ?)";
            entityManager.createNativeQuery(sql)
                    .setParameter(1, userId)
                    .setParameter(2, tripId)
                    .executeUpdate();
            System.out.println("Relación usuario-viaje creada: userId=" + userId + ", tripId=" + tripId);
        } catch (Exception e) {
            System.err.println("Error creando relación usuario-viaje: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /*Estadisticas de viaje*/
    //TODO todavia no implementado
    /*public Map<String, Object> getTripStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Set<Trip> trips = user.getTrips();
        LocalDate today = LocalDate.now();

        long totalTrips = trips.size();
        long completedTrips = trips.stream()
                .filter(trip -> trip.getDateF().isBefore(today))
                .count();
        long planningTrips = trips.stream()
                .filter(trip -> trip.getDateI().isAfter(today))
                .count();
        long activeTrips = totalTrips - completedTrips - planningTrips;

        BigDecimal totalCost = trips.stream()
                .map(Trip::getCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalParticipants = trips.stream()
                .mapToLong(trip -> trip.getUsers().size())
                .sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTrips", totalTrips);
        stats.put("completedTrips", completedTrips);
        stats.put("planningTrips", planningTrips);
        stats.put("activeTrips", activeTrips);
        stats.put("totalCost", totalCost);
        stats.put("totalParticipants", totalParticipants);

        return stats;
    }*/
}
