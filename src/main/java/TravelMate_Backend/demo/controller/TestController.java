package TravelMate_Backend.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {
    
    @GetMapping("/hello")
    public Map<String, String> hello() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Backend funcionando correctamente");
        response.put("status", "OK");
        return response;
    }
    
    @GetMapping("/oauth2-url")
    public Map<String, String> getOAuth2Url() {
        Map<String, String> response = new HashMap<>();
        response.put("oauth2Url", "http://localhost:8080/oauth2/authorization/google");
        return response;
    }
}
