package com.factoryflow.erp.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityPasswordTest {

    private static final String BENCHMARK_HASH =
            "$2a$10$S03fW0rDal/AXU3hYBG0GOAzIgegPYnHTkv20xdtjfQGPmxp4MIwa";

    @Test
    void benchmarkSeedPasswordHashMatchesPass123() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        assertThat(encoder.matches("pass123", BENCHMARK_HASH)).isTrue();
        assertThat(encoder.matches("wrongpass", BENCHMARK_HASH)).isFalse();
    }
}
