package com.sample_generator.sample.config;

import com.sample_generator.sample.Entity.Role;
import com.sample_generator.sample.Entity.User;
import com.sample_generator.sample.repository.RoleRepository;
import com.sample_generator.sample.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeData(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {


            /* ================================================
             * CREATE ALL ROLES
             * ================================================ */

            Role adminRole = getOrCreateRole(roleRepository, "ADMIN");

            Role userRole = getOrCreateRole(roleRepository, "USER");

            Role salesTeamRole = getOrCreateRole(roleRepository, "SALES_TEAM");

            Role researchTeamRole = getOrCreateRole(roleRepository, "RESEARCH_TEAM");

            Role digitalMarketingRole = getOrCreateRole(roleRepository, "DIGITAL_MARKETING");


            /* ================================================
             * CREATE ADMIN USER
             * ================================================ */

            if (!userRepository.existsByUsername("admin@spherical.com")) {

                User admin = new User();

                admin.setUsername("admin@spherical.com");

                admin.setPassword(
                        passwordEncoder.encode("admin123")
                );

                admin.setEnabled(true);

                admin.setRoles(new HashSet<>(Set.of(adminRole)));

                userRepository.save(admin);

                System.out.println("✓ Admin user created: admin@spherical.com / admin123");
            }


            /* ================================================
             * KEEP LEGACY ADMIN (backward compatibility)
             * ================================================ */

            if (!userRepository.existsByUsername("admin@aayush.com")) {

                User admin = new User();

                admin.setUsername("admin@aayush.com");

                admin.setPassword(
                        passwordEncoder.encode("admin123")
                );

                admin.setEnabled(true);

                admin.setRoles(new HashSet<>(Set.of(adminRole)));

                userRepository.save(admin);

                System.out.println("✓ Legacy admin user created: admin@aayush.com / admin123");
            }


            /* ================================================
             * CREATE SALES TEAM USER
             * ================================================ */

            if (!userRepository.existsByUsername("sales@spherical.com")) {

                User salesUser = new User();

                salesUser.setUsername("sales@spherical.com");

                salesUser.setPassword(
                        passwordEncoder.encode("sales123")
                );

                salesUser.setEnabled(true);

                salesUser.setRoles(new HashSet<>(Set.of(salesTeamRole)));

                userRepository.save(salesUser);

                System.out.println("✓ Sales Team user created: sales@spherical.com / sales123");
            }


            /* ================================================
             * CREATE RESEARCH TEAM USER
             * ================================================ */

            if (!userRepository.existsByUsername("research@spherical.com")) {

                User researchUser = new User();

                researchUser.setUsername("research@spherical.com");

                researchUser.setPassword(
                        passwordEncoder.encode("research123")
                );

                researchUser.setEnabled(true);

                researchUser.setRoles(new HashSet<>(Set.of(researchTeamRole)));

                userRepository.save(researchUser);

                System.out.println("✓ Research Team user created: research@spherical.com / research123");
            }


            /* ================================================
             * CREATE DIGITAL MARKETING TEAM USER
             * ================================================ */

            if (!userRepository.existsByUsername("marketing@spherical.com")) {

                User marketingUser = new User();

                marketingUser.setUsername("marketing@spherical.com");

                marketingUser.setPassword(
                        passwordEncoder.encode("marketing123")
                );

                marketingUser.setEnabled(true);

                marketingUser.setRoles(new HashSet<>(Set.of(digitalMarketingRole)));

                userRepository.save(marketingUser);

                System.out.println("✓ Digital Marketing user created: marketing@spherical.com / marketing123");
            }


            /* ================================================
             * CREATE GENERAL USER
             * ================================================ */

            if (!userRepository.existsByUsername("user@spherical.com")) {

                User regularUser = new User();

                regularUser.setUsername("user@spherical.com");

                regularUser.setPassword(
                        passwordEncoder.encode("user123")
                );

                regularUser.setEnabled(true);

                regularUser.setRoles(new HashSet<>(Set.of(userRole)));

                userRepository.save(regularUser);

                System.out.println("✓ Regular user created: user@spherical.com / user123");
            }


            System.out.println("===========================================");
            System.out.println("  DATA INITIALIZATION COMPLETE");
            System.out.println("===========================================");
            System.out.println("  Login Credentials:");
            System.out.println("  Admin:    admin@spherical.com  / admin123");
            System.out.println("  Sales:    sales@spherical.com  / sales123");
            System.out.println("  Research: research@spherical.com / research123");
            System.out.println("  Marketing:marketing@spherical.com / marketing123");
            System.out.println("  User:     user@spherical.com   / user123");
            System.out.println("===========================================");

        };
    }


    private Role getOrCreateRole(
            RoleRepository roleRepository,
            String roleName
    ) {

        return roleRepository
                .findByName(roleName)
                .orElseGet(() -> {

                    Role role = new Role();

                    role.setName(roleName);

                    return roleRepository.save(role);

                });
    }
}