package TravelMate_Backend.demo.model;

public enum Currency {
    PESOS("ARS", "$"),
    DOLARES("USD", "US$"),
    EUROS("EUR", "€");

    private final String code;
    private final String symbol;

    Currency(String code, String symbol) {
        this.code = code;
        this.symbol = symbol;
    }

    public String getCode() {
        return code;
    }

    public String getSymbol() {
        return symbol;
    }
}

