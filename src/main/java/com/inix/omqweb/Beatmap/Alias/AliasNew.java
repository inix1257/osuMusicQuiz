package com.inix.omqweb.Beatmap.Alias;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "alias_new")
public class AliasNew {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, name = "source_text")
    private String sourceText;
    
    @Column(nullable = false, name = "target_text")
    private String targetText;
    
    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false, name = "alias_type")
    private AliasType aliasType;
    
    @Override
    public String toString() {
        return sourceText + " → " + targetText + " [" + aliasType + "]";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof AliasNew aliasNew)) return false;
        return aliasNew.sourceText.equalsIgnoreCase(this.sourceText) && 
               aliasNew.targetText.equalsIgnoreCase(this.targetText) && 
               aliasNew.aliasType == this.aliasType;
    }
    
    @Override
    public int hashCode() {
        return sourceText.toLowerCase().hashCode() + 
               targetText.toLowerCase().hashCode() + 
               aliasType.hashCode();
    }
} 