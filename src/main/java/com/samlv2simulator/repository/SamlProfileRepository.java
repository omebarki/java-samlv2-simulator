package com.samlv2simulator.repository;

import com.samlv2simulator.model.SamlProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SamlProfileRepository extends JpaRepository<SamlProfile, Long> {
    List<SamlProfile> findAllByOrderByUpdatedAtDesc();
}
