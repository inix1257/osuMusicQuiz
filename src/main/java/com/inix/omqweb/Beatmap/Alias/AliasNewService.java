package com.inix.omqweb.Beatmap.Alias;

import com.inix.omqweb.Beatmap.Beatmap;
import com.inix.omqweb.Beatmap.BeatmapRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AliasNewService {
    private final Logger logger = LoggerFactory.getLogger(AliasNewService.class);
    
    @Autowired
    private AliasNewRepository aliasNewRepository;
    
    @Autowired
    private BeatmapRepository beatmapRepository;
    
    public List<AliasNew> getAllAliases() {
        return aliasNewRepository.findAll();
    }
    
    public List<AliasNew> getAliasesByType(AliasType aliasType) {
        return aliasNewRepository.findByAliasType(aliasType);
    }
    
    public void addAlias(AliasNewAddDTO aliasAddDTO) {
        try {
            AliasType aliasType = AliasType.valueOf(aliasAddDTO.getType());
            
            // Check if alias already exists
            List<AliasNew> existingAliases = aliasNewRepository.findBySourceTextIgnoreCaseAndAliasType(
                    aliasAddDTO.getSourceText(), aliasType);
            
            for (AliasNew existingAlias : existingAliases) {
                if (existingAlias.getTargetText().equalsIgnoreCase(aliasAddDTO.getTargetText())) {
                    logger.info("Alias already exists: " + existingAlias);
                    return;
                }
            }
            
            AliasNew alias = AliasNew.builder()
                    .sourceText(aliasAddDTO.getSourceText())
                    .targetText(aliasAddDTO.getTargetText())
                    .aliasType(aliasType)
                    .build();

            AliasNew alias_reverse = AliasNew.builder()
                    .sourceText(aliasAddDTO.getTargetText())
                    .targetText(aliasAddDTO.getSourceText())
                    .aliasType(aliasType)
                    .build();
            
            aliasNewRepository.save(alias);
            aliasNewRepository.save(alias_reverse);
            logger.info("Added new alias: " + alias);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid alias type: " + aliasAddDTO.getType(), e);
        }
    }
    
    /**
     * Migrates existing aliases to the new format.
     * @return The number of aliases migrated
     */
    public int migrateExistingAliases(AliasRepository oldAliasRepository) {
        List<Alias> oldAliases = oldAliasRepository.findAll();
        Set<AliasNew> newAliases = new HashSet<>();
        Map<Integer, Beatmap> beatmapCache = new HashMap<>();
        
        int count = 0;
        int skipped = 0;
        
        for (Alias oldAlias : oldAliases) {
            try {
                // Get beatmap from repository if not in cache
                int beatmapsetId = oldAlias.getAliasId().getBeatmapset_id();
                Beatmap beatmap = beatmapCache.computeIfAbsent(
                    beatmapsetId, 
                    id -> beatmapRepository.findById(id).orElse(null)
                );
                
                if (beatmap == null) {
                    logger.warn("Beatmap not found for ID: " + beatmapsetId);
                    skipped++;
                    continue;
                }
                
                String sourceText = oldAlias.getAliasId().getName();
                String targetText;
                
                switch (oldAlias.getAliasType()) {
                    case ARTIST -> targetText = beatmap.getArtist();
                    case TITLE -> targetText = beatmap.getTitle();
                    case CREATOR -> targetText = beatmap.getCreator();
                    default -> {
                        logger.warn("Unknown alias type: " + oldAlias.getAliasType());
                        skipped++;
                        continue;
                    }
                }
                
                // Don't create aliases that point to themselves
                if (sourceText == null || targetText == null || sourceText.equalsIgnoreCase(targetText)) {
                    skipped++;
                    continue;
                }
                
                AliasNew newAlias = AliasNew.builder()
                        .sourceText(sourceText)
                        .targetText(targetText)
                        .aliasType(oldAlias.getAliasType())
                        .build();
                
                if (newAliases.add(newAlias)) {
                    count++;
                }
            } catch (Exception e) {
                logger.error("Error migrating alias: " + oldAlias, e);
                skipped++;
            }
        }
        
        if (!newAliases.isEmpty()) {
            aliasNewRepository.saveAll(newAliases);
        }
        
        logger.info("Migrated " + count + " aliases to the new format (skipped " + skipped + ")");
        return count;
    }
    
    /**
     * Checks if the given text matches any alias for the specified type.
     * @param text The text to check
     * @param aliasType The type of alias
     * @param exactTarget The expected target text
     * @return true if the text matches an alias, false otherwise
     */
    public boolean isAliasMatch(String text, AliasType aliasType, String exactTarget) {
        // Direct match
        if (text.equalsIgnoreCase(exactTarget)) {
            return true;
        }
        
        // Check if there's an alias match (source to target)
        List<AliasNew> sourceAliases = aliasNewRepository.findBySourceTextIgnoreCaseAndAliasType(text, aliasType);
        for (AliasNew alias : sourceAliases) {
            if (alias.getTargetText().equalsIgnoreCase(exactTarget)) {
                return true;
            }
        }
        
        // Check if there's an alias match (target to source)
        List<AliasNew> targetAliases = aliasNewRepository.findByTargetTextIgnoreCaseAndAliasType(text, aliasType);
        for (AliasNew alias : targetAliases) {
            if (alias.getSourceText().equalsIgnoreCase(exactTarget)) {
                return true;
            }
        }
        
        return false;
    }
} 