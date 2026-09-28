package com.factorypick.api.tools;

import com.factorypick.api.FactoryPickApplication;
import com.factorypick.api.repository.FactoryRepository;
import com.factorypick.api.service.KakaoGeocodingClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Explicit maintenance command; never runs during ordinary server startup. */
public class RetryGeocoding {
    record Target(long factoryId, String address, String status, String geocodedAt, String updatedAt) {}

    public static void main(String[] args) throws Exception {
        try (var context = new SpringApplicationBuilder(FactoryPickApplication.class)
                .web(WebApplicationType.NONE).run("--spring.sql.init.mode=never", "--debug=false",
                        "--logging.level.root=WARN", "--logging.level.org.springframework=WARN",
                        "--spring.main.banner-mode=off")) {
            var jdbc = context.getBean(JdbcTemplate.class);
            var factories = context.getBean(FactoryRepository.class);
            var kakao = context.getBean(KakaoGeocodingClient.class);
            var json = context.getBean(ObjectMapper.class);
            var targets = jdbc.query("""
                    SELECT factory_id,address,geocoding_status,geocoded_at,updated_at FROM factory
                    WHERE geocoding_status='NOT_FOUND' AND latitude IS NULL AND longitude IS NULL
                      AND address IS NOT NULL AND TRIM(address)<>'' ORDER BY factory_id
                    """, (rs, row) -> new Target(rs.getLong("factory_id"),rs.getString("address"),
                    rs.getString("geocoding_status"),rs.getString("geocoded_at"),rs.getString("updated_at")));
            Path output = Path.of("backups", "geocoding-retry-" + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))).toAbsolutePath();
            Files.createDirectories(output);
            try (var before = Files.newBufferedWriter(output.resolve("before.jsonl"), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW)) {
                for (var target : targets) { before.write(json.writeValueAsString(target)); before.newLine(); }
            }
            // These originals already returned NOT_FOUND. Reuse one exact road-address lookup
            // for factories in the same building; do not modify their stored addresses.
            Map<String,List<Target>> groups = new LinkedHashMap<>();
            for (var target : targets) groups.computeIfAbsent(KakaoGeocodingClient.roadAddress(target.address()),
                    ignored -> new ArrayList<>()).add(target);
            var queue = new ConcurrentLinkedQueue<>(groups.entrySet());
            var counters = new ConcurrentSkipListMap<String,AtomicInteger>();
            var processed = new AtomicInteger();
            var lookedUp = new AtomicInteger();
            var stopped = new AtomicReference<String>();
            var errors = new AtomicInteger();
            Runnable progress = () -> System.out.println("PROGRESS factories=" + processed.get() + "/" + targets.size()
                    + " queries=" + lookedUp.get() + "/" + groups.size() + " results=" + counts(counters));
            System.out.println("RETRY_START factories=" + targets.size() + " uniqueQueries=" + groups.size()
                    + " backup=" + output);
            var workers = Executors.newFixedThreadPool(3);
            var reporter = Executors.newSingleThreadScheduledExecutor();
            try (var results = Files.newBufferedWriter(output.resolve("results.jsonl"), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW)) {
                reporter.scheduleAtFixedRate(progress,15,15,TimeUnit.SECONDS);
                List<Future<?>> futures = new ArrayList<>();
                for (int worker=0;worker<3;worker++) futures.add(workers.submit(() -> {
                    while (stopped.get()==null) {
                        var group = queue.poll();
                        if (group==null) return;
                        try {
                            var result = kakao.lookup(group.getKey());
                            if (result.status().equals("FAILED")) {
                                Thread.sleep(1000);
                                result = kakao.lookup(group.getKey());
                            }
                            lookedUp.incrementAndGet();
                            if (result.status().equals("FAILED")) {
                                if (errors.incrementAndGet() >= 5) {
                                    stopped.compareAndSet(null,"Repeated provider failures; remaining rows were not changed.");
                                    return;
                                }
                            } else errors.set(0);
                            for (var target : group.getValue()) {
                                int changed = factories.saveGeocoding(target.factoryId(),target.address(),result);
                                String outcome = changed==1 ? result.status() : "SKIPPED_CHANGED";
                                counters.computeIfAbsent(outcome,ignored -> new AtomicInteger()).incrementAndGet();
                                processed.incrementAndGet();
                                Map<String,Object> entry = new LinkedHashMap<>();
                                entry.put("factoryId",target.factoryId());
                                entry.put("query",group.getKey());
                                entry.put("status",outcome);
                                entry.put("latitude",result.latitude());
                                entry.put("longitude",result.longitude());
                                synchronized (results) {
                                    results.write(json.writeValueAsString(entry)); results.newLine(); results.flush();
                                }
                            }
                            Thread.sleep(200);
                        } catch (ResponseStatusException exception) {
                            stopped.compareAndSet(null,exception.getReason());
                        } catch (Exception exception) {
                            // Do not print request headers, credentials or third-party exception bodies.
                            stopped.compareAndSet(null,"Stopped after " + exception.getClass().getSimpleName());
                            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
                        }
                    }
                }));
                for (var future : futures) future.get();
            } finally {
                reporter.shutdownNow(); workers.shutdownNow();
            }
            Map<String,Object> summary = new LinkedHashMap<>();
            summary.put("targetFactories",targets.size());
            summary.put("processedFactories",processed.get());
            summary.put("unprocessedFactories",targets.size()-processed.get());
            summary.put("uniqueQueries",groups.size());
            summary.put("queried",lookedUp.get());
            summary.put("results",counts(counters));
            summary.put("stopReason",stopped.get());
            summary.put("databaseStatuses",jdbc.queryForList(
                    "SELECT geocoding_status,COUNT(*) count FROM factory GROUP BY geocoding_status"));
            Files.writeString(output.resolve("summary.json"),json.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(summary),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
            progress.run();
            System.out.println("RETRY_SUMMARY " + json.writeValueAsString(summary));
            if (stopped.get()!=null) throw new IllegalStateException("Retry stopped. See " + output.resolve("summary.json"));
        }
    }

    private static Map<String,Integer> counts(Map<String,AtomicInteger> counters) {
        Map<String,Integer> counts = new TreeMap<>();
        counters.forEach((status,count) -> counts.put(status,count.get()));
        return counts;
    }
}
