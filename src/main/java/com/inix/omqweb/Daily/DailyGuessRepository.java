package com.inix.omqweb.Daily;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Date;

public interface DailyGuessRepository extends JpaRepository<DailyGuess, Integer> {
    @Query("SELECT dg FROM DailyGuess dg WHERE dg.date >= :date ORDER BY dg.date ASC LIMIT 1")
    DailyGuess findFirstByDateAfter(@Param("date") Date date);
}