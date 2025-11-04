package TravelMate_Backend.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "destinations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Destination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //El nombre es completo para despues usarlo en el front ej: "Córdoba, Argentina"
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    //"Argentina" A definir si lo unificamos
    @Column(name = "contry", length = 100)
    private String country;

    @Column(name = "Latitude", length = 100)
    private Integer latitude;

    @Column(name = "Length", length = 100)
    private Integer length;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "cost", precision = 12, scale = 2)
    private BigDecimal cost = BigDecimal.ZERO;

    @OneToMany(mappedBy = "destination", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<TripDestination> tripDestinations = new HashSet<>();
}