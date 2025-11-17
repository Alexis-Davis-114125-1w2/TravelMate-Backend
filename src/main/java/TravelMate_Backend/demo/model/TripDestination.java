package TravelMate_Backend.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "trip_destinations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripDestination{

    @EmbeddedId
    private TripDestinationId id;

    @ManyToOne
    @MapsId("tripId")
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne
    @MapsId("destinationId")
    @JoinColumn(name = "destination_id", nullable = false)
    private Destination destination;

    @Column(name = "transport_mode", nullable = false, length = 10)
    private String transportMode; // 'auto' o 'avion'

    // Campos para transporte en auto (Google Distance Matrix)
    @Column(name = "origin_address", columnDefinition = "TEXT")
    private String originAddress;

    @Column(name = "origin_latitude", precision = 9, scale = 6)
    private BigDecimal originLatitude;

    @Column(name = "origin_longitude", precision = 9, scale = 6)
    private BigDecimal originLongitude;

    @Column(name = "destination_address", columnDefinition = "TEXT")
    private String destinationAddress;

    @Column(name = "destination_latitude", precision = 9, scale = 6)
    private BigDecimal destinationLatitude;

    @Column(name = "destination_longitude", precision = 9, scale = 6)
    private BigDecimal destinationLongitude;

    // Campos para transporte en avión
    @Column(name = "flight_status", length = 50)
    private String flightStatus;

    @Column(name = "departure_airport", length = 255)
    private String departureAirport;

    @Column(name = "departure_timezone", length = 100)
    private String departureTimezone;

    @Column(name = "departure_scheduled")
    private ZonedDateTime departureScheduled;

    @Column(name = "departure_actual")
    private ZonedDateTime departureActual;

    @PrePersist
    @PreUpdate
    private void validateTransportData() {
        if ("auto".equals(transportMode)) {
            if (originAddress == null || destinationAddress == null) {
                throw new IllegalStateException("Auto transport requires origin and destination addresses");
            }
        } else if ("avion".equals(transportMode)) {
            if (departureAirport == null || departureScheduled == null) {
                // No lanzar excepción
                System.out.println("WARNING: Vuelo sin datos completos");
            }
        } else {
            // No lanzar excepción
            System.out.println("WARNING: caminando sin datos completos");
        }
    }
}
