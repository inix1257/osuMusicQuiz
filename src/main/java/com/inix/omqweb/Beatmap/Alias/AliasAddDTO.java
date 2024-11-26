package com.inix.omqweb.Beatmap.Alias;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AliasAddDTO {
    private String source;
    private String target;
    private String type;
}
