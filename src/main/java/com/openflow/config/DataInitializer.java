package com.openflow.config;

import com.openflow.model.Role;
import com.openflow.model.User;
import com.openflow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Create default admin user if it doesn't exist
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@openflow.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            System.out.println("Default admin user created: username=admin, password=admin123, role=ADMIN");
        }

        // Create default demo user if it doesn't exist
        if (!userRepository.existsByUsername("demo")) {
            User demo = new User();
            demo.setUsername("demo");
            demo.setEmail("demo@openflow.com");
            demo.setPassword(passwordEncoder.encode("demo123"));
            demo.setRole(Role.USER);
            userRepository.save(demo);
            System.out.println("Default demo user created: username=demo, password=demo123, role=USER");
        }
    }
}

