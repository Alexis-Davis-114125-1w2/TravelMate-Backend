package TravelMate_Backend.demo.service;

import TravelMate_Backend.demo.dto.TripCreate;
import TravelMate_Backend.demo.dto.TripDetailsResponse;
import TravelMate_Backend.demo.model.*;
import TravelMate_Backend.demo.repository.TripRepository;
import TravelMate_Backend.demo.repository.UserRepository;
import TravelMate_Backend.demo.repository.DestinationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WalletService walletService;

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

        String joinCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        trip.setJoinCode(joinCode);
        trip.setCreateBy(userId);
        trip.setAdminIds(new HashSet<>(List.of(userId)));

        Trip savedTrip = tripRepository.save(trip);
        
        // Crear la relación usuario-viaje usando SQL directo para evitar ConcurrentModificationException
        createUserTripRelation(userId, savedTrip.getId());
        
        // Crear TripDestination para origen y destino si están disponibles (después de guardar el trip)
        createTripDestinations(savedTrip, tripDto);

        // Crear billeteras: general e individual del usuario creador
        BigDecimal generalAmount = tripDto.getCost() != null ? tripDto.getCost() : BigDecimal.ZERO;
        TravelMate_Backend.demo.model.Currency currency = tripDto.getCurrency() != null ? tripDto.getCurrency() : TravelMate_Backend.demo.model.Currency.PESOS;
        
        // Crear billetera general
        walletService.createGeneralWallet(savedTrip, generalAmount, currency);
        
        // Crear billetera individual del usuario creador
        walletService.createIndividualWallet(savedTrip, user, currency);

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

        // Crear billetera individual para el nuevo usuario
        // Obtener la moneda de la billetera general del viaje
        TravelMate_Backend.demo.model.Currency currency = walletService.getGeneralWallet(tripId).getCurrency();
        walletService.createIndividualWallet(trip, newUser, currency);
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

    public Trip updateTrip(Long tripId, TripCreate tripDto, Long userId) {
        Trip trip = getTripById(tripId, userId);
        MockTrip(tripDto, trip);
        trip = tripRepository.save(trip);
        trip.setStatus(determineStatus(trip));
        return trip;
    }

    @Transactional
    public Trip updateTripDates(Long tripId, TravelMate_Backend.demo.dto.TripDatesUpdateRequest request, Long userId) {
        Trip trip = getTripById(tripId, userId);
        
        // Verificar que el usuario es admin
        if (!trip.getAdminIds().contains(userId)) {
            throw new RuntimeException("Solo los administradores pueden actualizar el viaje");
        }
        
        trip.setDateI(request.getDateI());
        trip.setDateF(request.getDateF());
        trip.setStatus(determineStatus(trip));
        
        return tripRepository.save(trip);
    }

    @Transactional
    public Trip updateTripLocations(Long tripId, TravelMate_Backend.demo.dto.TripLocationUpdateRequest request, Long userId) {
        try {
            System.out.println("TripServices.updateTripLocations - Iniciando actualización para tripId: " + tripId);
            
            Trip trip = getTripById(tripId, userId);
            
            // Verificar que el usuario es admin
            if (!trip.getAdminIds().contains(userId)) {
                throw new RuntimeException("Solo los administradores pueden actualizar el viaje");
            }
            
            // Cargar TripDestinations existentes
            List<TripDestination> tripDestinations = loadTripDestinations(tripId);
            
            // Si hay TripDestinations, actualizar usando actualización directa en BD
            if (!tripDestinations.isEmpty()) {
                TripDestination existingTripDestination = tripDestinations.get(0);
                
                // Obtener el destination_id actual
                Long currentDestinationId = existingTripDestination.getId().getDestinationId();
                System.out.println("TripServices.updateTripLocations - Destination ID actual: " + currentDestinationId);
                
                // Actualizar destino si es diferente
                Destination destination = findOrCreateDestination(request.getDestination());
                System.out.println("TripServices.updateTripLocations - Nuevo Destination ID: " + destination.getId());
                
                // Si el destination_id cambió, necesitamos eliminar el antiguo y crear uno nuevo
                if (!currentDestinationId.equals(destination.getId())) {
                    System.out.println("TripServices.updateTripLocations - Destination ID cambió, eliminando registro antiguo");
                    // Eliminar el registro antiguo
                    int deleted = entityManager.createNativeQuery("DELETE FROM trip_destinations WHERE trip_id = ? AND destination_id = ?")
                            .setParameter(1, tripId)
                            .setParameter(2, currentDestinationId)
                            .executeUpdate();
                    System.out.println("TripServices.updateTripLocations - Registros eliminados: " + deleted);
                    
                    entityManager.flush();
                    
                    // Crear uno nuevo con el nuevo destination_id
                    System.out.println("TripServices.updateTripLocations - Creando nuevo registro");
                    createTripDestinationWithOriginAndDestination(trip, destination, 
                        createTripCreateFromLocationRequest(request));
                } else {
                    // Si el destination_id no cambió, actualizar directamente
                    System.out.println("TripServices.updateTripLocations - Actualizando registro existente");
                    
                    // Usar actualización SQL directa para evitar problemas con relaciones
                    String updateSql = "UPDATE trip_destinations SET " +
                            "origin_address = ?, " +
                            "origin_latitude = ?, " +
                            "origin_longitude = ?, " +
                            "destination_address = ?, " +
                            "destination_latitude = ?, " +
                            "destination_longitude = ?, " +
                            "transport_mode = ? " +
                            "WHERE trip_id = ? AND destination_id = ?";
                    
                    // Obtener el vehicle del request o del tripDestination existente
                    String vehicle = request.getVehicle() != null ? 
                            request.getVehicle() : 
                            (existingTripDestination.getTransportMode() != null ? existingTripDestination.getTransportMode() : "auto");
                    
                    System.out.println("TripServices.updateTripLocations - Ejecutando UPDATE con parámetros:");
                    System.out.println("  origin_address: " + (request.getOriginAddress() != null ? request.getOriginAddress() : request.getOrigin()));
                    System.out.println("  origin_lat: " + (request.getOriginCoords() != null ? request.getOriginCoords().getLat() : "null"));
                    System.out.println("  origin_lng: " + (request.getOriginCoords() != null ? request.getOriginCoords().getLng() : "null"));
                    System.out.println("  destination_address: " + (request.getDestinationAddress() != null ? request.getDestinationAddress() : request.getDestination()));
                    System.out.println("  destination_lat: " + (request.getDestinationCoords() != null ? request.getDestinationCoords().getLat() : "null"));
                    System.out.println("  destination_lng: " + (request.getDestinationCoords() != null ? request.getDestinationCoords().getLng() : "null"));
                    System.out.println("  vehicle: " + vehicle);
                    System.out.println("  trip_id: " + tripId);
                    System.out.println("  destination_id: " + currentDestinationId);
                    
                    int updated = entityManager.createNativeQuery(updateSql)
                            .setParameter(1, request.getOriginAddress() != null ? request.getOriginAddress() : request.getOrigin())
                            .setParameter(2, request.getOriginCoords() != null ? request.getOriginCoords().getLat() : null)
                            .setParameter(3, request.getOriginCoords() != null ? request.getOriginCoords().getLng() : null)
                            .setParameter(4, request.getDestinationAddress() != null ? request.getDestinationAddress() : request.getDestination())
                            .setParameter(5, request.getDestinationCoords() != null ? request.getDestinationCoords().getLat() : null)
                            .setParameter(6, request.getDestinationCoords() != null ? request.getDestinationCoords().getLng() : null)
                            .setParameter(7, vehicle)
                            .setParameter(8, tripId)
                            .setParameter(9, currentDestinationId)
                            .executeUpdate();
                    
                    System.out.println("TripServices.updateTripLocations - Registros actualizados: " + updated);
                    
                    if (updated == 0) {
                        throw new RuntimeException("No se pudo actualizar el registro. Posiblemente no existe o hubo un error.");
                    }
                    
                    entityManager.flush();
                }
            } else {
                // Si no hay TripDestinations, crear uno nuevo
                System.out.println("TripServices.updateTripLocations - No hay TripDestinations existentes, creando uno nuevo");
                Destination destination = findOrCreateDestination(request.getDestination());
                createTripDestinationWithOriginAndDestination(trip, destination, 
                    createTripCreateFromLocationRequest(request));
            }
            
            System.out.println("TripServices.updateTripLocations - Actualización completada exitosamente");
            return tripRepository.save(trip);
            
        } catch (Exception e) {
            System.err.println("TripServices.updateTripLocations - ERROR: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al actualizar las ubicaciones del viaje: " + e.getMessage(), e);
        }
    }

    private TripCreate createTripCreateFromLocationRequest(TravelMate_Backend.demo.dto.TripLocationUpdateRequest request) {
        TripCreate tripCreate = new TripCreate();
        tripCreate.setOrigin(request.getOrigin());
        tripCreate.setDestination(request.getDestination());
        tripCreate.setOriginAddress(request.getOriginAddress());
        tripCreate.setDestinationAddress(request.getDestinationAddress());
        tripCreate.setVehicle(request.getVehicle());
        
        if (request.getOriginCoords() != null) {
            TripCreate.Coords originCoords = new TripCreate.Coords();
            originCoords.setLat(request.getOriginCoords().getLat());
            originCoords.setLng(request.getOriginCoords().getLng());
            tripCreate.setOriginCoords(originCoords);
        }
        
        if (request.getDestinationCoords() != null) {
            TripCreate.Coords destinationCoords = new TripCreate.Coords();
            destinationCoords.setLat(request.getDestinationCoords().getLat());
            destinationCoords.setLng(request.getDestinationCoords().getLng());
            tripCreate.setDestinationCoords(destinationCoords);
        }
        
        return tripCreate;
    }

    private void MockTrip(TripCreate tripDto, Trip trip) {
        trip.setName(tripDto.getName());
        trip.setDescription(tripDto.getDescription());
        trip.setDateI(tripDto.getDateI());
        trip.setDateF(tripDto.getDateF());
        trip.setCost(tripDto.getCost() != null ? tripDto.getCost() : BigDecimal.ZERO);
        //trip.setImage(tripDto.getImage());

    }
    //Todo resolver a futuro
    public Trip getTripById(Long tripId, Long userId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Viaje no encontrado"));

        // Verificar acceso sin tocar la relación
        boolean userParticipates = tripRepository.existsByIdAndUsersId(tripId, userId);

        if (!userParticipates) {
            throw new RuntimeException("No tienes acceso a este viaje");
        }

        // No tocar trip.getUsers(), ni limpiar ni agregar nada
        return trip;
    }


    public TripDetailsResponse getTripDetails(Long tripId, Long userId) {
        System.out.println("TripServices.getTripDetails - Iniciando para tripId: " + tripId + ", userId: " + userId);
        Trip trip = getTripById(tripId, userId);
        
        // Crear la respuesta con los datos básicos del viaje
        TripDetailsResponse response = new TripDetailsResponse();
        response.setId(trip.getId());
        response.setName(trip.getName());
        response.setDescription(trip.getDescription());
        response.setDateI(trip.getDateI());
        response.setDateF(trip.getDateF());
        response.setCost(trip.getCost());
        response.setJoinCode(trip.getJoinCode());
        response.setStatus(determineStatus(trip));
        response.setCreateBy(trip.getCreateBy());
        response.setAdminIds(trip.getAdminIds());
        
        // Cargar información de TripDestination de forma segura
        try {
            List<TripDestination> tripDestinations = loadTripDestinations(tripId);
            if (!tripDestinations.isEmpty()) {
                TripDestination tripDestination = tripDestinations.get(0); // Tomar el primero
                
                // Configurar información de transporte
                response.setTransportMode(tripDestination.getTransportMode());
                response.setVehicle(tripDestination.getTransportMode());
                
                // Configurar origen
                if (tripDestination.getOriginAddress() != null) {
                    response.setOrigin(tripDestination.getOriginAddress());
                    response.setOriginAddress(tripDestination.getOriginAddress());
                }
                if (tripDestination.getOriginLatitude() != null) {
                    response.setOriginLatitude(tripDestination.getOriginLatitude().doubleValue());
                }
                if (tripDestination.getOriginLongitude() != null) {
                    response.setOriginLongitude(tripDestination.getOriginLongitude().doubleValue());
                }
                
                // Configurar destino
                if (tripDestination.getDestination() != null) {
                    response.setDestination(tripDestination.getDestination().getName());
                }
                if (tripDestination.getDestinationAddress() != null) {
                    response.setDestinationAddress(tripDestination.getDestinationAddress());
                }
                if (tripDestination.getDestinationLatitude() != null) {
                    response.setDestinationLatitude(tripDestination.getDestinationLatitude().doubleValue());
                }
                if (tripDestination.getDestinationLongitude() != null) {
                    response.setDestinationLongitude(tripDestination.getDestinationLongitude().doubleValue());
                }
                
                System.out.println("TripServices.getTripDetails - Datos de TripDestination cargados:");
                System.out.println("  - Origen: " + response.getOrigin());
                System.out.println("  - Destino: " + response.getDestination());
                System.out.println("  - Transporte: " + response.getTransportMode());
                System.out.println("  - Coordenadas origen: " + response.getOriginLatitude() + ", " + response.getOriginLongitude());
                System.out.println("  - Coordenadas destino: " + response.getDestinationLatitude() + ", " + response.getDestinationLongitude());
            } else {
                System.out.println("TripServices.getTripDetails - No se encontraron TripDestinations para el viaje");
            }
        } catch (Exception e) {
            System.err.println("TripServices.getTripDetails - Error cargando TripDestinations: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Obtener participantes
        List<User> participants = userRepository.findByTripsId(tripId);
        List<TripDetailsResponse.ParticipantInfo> participantInfos = participants.stream()
                .map(user -> new TripDetailsResponse.ParticipantInfo(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        null // No hay profilePicture en el modelo actual
                ))
                .collect(Collectors.toList());
        response.setParticipants(participantInfos);
        
        return response;
    }
    @Transactional
    public void deleteTrip(Long tripId, Long userId) {
        try {
            // Verificar acceso del usuario
            if (!userHasAccess(tripId, userId)) {
                throw new RuntimeException("No tienes acceso a este viaje");
            }

            // 1️⃣ Borrar relaciones en users_trip
            jdbcTemplate.update("DELETE FROM users_trip WHERE trip_id = ?", tripId);

            // 2️⃣ Borrar relaciones con destinos, si existen
            jdbcTemplate.update("DELETE FROM trip_destinations WHERE trip_id = ?", tripId);

            // 3️⃣ Borrar el viaje en sí
            jdbcTemplate.update("DELETE FROM trips WHERE id = ?", tripId);

            System.out.println("✅ Viaje eliminado correctamente (SQL directo): " + tripId);
        } catch (Exception e) {
            System.err.println("❌ Error eliminando trip: " + e.getMessage());
            e.printStackTrace();
        }

    }

    private boolean userHasAccess(Long tripId, Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users_trip WHERE trip_id = ? AND user_id = ?",
                Integer.class,
                tripId, userId
        );
        return count != null && count > 0;
    }

    public List<User> getTripParticipants(Long tripId, Long userId) {
        tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Viaje no encontrado"));

        boolean userParticipates = tripRepository.existsByIdAndUsersId(tripId, userId);

        if (!userParticipates) {
            throw new RuntimeException("No tienes acceso a este viaje");
        }
        List<User> users = userRepository.findByTripsId(tripId);


        return users;
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
    
    private List<TripDestination> loadTripDestinations(Long tripId) {
        try {
            // Usar consulta más específica para evitar problemas
            String sql = "SELECT origin_address, origin_latitude, origin_longitude, " +
                        "destination_address, destination_latitude, destination_longitude, " +
                        "transport_mode, destination_id FROM trip_destinations WHERE trip_id = ?";
            List<Object[]> results = entityManager.createNativeQuery(sql)
                    .setParameter(1, tripId)
                    .getResultList();
            
            System.out.println("TripServices.loadTripDestinations - Consulta ejecutada para tripId: " + tripId);
            System.out.println("TripServices.loadTripDestinations - Resultados encontrados: " + results.size());
            
            List<TripDestination> tripDestinations = new ArrayList<>();
            
            for (Object[] row : results) {
                System.out.println("TripServices.loadTripDestinations - Procesando fila: " + java.util.Arrays.toString(row));
                
                TripDestination tripDestination = new TripDestination();
                
                // Crear ID compuesto
                TripDestinationId id = new TripDestinationId();
                id.setTripId(tripId);
                id.setDestinationId(((Number) row[7]).longValue()); // destination_id
                tripDestination.setId(id);
                
                // Mapear campos según la nueva consulta:
                // 0: origin_address, 1: origin_latitude, 2: origin_longitude,
                // 3: destination_address, 4: destination_latitude, 5: destination_longitude,
                // 6: transport_mode, 7: destination_id
                
                if (row[0] != null) { // origin_address
                    tripDestination.setOriginAddress((String) row[0]);
                    System.out.println("TripServices.loadTripDestinations - Origin address: " + row[0]);
                }
                if (row[1] != null) { // origin_latitude
                    tripDestination.setOriginLatitude(new BigDecimal(row[1].toString()));
                    System.out.println("TripServices.loadTripDestinations - Origin latitude: " + row[1]);
                }
                if (row[2] != null) { // origin_longitude
                    tripDestination.setOriginLongitude(new BigDecimal(row[2].toString()));
                    System.out.println("TripServices.loadTripDestinations - Origin longitude: " + row[2]);
                }
                if (row[3] != null) { // destination_address
                    tripDestination.setDestinationAddress((String) row[3]);
                    System.out.println("TripServices.loadTripDestinations - Destination address: " + row[3]);
                }
                if (row[4] != null) { // destination_latitude
                    tripDestination.setDestinationLatitude(new BigDecimal(row[4].toString()));
                    System.out.println("TripServices.loadTripDestinations - Destination latitude: " + row[4]);
                }
                if (row[5] != null) { // destination_longitude
                    tripDestination.setDestinationLongitude(new BigDecimal(row[5].toString()));
                    System.out.println("TripServices.loadTripDestinations - Destination longitude: " + row[5]);
                }
                if (row[6] != null) { // transport_mode
                    tripDestination.setTransportMode((String) row[6]);
                    System.out.println("TripServices.loadTripDestinations - Transport mode: " + row[6]);
                }
                
                // Cargar el destino relacionado
                Long destinationId = ((Number) row[7]).longValue();
                Destination destination = destinationRepository.findById(destinationId).orElse(null);
                if (destination != null) {
                    tripDestination.setDestination(destination);
                    System.out.println("TripServices.loadTripDestinations - Destination name: " + destination.getName());
                }
                
                tripDestinations.add(tripDestination);
            }
            
            System.out.println("TripServices.loadTripDestinations - Cargados " + tripDestinations.size() + " TripDestinations");
            return tripDestinations;
            
        } catch (Exception e) {
            System.err.println("TripServices.loadTripDestinations - Error: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
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

    @Transactional
    public void joinTripByCode(String code, Long userId) {
        Trip trip = tripRepository.findByJoinCode(code)
                .orElseThrow(() -> new RuntimeException("Código inválido o viaje no encontrado"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        boolean exists = entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM users_trip WHERE user_id = ? AND trip_id = ?")
                .setParameter(1, userId)
                .setParameter(2, trip.getId())
                .getSingleResult() != null &&
                ((Number) entityManager.createNativeQuery(
                                "SELECT COUNT(*) FROM users_trip WHERE user_id = ? AND trip_id = ?")
                        .setParameter(1, userId)
                        .setParameter(2, trip.getId())
                        .getSingleResult()).intValue() > 0;

        if (exists) {
            throw new RuntimeException("El usuario ya está en este viaje");
        }

        createUserTripRelation(userId, trip.getId());
        
        // Crear billetera individual para el nuevo usuario
        // Obtener la moneda de la billetera general del viaje
        TravelMate_Backend.demo.model.Currency currency = walletService.getGeneralWallet(trip.getId()).getCurrency();
        walletService.createIndividualWallet(trip, user, currency);
        
        System.out.println("usuario guardado");
    }

    public Trip addAdminId(Long userId, Long adminId, Long tripId) {
        Trip trip = getTripById(tripId, userId);
        trip.getAdminIds().add(adminId);
        return tripRepository.save(trip);
    }

    public ResponseEntity<?> removeAdminId(Long userId, Long adminId, Long tripId) {
        Trip trip = getTripById(tripId, userId);
        if (trip.getAdminIds().size()<=1) {
            return ResponseEntity.badRequest().body("No se puede eliminar el admin, porque no hay uno solo");
        }
        trip.getAdminIds().remove(adminId);
        Trip trip1 = tripRepository.save(trip);
        return ResponseEntity.ok(trip1);
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
