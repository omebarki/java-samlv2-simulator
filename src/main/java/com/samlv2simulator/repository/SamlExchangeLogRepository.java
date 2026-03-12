package com.samlv2simulator.repository;

import com.samlv2simulator.model.SamlExchangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SamlExchangeLogRepository extends JpaRepository<SamlExchangeLog, Long> {
    List<SamlExchangeLog> findTop20ByOrderByTimestampDesc();
    List<SamlExchangeLog> findAllByOrderByTimestampDesc();
}
