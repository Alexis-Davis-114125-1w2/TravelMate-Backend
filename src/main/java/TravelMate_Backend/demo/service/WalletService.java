package TravelMate_Backend.demo.service;

import TravelMate_Backend.demo.dto.WalletResponse;
import TravelMate_Backend.demo.dto.WalletUpdateRequest;
import TravelMate_Backend.demo.model.*;
import TravelMate_Backend.demo.repository.WalletRepository;
import TravelMate_Backend.demo.repository.TripRepository;
import TravelMate_Backend.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private UserRepository userRepository;

    public WalletResponse getGeneralWallet(Long tripId) {
        Wallet wallet = walletRepository.findByTripIdAndIsGeneralTrue(tripId)
                .orElseThrow(() -> new RuntimeException("Billetera general no encontrada para el viaje"));
        return convertToResponse(wallet);
    }

    public WalletResponse getIndividualWallet(Long tripId, Long userId) {
        Wallet wallet = walletRepository.findByTripIdAndUserIdAndIsGeneralFalse(tripId, userId)
                .orElseThrow(() -> new RuntimeException("Billetera individual no encontrada"));
        return convertToResponse(wallet);
    }

    public List<WalletResponse> getAllWalletsByTrip(Long tripId) {
        List<Wallet> wallets = walletRepository.findByTripId(tripId);
        return wallets.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public WalletResponse updateGeneralWallet(Long tripId, WalletUpdateRequest request) {
        tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Viaje no encontrado"));

        Wallet wallet = walletRepository.findByTripIdAndIsGeneralTrue(tripId)
                .orElseThrow(() -> new RuntimeException("Billetera general no encontrada"));

        wallet.setAmount(request.getAmount());
        wallet.setCurrency(request.getCurrency());
        wallet = walletRepository.save(wallet);

        return convertToResponse(wallet);
    }

    public WalletResponse updateIndividualWallet(Long tripId, Long userId, WalletUpdateRequest request) {
        tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Viaje no encontrado"));

        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Verificar que el usuario pertenece al viaje usando el método del repository
        boolean userBelongsToTrip = tripRepository.existsByIdAndUsersId(tripId, userId);
        if (!userBelongsToTrip) {
            throw new RuntimeException("El usuario no pertenece a este viaje");
        }

        Wallet wallet = walletRepository.findByTripIdAndUserIdAndIsGeneralFalse(tripId, userId)
                .orElseThrow(() -> new RuntimeException("Billetera individual no encontrada"));

        wallet.setAmount(request.getAmount());
        wallet.setCurrency(request.getCurrency());
        wallet = walletRepository.save(wallet);

        return convertToResponse(wallet);
    }

    public Wallet createGeneralWallet(Trip trip, BigDecimal amount, Currency currency) {
        Wallet wallet = new Wallet();
        wallet.setTrip(trip);
        wallet.setUser(null);
        wallet.setAmount(amount);
        wallet.setCurrency(currency);
        wallet.setIsGeneral(true);
        return walletRepository.save(wallet);
    }

    public Wallet createIndividualWallet(Trip trip, User user, Currency currency) {
        Wallet wallet = new Wallet();
        wallet.setTrip(trip);
        wallet.setUser(user);
        wallet.setAmount(BigDecimal.ZERO);
        wallet.setCurrency(currency);
        wallet.setIsGeneral(false);
        return walletRepository.save(wallet);
    }

    private WalletResponse convertToResponse(Wallet wallet) {
        WalletResponse response = new WalletResponse();
        response.setId(wallet.getId());
        response.setTripId(wallet.getTrip().getId());
        response.setAmount(wallet.getAmount());
        response.setCurrency(wallet.getCurrency());
        response.setCurrencySymbol(wallet.getCurrency().getSymbol());
        response.setIsGeneral(wallet.getIsGeneral());
        response.setCreatedAt(wallet.getCreatedAt());
        response.setUpdatedAt(wallet.getUpdatedAt());

        if (wallet.getUser() != null) {
            response.setUserId(wallet.getUser().getId());
            response.setUserName(wallet.getUser().getName());
            response.setUserEmail(wallet.getUser().getEmail());
        }

        return response;
    }
}

