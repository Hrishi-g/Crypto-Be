package com.practice.firstapp.repo;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.practice.firstapp.dto.PortfolioDto;
import com.practice.firstapp.vo.Portfolio;

public interface PortfolioRepo extends JpaRepository<Portfolio, Long> {

    List<PortfolioDto> findByUserId(Long userId);

    Optional<Portfolio> findByUserIdAndAsset(Long userId, String asset);

}
