package com.inix.omqweb.Beatmap.Alias;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AliasNewRepository extends JpaRepository<AliasNew, Long> {
    
    List<AliasNew> findByAliasType(AliasType aliasType);
    
    @Query("SELECT a FROM AliasNew a WHERE a.sourceText = :text AND a.aliasType = :aliasType")
    List<AliasNew> findBySourceTextAndAliasType(@Param("text") String text, @Param("aliasType") AliasType aliasType);
    
    @Query("SELECT a FROM AliasNew a WHERE a.targetText = :text AND a.aliasType = :aliasType")
    List<AliasNew> findByTargetTextAndAliasType(@Param("text") String text, @Param("aliasType") AliasType aliasType);
    
    @Query("SELECT a FROM AliasNew a WHERE LOWER(a.sourceText) = LOWER(:text) AND a.aliasType = :aliasType")
    List<AliasNew> findBySourceTextIgnoreCaseAndAliasType(@Param("text") String text, @Param("aliasType") AliasType aliasType);
    
    @Query("SELECT a FROM AliasNew a WHERE LOWER(a.targetText) = LOWER(:text) AND a.aliasType = :aliasType")
    List<AliasNew> findByTargetTextIgnoreCaseAndAliasType(@Param("text") String text, @Param("aliasType") AliasType aliasType);
} 