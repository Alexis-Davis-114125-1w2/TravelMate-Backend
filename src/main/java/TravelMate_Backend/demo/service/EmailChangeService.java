package TravelMate_Backend.demo.service;

import TravelMate_Backend.demo.model.EmailChangeToken;
import TravelMate_Backend.demo.model.User;
import TravelMate_Backend.demo.repository.EmailChangeTokenRepository;
import TravelMate_Backend.demo.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;

@Service
public class EmailChangeService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailChangeTokenRepository tokenRepository;

    private static final SecureRandom random = new SecureRandom();

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendEmailChangeCode(String currentEmail, String newEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(currentEmail);
            helper.setSubject("📧 Código de verificación para cambio de email - TravelMate");

            // Generar código de 6 dígitos
            String code = String.format("%06d", random.nextInt(1000000));

            // Eliminar tokens anteriores del mismo email
            tokenRepository.deleteByEmail(currentEmail);

            // Crear y guardar token
            EmailChangeToken token = new EmailChangeToken(currentEmail, newEmail, code);
            tokenRepository.save(token);

            String htmlContent = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; 
                                border: 1px solid #e0e0e0; border-radius: 10px; padding: 20px;">
                        <h2 style="color: #2c3e50;">Hola 👋,</h2>
                        <p style="font-size: 15px; color: #333;">
                            Hemos recibido una solicitud para <strong>cambiar tu email</strong> de <strong>%s</strong> a <strong>%s</strong>.
                        </p>
                        <div style="text-align: center; margin: 30px 0;">
                            <p style="font-size: 16px; color: #555;">Tu código de verificación es:</p>
                            <div style="background-color: #f0f0f0; display: inline-block; 
                                        padding: 15px 25px; border-radius: 8px; 
                                        font-size: 22px; font-weight: bold; color: #007bff;">
                                %s
                            </div>
                            <p style="font-size: 14px; color: #888; margin-top: 10px;">
                                Este código expirará en <strong>15 minutos</strong>.
                            </p>
                        </div>
                        <p style="font-size: 14px; color: #555;">
                            Si no solicitaste este cambio, puedes ignorar este correo.
                        </p>
                        <hr style="border: none; border-top: 1px solid #ddd; margin: 20px 0;">
                        <p style="font-size: 14px; color: #888; text-align: center;">
                            Saludos,<br><strong>Equipo TravelMate</strong>
                        </p>
                    </div>
                    """.formatted(currentEmail, newEmail, code);

            helper.setText(htmlContent, true); // true = HTML

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar el correo de verificación", e);
        }
    }

    public boolean verifyCode(String email, String code) {
        Optional<EmailChangeToken> tokenOpt =
                tokenRepository.findByEmailAndCodeAndUsedFalse(email, code);

        if (tokenOpt.isEmpty()) {
            return false;
        }

        EmailChangeToken token = tokenOpt.get();

        if (token.isExpired()) {
            throw new RuntimeException("El código ha expirado");
        }

        return true;
    }

    @Transactional
    public void confirmEmailChange(String email, String code) {
        Optional<EmailChangeToken> tokenOpt =
                tokenRepository.findByEmailAndCodeAndUsedFalse(email, code);

        if (tokenOpt.isEmpty()) {
            throw new RuntimeException("Código inválido");
        }

        EmailChangeToken token = tokenOpt.get();

        if (token.isExpired()) {
            throw new RuntimeException("El código ha expirado");
        }

        if (token.isUsed()) {
            throw new RuntimeException("El código ya fue utilizado");
        }

        // Verificar que el nuevo email no esté en uso
        if (userRepository.existsByEmail(token.getNewEmail())) {
            throw new RuntimeException("El nuevo email ya está en uso por otro usuario");
        }

        // Actualizar email del usuario
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setEmail(token.getNewEmail());
        userRepository.save(user);

        // Marcar token como usado
        token.setUsed(true);
        tokenRepository.save(token);
    }
}

