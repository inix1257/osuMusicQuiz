package com.inix.omqweb.Beatmap.Alias;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AliasId implements Serializable {
    private int beatmapset_id;
    private String name;
    private int type;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AliasId aliasId)) return false;
        return beatmapset_id == aliasId.beatmapset_id && name.equalsIgnoreCase(aliasId.name) && type == aliasId.type;
    }

    @Override
    public int hashCode() {
        return beatmapset_id + name.toLowerCase().hashCode() + type;
    }
}
