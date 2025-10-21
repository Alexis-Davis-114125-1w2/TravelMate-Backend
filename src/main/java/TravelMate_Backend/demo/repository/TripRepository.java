package TravelMate_Backend.demo.repository;

import TravelMate_Backend.demo.model.Trip;
import TravelMate_Backend.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByUsersId(Long userId);
    boolean existsByIdAndUsersId(Long tripId, Long userId);
    Optional<Trip> findByJoinCode(String joinCode);
}
