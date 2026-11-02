package com.kanimc;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class KanimcApplication {
    public static void main(String[] args) {
        SpringApplication.run(KanimcApplication.class, args);
    }

    @Bean
    public CommandLineRunner sanitizeFarmerCodes(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                // Ensure all farmer/account codes in database are within 4 letters/characters
                int updated = jdbcTemplate.update(
                    "UPDATE accounts SET party_id = CAST(id AS VARCHAR(4)) " +
                    "WHERE party_id IS NULL OR LENGTH(party_id) > 4 OR party_id LIKE 'P-%'"
                );
                System.out.println("[KANIMC] Sanitized farmer/account codes in DB (within 4 letters): " + updated + " records updated.");
            } catch (Exception e) {
                System.err.println("[KANIMC] Could not sanitize farmer codes: " + e.getMessage());
            }
        };
    }
}
