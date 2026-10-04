
package com.project.cryptx.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.cryptx.vo.ResetPassToken;

@Repository
public interface ResetPassTokenRepo extends JpaRepository<ResetPassToken, Long> {

    Optional<ResetPassToken> findByToken(String token);

}