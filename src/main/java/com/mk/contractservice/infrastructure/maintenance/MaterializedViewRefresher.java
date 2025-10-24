package com.mk.contractservice.infrastructure.maintenance;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MaterializedViewRefresher {
    private final JdbcTemplate jdbc;

    public MaterializedViewRefresher(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // toutes les 5 minutes (cron: sec min heure jour mois jourSemaine)
    @Scheduled(cron = "0 */5 * * * *")
    public void refreshMViews() {
        jdbc.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_active_contracts");
        jdbc.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_active_contracts_sum");
    }
}