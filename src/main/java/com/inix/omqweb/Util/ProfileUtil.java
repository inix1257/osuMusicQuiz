package com.inix.omqweb.Util;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class ProfileUtil {
    private final Environment environment;

    public boolean isDevEnv() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }
}
