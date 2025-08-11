package com.inix.omqweb.Daily;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Date;

public interface DailyGuessRepository extends JpaRepository<DailyGuess, Integer> {
    DailyGuess findDailyGuessById(int id);

    @Query("SELECT d FROM DailyGuess d ORDER BY d.id DESC LIMIT 1")
    DailyGuess getLastDailyGuess();
}