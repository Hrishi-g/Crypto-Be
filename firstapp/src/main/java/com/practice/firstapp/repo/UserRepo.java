package com.practice.firstapp.repo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.practice.firstapp.vo.Users;

@Repository
public interface UserRepo extends JpaRepository<Users, Long> {
    Optional<Users> findByUsername(String username);

    Optional<Users> findByEmail(String identifier);

    Optional<Users> findByUsernameOrEmail(String username, String email);

    Optional<Users> findByProviderAndProviderId(String provider, String providerId);
}
