package com.factorypick.api.repository;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class AdminRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public AdminRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<String> findPasswordHash(String username) {
        return jdbc.query("SELECT password_hash FROM admins WHERE username=:username", Map.of("username", username),
                (rs, n) -> rs.getString(1)).stream().findFirst();
    }
    public boolean exists(String username) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM admins WHERE username=:username",
                Map.of("username", username), Integer.class);
        return count != null && count > 0;
    }
    public void insert(String username, String passwordHash) {
        jdbc.update("INSERT INTO admins(username,password_hash) VALUES(:username,:hash)",
                Map.of("username", username, "hash", passwordHash));
    }
}
