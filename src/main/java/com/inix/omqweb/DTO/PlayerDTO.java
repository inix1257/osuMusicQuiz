package com.inix.omqweb.DTO;

import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import lombok.Data;

import java.util.Date;

@Data
public class PlayerDTO {
    private String id;
    private String username;
    private String country_code;
    private String cover_url;
    private String avatar_url;

    @Temporal(TemporalType.TIMESTAMP)
    @Transient
    private Date join_date;
}
