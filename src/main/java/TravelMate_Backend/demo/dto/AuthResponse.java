package TravelMate_Backend.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    
    private String token;
    private String type = "Bearer";
    private Long id;
    private String name;
    private String email;
    private String profilePictureUrl;
    private String provider;
    
    public AuthResponse(String token, Long id, String name, String email, String profilePictureUrl, String provider) {
        this.token = token;
        this.id = id;
        this.name = name;
        this.email = email;
        this.profilePictureUrl = profilePictureUrl;
        this.provider = provider;
    }
}
