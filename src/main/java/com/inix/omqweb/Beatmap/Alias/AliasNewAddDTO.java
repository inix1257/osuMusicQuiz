package com.inix.omqweb.Beatmap.Alias;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AliasNewAddDTO {
    private String sourceText;
    private String targetText;
    private String type;
} 