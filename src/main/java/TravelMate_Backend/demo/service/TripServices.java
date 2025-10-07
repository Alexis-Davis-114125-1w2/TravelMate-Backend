package TravelMate_Backend.demo.service;

import TravelMate_Backend.demo.dto.TripCreate;
import TravelMate_Backend.demo.model.Trip;
import TravelMate_Backend.demo.model.User;
import TravelMate_Backend.demo.repository.TripRepository;
import TravelMate_Backend.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
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

        trip = tripRepository.save(trip);

        trip.getUsers().add(user);
        user.getTrips().add(trip);
        tripRepository.save(trip);
        userRepository.save(user);

        trip.setStatus(determineStatus(trip));

        return trip;
    }

    public List<Trip> getUserTrips(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return user.getTrips().stream().peek(trip -> trip.setStatus(determineStatus(trip)))
                .collect(Collectors.toList());
    }

    public void addUserToTrip(Long newUserId, Long tripId, Long currentUserId) {
        Trip trip = getTripById(tripId, currentUserId);

        User newUser = userRepository.findById(newUserId)
                .orElseThrow(() -> new RuntimeException("Usuario a agregar no encontrado"));

        boolean alreadyParticipates = trip.getUsers().stream()
                .anyMatch(user -> user.getId().equals(newUserId));

        if (alreadyParticipates) {
            throw new RuntimeException("El usuario ya participa en este viaje");
        }


        trip.getUsers().add(newUser);
        newUser.getTrips().add(trip);
        userRepository.save(newUser);
    }

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

        trip.getUsers().remove(userToRemove);
        userToRemove.getTrips().remove(trip);
        userRepository.save(userToRemove);

        if (trip.getUsers().isEmpty()) {
            tripRepository.delete(trip);
        }
    }

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

        boolean userParticipates = trip.getUsers().stream()
                .anyMatch(user -> user.getId().equals(userId));

        if (!userParticipates) {
            throw new RuntimeException("No tienes acceso a este viaje");
        }

        return trip;
    }

    public void deleteTrip(Long tripId, Long userId) {
        Trip trip = getTripById(tripId, userId);

        // Remover todas las relaciones con usuarios
        trip.getUsers().forEach(user -> user.getTrips().remove(trip));
        trip.getUsers().clear();

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
