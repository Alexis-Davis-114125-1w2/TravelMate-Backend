package TravelMate_Backend.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;

@Entity
@Table(name = "purchase")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    @JsonIgnore
    private User user; // null para compras generales del viaje

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    @JsonIgnore
    private Trip trip;

    @Column(name = "description", length = 255)
    private String description; // Descripción de la compra

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    @NotNull(message = "La moneda es obligatoria")
    private Currency currency;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate; // Fecha de la compra

    @Column(name = "is_general", nullable = false)
    private Boolean isGeneral = false; // true para compra general del viaje, false para individual

    @Column(name = "created_by", nullable = false)
    private Long createdBy; // ID del usuario que creó/añadió esta compra

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void validatePurchase() {
        if (trip == null) {
            throw new IllegalStateException("Purchase must have a trip");
        }
        // Si es compra general, user debe ser null
        if (isGeneral != null && isGeneral && user != null) {
            throw new IllegalStateException("General purchase cannot have a user");
        }
        // Si es compra individual, user no debe ser null
        if (isGeneral != null && !isGeneral && user == null) {
            throw new IllegalStateException("Individual purchase must have a user");
        }
        if (price != null && price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Price cannot be negative");
        }
        if (purchaseDate == null) {
            throw new IllegalStateException("Purchase date is required");
        }
        if (createdBy == null) {
            throw new IllegalStateException("Purchase must have a creator (createdBy)");
        }
    }
}

