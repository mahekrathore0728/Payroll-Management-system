package com.payroll.utils;

import com.payroll.model.User;
import com.payroll.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;

    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Non-destructive maintenance for existing database users:
        // Safely backfill email from linked employee profile if missing
        for (User u : userRepository.findAll()) {
            if (u.getEmail() == null || u.getEmail().isBlank()) {
                if (u.getEmployee() != null && u.getEmployee().getEmail() != null) {
                    u.setEmail(u.getEmployee().getEmail());
                    userRepository.save(u);
                }
            }
        }

        log.info("Database startup check complete. Total registered users in database: {}", userRepository.count());
    }
}
