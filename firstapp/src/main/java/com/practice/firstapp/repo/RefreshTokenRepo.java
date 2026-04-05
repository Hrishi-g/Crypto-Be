package com.practice.firstapp.repo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.practice.firstapp.vo.Refresh_token;
import com.practice.firstapp.vo.Users;

@Repository
public interface RefreshTokenRepo extends JpaRepository<Refresh_token, Long> {
    Optional<Refresh_token> findByToken(String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM Refresh_token r WHERE r.token = :token")
    void deleteByToken(@Param("token") String token);

    Optional<Refresh_token> findByUser(Users user);
}
