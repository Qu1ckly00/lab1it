package org.example.server.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public static class LoginReq { public String username; public String password; }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginReq req) {
        if (req.username == null || req.username.trim().isEmpty() || req.password == null || req.password.trim().isEmpty())
            return ResponseEntity.badRequest().body("Invalid credentials");
        // mock token = username
        Map<String,Object> resp = new HashMap<>();
        resp.put("token", req.username);
        resp.put("user", req.username);
        return ResponseEntity.ok(resp);
    }
}