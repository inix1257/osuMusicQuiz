package com.inix.omqweb.Beatmap.Alias;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.inix.omqweb.Beatmap.Beatmap;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "alias")
public class Alias {
    @EmbeddedId
    private AliasId aliasId;

    @ManyToOne
    @JoinColumn(name = "beatmapset_id", referencedColumnName = "beatmapset_id", insertable = false, updatable = false)
    @JsonIgnore
    private Beatmap beatmap;

    @Column(insertable = false, updatable = false)
    private String name;

    @Column(name = "type", insertable = false, updatable = false)
    private AliasType aliasType;

    @Override
    public String toString() {
        return beatmap.getArtistAndTitle() + " [" + aliasId.getName() + "] " + beatmap.getBeatmapset_id() + " " + aliasId.getType();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof Alias alias)) return false;
        return alias.aliasId.equals(this.aliasId);
    }

    @Override
    public int hashCode() {
        return aliasId.hashCode();
    }
}
