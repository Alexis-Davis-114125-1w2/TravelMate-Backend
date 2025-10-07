package TravelMate_Backend.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripDestinationId implements Serializable {

    @Column(name = "trip_id")
    private Long tripId;

    @Column(name = "destination_id")
    private Long destinationId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TripDestinationId that = (TripDestinationId) o;
        return Objects.equals(tripId, that.tripId) &&
                Objects.equals(destinationId, that.destinationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tripId, destinationId);
    }
}