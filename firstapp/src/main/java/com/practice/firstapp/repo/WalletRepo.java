package com.practice.firstapp.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.practice.firstapp.vo.Wallet;

@Repository
public interface WalletRepo extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserId(Long userId);
}
