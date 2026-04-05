package com.practice.firstapp.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.practice.firstapp.vo.Transaction;

@Repository
public interface TransactionRepo extends JpaRepository<Transaction, Long> {

}
