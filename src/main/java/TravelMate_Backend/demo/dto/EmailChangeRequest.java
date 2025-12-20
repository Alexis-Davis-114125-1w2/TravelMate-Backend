package TravelMate_Backend.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailChangeRequest {
    @NotBlank(message = "El nuevo email es obligatorio")
    @Email(message = "El formato del email no es válido")
    private String newEmail;
}

