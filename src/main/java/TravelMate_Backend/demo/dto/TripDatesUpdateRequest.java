package TravelMate_Backend.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripDatesUpdateRequest {
    @NotNull(message = "La fecha de inicio es obligatoria")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateI;

    @NotNull(message = "La fecha de fin es obligatoria")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateF;

    @AssertTrue(message = "La fecha de fin debe ser posterior a la fecha de inicio")
    public boolean isValidDateRange() {
        if (dateI == null || dateF == null) {
            return true;
        }
        return dateF.isAfter(dateI);
    }
}

