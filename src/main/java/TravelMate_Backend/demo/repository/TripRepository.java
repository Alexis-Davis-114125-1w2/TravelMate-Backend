package TravelMate_Backend.demo.repository;

import TravelMate_Backend.demo.model.Trip;
import TravelMate_Backend.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByUsersId(Long userId);
    boolean existsByIdAndUsersId(Long tripId, Long userId);
    Optional<Trip> findByJoinCode(String joinCode);
    @Modifying
    @Query(value = "DELETE FROM users_trip WHERE trip_id = :tripId", nativeQuery = true)
    void deleteTripRelations(@Param("tripId") Long tripId);
}
