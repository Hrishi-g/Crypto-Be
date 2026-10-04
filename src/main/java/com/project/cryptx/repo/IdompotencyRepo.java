package com.project.cryptx.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.cryptx.dto.IdompotencyDto;
import com.project.cryptx.vo.Idompotency;

@Repository
public interface IdompotencyRepo extends JpaRepository<Idompotency, Long> {

    Optional<IdompotencyDto> findByIdempotencyKey(String idempotencyKey);
}
