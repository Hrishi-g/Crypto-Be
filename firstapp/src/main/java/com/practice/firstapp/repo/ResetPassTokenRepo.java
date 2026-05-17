
package com.practice.firstapp.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.practice.firstapp.vo.ResetPassToken;

@Repository
public interface ResetPassTokenRepo extends JpaRepository<ResetPassToken, Long> {

    Optional<ResetPassToken> findByToken(String token);

}