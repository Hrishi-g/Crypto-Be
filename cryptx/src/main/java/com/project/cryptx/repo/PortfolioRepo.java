package com.project.cryptx.repo;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.cryptx.dto.PortfolioDto;
import com.project.cryptx.vo.Portfolio;

public interface PortfolioRepo extends JpaRepository<Portfolio, Long> {

    List<PortfolioDto> findByUserId(Long userId);

    Optional<Portfolio> findByUserIdAndAsset(Long userId, String asset);

}
