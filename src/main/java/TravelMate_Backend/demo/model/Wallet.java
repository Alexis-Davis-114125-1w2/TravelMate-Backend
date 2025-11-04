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
import java.time.ZonedDateTime;

@Entity
@Table(name = "wallet")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    @JsonIgnore
    private User user; // null para billeteras generales

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    @JsonIgnore
    private Trip trip;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    @NotNull(message = "La moneda es obligatoria")
    private Currency currency;

    @Column(name = "is_general", nullable = false)
    private Boolean isGeneral = false; // true para billetera compartida, false para individual

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void validateWallet() {
        if (trip == null) {
            throw new IllegalStateException("Wallet must have a trip");
        }
        // Si es billetera general, user debe ser null
        if (isGeneral != null && isGeneral && user != null) {
            throw new IllegalStateException("General wallet cannot have a user");
        }
        // Si es billetera individual, user no debe ser null
        if (isGeneral != null && !isGeneral && user == null) {
            throw new IllegalStateException("Individual wallet must have a user");
        }
        if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Amount cannot be negative");
        }
    }
}
