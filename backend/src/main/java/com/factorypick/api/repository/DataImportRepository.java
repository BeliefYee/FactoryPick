package com.factorypick.api.repository;

import com.factorypick.api.domain.DataImport;
import com.factorypick.api.dto.ImportResult;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class DataImportRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public DataImportRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public long insert(String sourceName, int total, int inserted, int updated, int skipped, int failed,
                       String status, String message) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO data_imports(source_name,total_rows,inserted_rows,updated_rows,skipped_rows,failed_rows,status,message)
                VALUES(:source,:total,:inserted,:updated,:skipped,:failed,:status,:message)
                """, new MapSqlParameterSource("source", sourceName).addValue("total", total)
                .addValue("inserted", inserted).addValue("updated", updated).addValue("skipped", skipped)
                .addValue("failed", failed).addValue("status", status).addValue("message", message),
                key, new String[]{"import_id"});
        return Objects.requireNonNull(key.getKey()).longValue();
    }

    public List<DataImport> findAll() {
        return jdbc.query("SELECT * FROM data_imports ORDER BY import_id DESC LIMIT 100", Map.of(), (rs, n) ->
                new DataImport(rs.getLong("import_id"), rs.getString("source_name"), rs.getInt("total_rows"),
                        rs.getInt("inserted_rows"), rs.getInt("updated_rows"), rs.getInt("skipped_rows"),
                        rs.getInt("failed_rows"), rs.getString("status"), rs.getString("message"),
                        rs.getTimestamp("imported_at").toLocalDateTime()));
    }
}
