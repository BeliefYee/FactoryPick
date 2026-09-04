package com.factorypick.api.config;

import com.factorypick.api.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final AdminRepository admins;
    private final PasswordEncoder encoder;
    private final String username;
    private final String password;

    public AdminBootstrap(AdminRepository admins, PasswordEncoder encoder,
                          @Value("${factorypick.admin.username}") String username,
                          @Value("${factorypick.admin.password}") String password) {
        this.admins = admins; this.encoder = encoder; this.username = username; this.password = password;
    }
    @Override public void run(ApplicationArguments args) {
        if (!admins.exists(username)) admins.insert(username, encoder.encode(password));
    }
}
