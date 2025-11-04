package TravelMate_Backend.demo.dto;

import TravelMate_Backend.demo.model.Currency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {
    private Long id;
    private Long tripId;
    private Long userId;
    private String userName;
    private String userEmail;
    private BigDecimal amount;
    private Currency currency;
    private String currencySymbol;
    private Boolean isGeneral;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}

