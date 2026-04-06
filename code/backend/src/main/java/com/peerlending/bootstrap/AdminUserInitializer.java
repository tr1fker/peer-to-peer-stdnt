package com.peerlending.bootstrap;

import com.peerlending.persistence.entity.RoleEntity;
import com.peerlending.persistence.entity.StudentProfileEntity;
import com.peerlending.persistence.entity.UserEntity;
import com.peerlending.persistence.repository.RoleRepository;
import com.peerlending.persistence.repository.StudentProfileRepository;
import com.peerlending.persistence.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
@Profile("!test")
public class AdminUserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin-email:admin@peerlending.local}")
    private String adminEmail;

    @Value("${app.bootstrap.admin-password:Admin123!}")
    private String adminPassword;

    public AdminUserInitializer(
            UserRepository userRepository,
            RoleRepository roleRepository,
            StudentProfileRepository studentProfileRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmailIgnoreCase(adminEmail)) {
            return;
        }
        UserEntity admin = new UserEntity();
        admin.setEmail(adminEmail.toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setEnabled(true);
        admin.setEmailVerified(true);
        Set<RoleEntity> roles = new HashSet<>();
        roleRepository.findByName("ROLE_ADMIN").ifPresent(roles::add);
        roleRepository.findByName("ROLE_BORROWER").ifPresent(roles::add);
        roleRepository.findByName("ROLE_LENDER").ifPresent(roles::add);
        admin.setRoles(roles);
        admin = userRepository.save(admin);

        StudentProfileEntity profile = new StudentProfileEntity();
        profile.setUser(admin);
        profile.setFullName("Администратор");
        studentProfileRepository.save(profile);
    }
}
