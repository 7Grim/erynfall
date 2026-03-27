package com.osrs.auth.health;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final DatabaseProbe databaseProbe;

    public HealthController(DatabaseProbe databaseProbe) {
        this.databaseProbe = databaseProbe;
    }

    @GetMapping("/health/live")
    public ResponseEntity<Map<String, Object>> liveness() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "erynfall-auth");
        body.put("time", Instant.now().toString());
        body.put("status", "UP");
        return ResponseEntity.ok(body);
    }

    @GetMapping("/health/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        return health();
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        DatabaseProbe.Result db = databaseProbe.check();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "erynfall-auth");
        body.put("time", Instant.now().toString());
        body.put("requireDatabase", db.requireDatabase());
        body.put("database", db.up() ? "UP" : "DOWN");
        body.put("databaseMessage", db.message());

        boolean ok = db.up() || !db.requireDatabase();
        body.put("status", ok ? "UP" : "DOWN");

        return ResponseEntity.status(ok ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
