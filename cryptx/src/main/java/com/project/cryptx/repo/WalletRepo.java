package com.project.cryptx.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.cryptx.vo.Wallet;

@Repository
public interface WalletRepo extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserId(Long userId);
}
