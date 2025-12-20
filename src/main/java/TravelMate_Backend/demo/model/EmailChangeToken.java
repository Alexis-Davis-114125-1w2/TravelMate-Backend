package TravelMate_Backend.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_change_tokens")
@Data
@NoArgsConstructor
public class EmailChangeToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String newEmail;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private boolean used = false;

    public EmailChangeToken(String email, String newEmail, String code) {
        this.email = email;
        this.newEmail = newEmail;
        this.code = code;
        this.expiryDate = LocalDateTime.now().plusMinutes(15); // Expira en 15 minutos
        this.used = false;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}

