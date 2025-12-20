package TravelMate_Backend.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyEmailChangeCodeRequest {
    @NotBlank(message = "El código es obligatorio")
    private String code;
}

