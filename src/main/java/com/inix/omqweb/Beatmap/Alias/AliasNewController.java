package com.inix.omqweb.Beatmap.Alias;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alias-new")
public class AliasNewController {
    
    @Autowired
    private AliasNewService aliasNewService;
    
    @Autowired
    private AliasRepository oldAliasRepository;
    
    @GetMapping
    public ResponseEntity<List<AliasNew>> getAllAliases() {
        return ResponseEntity.ok(aliasNewService.getAllAliases());
    }
    
    @GetMapping("/{type}")
    public ResponseEntity<List<AliasNew>> getAliasesByType(@PathVariable String type) {
        try {
            AliasType aliasType = AliasType.valueOf(type.toUpperCase());
            return ResponseEntity.ok(aliasNewService.getAliasesByType(aliasType));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping
    public ResponseEntity<Void> addAlias(@RequestBody AliasNewAddDTO aliasAddDTO) {
        aliasNewService.addAlias(aliasAddDTO);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/migrate")
    public ResponseEntity<Map<String, Integer>> migrateAliases() {
        int count = aliasNewService.migrateExistingAliases(oldAliasRepository);
        return ResponseEntity.ok(Map.of("migratedCount", count));
    }
} 