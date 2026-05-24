package com.practice.firstapp.repo;

import java.util.Optional;

import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.practice.firstapp.dto.TransactionHistory;
import com.practice.firstapp.vo.Transaction;

@Repository
public interface TransactionRepo extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByReferenceId(String referenceId);

    @Query("SELECT new com.practice.firstapp.dto.TransactionHistory(t.type, t.asset, t.createdAt, t.quantity, t.price, t.amount, t.balanceAfter) "
            + "FROM Transaction t "
            + "WHERE t.wallet.user.id = :userId "
            + "ORDER BY t.createdAt DESC")
    Slice<TransactionHistory> findByWalletUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

}

