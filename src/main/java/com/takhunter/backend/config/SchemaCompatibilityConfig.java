package com.takhunter.backend.config;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class SchemaCompatibilityConfig implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaCompatibilityConfig.class);

    private final JdbcTemplate jdbcTemplate;

    public SchemaCompatibilityConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        dropLegacyKegiatanEnumChecks();
    }

    private void dropLegacyKegiatanEnumChecks() {
        try {
            List<Map<String, Object>> constraints = jdbcTemplate.queryForList("""
                    select conname, pg_get_constraintdef(oid) as definition
                    from pg_constraint
                    where conrelid = 'kegiatan'::regclass
                      and contype = 'c'
                      and (
                        lower(pg_get_constraintdef(oid)) like '%kategori%'
                        or lower(pg_get_constraintdef(oid)) like '%status_publikasi%'
                      )
                    """);

            for (Map<String, Object> constraint : constraints) {
                String name = String.valueOf(constraint.get("conname"));
                jdbcTemplate.execute("alter table kegiatan drop constraint if exists \"" + name.replace("\"", "\"\"") + "\"");
                log.info("Dropped legacy kegiatan check constraint: {}", name);
            }
        } catch (Exception exception) {
            log.debug("Skipping kegiatan schema compatibility check: {}", exception.getMessage());
        }
    }
}
