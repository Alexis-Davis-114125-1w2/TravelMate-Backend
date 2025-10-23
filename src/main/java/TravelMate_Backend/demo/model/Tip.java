package TravelMate_Backend.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tips")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 200, message = "El nombre debe tener entre 2 y 200 caracteres")
    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotBlank(message = "La dirección es obligatoria")
    @Column(nullable = false, length = 500)
    private String address;

    @NotNull(message = "La latitud es obligatoria")
    @Column(nullable = false)
    private Double latitude;

    @NotNull(message = "La longitud es obligatoria")
    @Column(nullable = false)
    private Double longitude;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(name = "tip_type", length = 50)
    private String tipType; // restaurant, lodging, attraction, gas_station

    @Column(name = "tip_icon", length = 10)
    private String tipIcon; // 🍽️, 🏨, 🎯, ⛽

    @ElementCollection
    @CollectionTable(name = "tip_types", joinColumns = @JoinColumn(name = "tip_id"))
    @Column(name = "type")
    private List<String> types;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy; // Email del usuario que creó el tip

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    @JsonIgnore
    private Trip trip;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Tip(String name, String description, String address, Double latitude, Double longitude, 
               Double rating, Double distanceKm, String tipType, String tipIcon, 
               List<String> types, String createdBy, Trip trip) {
        this.name = name;
        this.description = description;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.rating = rating;
        this.distanceKm = distanceKm;
        this.tipType = tipType;
        this.tipIcon = tipIcon;
        this.types = types;
        this.createdBy = createdBy;
        this.trip = trip;
    }
}
