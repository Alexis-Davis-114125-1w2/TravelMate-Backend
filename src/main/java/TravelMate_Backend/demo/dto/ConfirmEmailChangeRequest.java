package TravelMate_Backend.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmEmailChangeRequest {
    @NotBlank(message = "El código es obligatorio")
    private String code;
}

