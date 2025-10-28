package TravelMate_Backend.demo.service;

import TravelMate_Backend.demo.model.PasswordResetToken;
import TravelMate_Backend.demo.model.User;
import TravelMate_Backend.demo.repository.PasswordResetTokenRepository;
import TravelMate_Backend.demo.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;

@Service
public class PasswordResetService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final SecureRandom random = new SecureRandom();

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private JavaMailSenderImpl mailSenderImpl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendPasswordResetCode(String toEmail, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("🔐 Código de recuperación de contraseña - TravelMate");

            String htmlContent = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; 
                                border: 1px solid #e0e0e0; border-radius: 10px; padding: 20px;">
                        <h2 style="color: #2c3e50;">Hola 👋,</h2>
                        <p style="font-size: 15px; color: #333;">
                            Hemos recibido una solicitud para <strong>restablecer tu contraseña</strong>.
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
                    """.formatted(code);

            helper.setText(htmlContent, true); // true = HTML

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar el correo de recuperación", e);
        }
    }

    @Transactional
    public void initiatePasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new RuntimeException("No existe un usuario con ese email");
        }

        // Eliminar tokens anteriores del mismo email
        tokenRepository.deleteByEmail(email);

        // Generar código de 6 dígitos
        String code = String.format("%06d", random.nextInt(1000000));

        // Crear y guardar token
        PasswordResetToken token = new PasswordResetToken(email, code);
        tokenRepository.save(token);

        // Enviar email
        sendPasswordResetCode(email, code);
    }

    public boolean verifyCode(String email, String code) {
        Optional<PasswordResetToken> tokenOpt =
                tokenRepository.findByEmailAndCodeAndUsedFalse(email, code);

        if (tokenOpt.isEmpty()) {
            return false;
        }

        PasswordResetToken token = tokenOpt.get();

        if (token.isExpired()) {
            throw new RuntimeException("El código ha expirado");
        }

        return true;
    }

    @Transactional
    public void resetPassword(String email, String code, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Las contraseñas no coinciden");
        }

        Optional<PasswordResetToken> tokenOpt =
                tokenRepository.findByEmailAndCodeAndUsedFalse(email, code);

        if (tokenOpt.isEmpty()) {
            throw new RuntimeException("Código inválido");
        }

        PasswordResetToken token = tokenOpt.get();

        if (token.isExpired()) {
            throw new RuntimeException("El código ha expirado");
        }

        if (token.isUsed()) {
            throw new RuntimeException("El código ya fue utilizado");
        }

        // Actualizar contraseña del usuario
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Marcar token como usado
        token.setUsed(true);
        tokenRepository.save(token);
    }
}