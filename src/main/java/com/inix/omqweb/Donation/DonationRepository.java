package com.inix.omqweb.Donation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Integer> {
    @Query("SELECT d.player, SUM(d.amount) FROM Donation d GROUP BY d.player")
    List<Object[]> findDonationsGroupedByPlayer();

    @Query(value = "SELECT * FROM donation ORDER BY time DESC LIMIT 5", nativeQuery = true)
    List<Donation> findRecentDonations();
}