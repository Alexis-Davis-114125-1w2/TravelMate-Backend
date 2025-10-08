package TravelMate_Backend.demo.config;

import TravelMate_Backend.demo.security.AuthTokenFilter;
import TravelMate_Backend.demo.security.JwtUtils;
import TravelMate_Backend.demo.service.UserDetailsServiceImpl;
import TravelMate_Backend.demo.service.OAuth2UserService;
import TravelMate_Backend.demo.service.AuthService;
import TravelMate_Backend.demo.model.User;
import TravelMate_Backend.demo.model.AuthProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    
    @Autowired
    private OAuth2UserService oAuth2UserService;
    
    public SecurityConfig() {
        System.out.println("SecurityConfig constructor - OAuth2UserService será inyectado");
    }
    
    @Autowired
    @Lazy
    private AuthService authService;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(passwordEncoder());
        authProvider.setUserDetailsService(userDetailsService);
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        System.out.println("SecurityConfig.filterChain - OAuth2UserService: " + (oAuth2UserService != null ? "INYECTADO" : "NULL"));
        
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> 
                auth.requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api/test/**").permitAll()
                    .requestMatchers("/oauth2/**").permitAll()
                    .requestMatchers("/api/oauth2/**").permitAll()
                    .requestMatchers("/api/trips/add").authenticated()
                    .requestMatchers("/api/trips/get/**").authenticated()
                    .requestMatchers("/api/trips/**").permitAll()
                    .requestMatchers("/login/oauth2/code/**").permitAll()
                    // Swagger/OpenAPI endpoints
                    .requestMatchers("/swagger-ui/**").permitAll()
                    .requestMatchers("/v3/api-docs/**").permitAll()
                    .requestMatchers("/swagger-ui.html").permitAll()
                    .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(oAuth2UserService)
                )
                .defaultSuccessUrl("http://localhost:3000/auth/callback", true)
                .successHandler((request, response, authentication) -> {
                    try {
                        System.out.println("OAuth2 Success Handler - Authentication: " + authentication.getName());
                        
                        // Obtener el usuario de la autenticación
                        Object principal = authentication.getPrincipal();
                        System.out.println("Principal type: " + principal.getClass().getName());
                        
                        String email = null;
                        org.springframework.security.oauth2.core.user.OAuth2User oauth2User = null;
                        if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
                            oauth2User = (org.springframework.security.oauth2.core.user.OAuth2User) principal;
                            email = oauth2User.getAttribute("email");
                            System.out.println("OAuth2 User email: " + email);
                        }
                        
                        if (email != null) {
                            System.out.println("Procesando usuario OAuth2 con email: " + email);
                            
                            // Buscar el usuario en la base de datos
                            User user = authService.findByEmail(email).orElse(null);
                            
                            if (user == null) {
                                System.out.println("Usuario no existe, creando nuevo usuario OAuth2...");
                                
                                // Crear nuevo usuario OAuth2
                                user = new User();
                                user.setName(oauth2User.getAttribute("name") != null ? 
                                    oauth2User.getAttribute("name") : email.split("@")[0]);
                                user.setEmail(email);
                                user.setGoogleId(oauth2User.getAttribute("sub"));
                                user.setProfilePictureUrl(oauth2User.getAttribute("picture"));
                                user.setProvider(AuthProvider.GOOGLE);
                                user.setEmailVerified(true);
                                user.setPassword("OAUTH2_USER_PASSWORD");
                                
                                System.out.println("Guardando nuevo usuario OAuth2...");
                                user = authService.save(user);
                                System.out.println("Usuario OAuth2 creado con ID: " + user.getId());
                            } else {
                                System.out.println("Usuario existente encontrado: " + user.getName() + " (ID: " + user.getId() + ")");
                            }
                            
                            // Generar token y redirigir
                            String token = authService.generateTokenForUser(user);
                            System.out.println("Token generado para OAuth2: " + token.substring(0, 20) + "...");
                            
                            // Redirigir con el token
                            String redirectUrl = "http://localhost:3000/auth/callback?token=" + token;
                            System.out.println("Redirigiendo a: " + redirectUrl);
                            response.sendRedirect(redirectUrl);
                            return;
                        }
                        
                        // Si no se puede obtener el usuario, redirigir a error
                        System.err.println("No se pudo obtener usuario OAuth2");
                        response.sendRedirect("http://localhost:3000/login/error?error=oauth2_user_not_found");
                    } catch (Exception e) {
                        System.err.println("Error en OAuth2 Success Handler: " + e.getMessage());
                        e.printStackTrace();
                        response.sendRedirect("http://localhost:3000/login/error?error=oauth2_error");
                    }
                })
            );
        
        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    private String generateTokenForUser(org.springframework.security.core.Authentication authentication) {
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
            org.springframework.security.oauth2.core.user.OAuth2User oauth2User = 
                (org.springframework.security.oauth2.core.user.OAuth2User) authentication.getPrincipal();
            String email = oauth2User.getAttribute("email");
            return jwtUtils.generateTokenFromUsername(email);
        }
        return "mock-jwt-token";
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedOrigins(List.of("http://localhost:8080", "http://127.0.0.1:8080"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
