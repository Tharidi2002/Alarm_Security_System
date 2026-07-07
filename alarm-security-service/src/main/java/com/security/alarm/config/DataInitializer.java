// package com.security.alarm.config;

// import com.security.alarm.entity.User;
// import com.security.alarm.repository.UserRepository;
// import org.springframework.boot.CommandLineRunner;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.stereotype.Component;

// @Component
// public class DataInitializer implements CommandLineRunner {

//     private final UserRepository userRepository;
//     private final PasswordEncoder passwordEncoder;

//     public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
//         this.userRepository = userRepository;
//         this.passwordEncoder = passwordEncoder;
//     }

//     @Override
//     public void run(String... args) {
//         // Create default admin if not exists
//         if (userRepository.findByUsername("admin").isEmpty()) {
//             User admin = new User();
//             admin.setUsername("admin");
//             admin.setPassword(passwordEncoder.encode("admin123"));
//             admin.setRole("ADMIN");
//             userRepository.save(admin);
//             System.out.println("✅ Default Admin user created: admin / admin123");
//         }

//         // Create default user1 if not exists
//         if (userRepository.findByUsername("user1").isEmpty()) {
//             User user1 = new User();
//             user1.setUsername("user1");
//             user1.setPassword(passwordEncoder.encode("user123"));
//             user1.setRole("USER");
//             userRepository.save(user1);
//             System.out.println("✅ Default User1 created: user1 / user123");
//         }

//         // Create default user2 if not exists
//         if (userRepository.findByUsername("user2").isEmpty()) {
//             User user2 = new User();
//             user2.setUsername("user2");
//             user2.setPassword(passwordEncoder.encode("user123"));
//             user2.setRole("USER");
//             userRepository.save(user2);
//             System.out.println("✅ Default User2 created: user2 / user123");
//         }
//     }
// }