package TravelMate_Backend.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "trips")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 150, message = "El nombre debe tener entre 2 y 150 caracteres")
    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "date_i", nullable = false)
    private LocalDate dateI;

    @Column(name = "date_f", nullable = false)
    private LocalDate dateF;

    @Column(name = "cost", precision = 10, scale = 2)
    private BigDecimal cost;

    @Column(name = "image")
    private byte[] image;

    @Transient
    private String status;

    @ManyToMany(mappedBy = "trips", fetch = FetchType.LAZY)
    private Set<User> users = new HashSet<>();

    public Trip(String name, String description, LocalDate dateI, LocalDate dateF, BigDecimal cost) {
        this.name = name;
        this.description = description;
        this.dateI = dateI;
        this.dateF = dateF;
        this.cost = cost != null ? cost : BigDecimal.ZERO;
    }
}
