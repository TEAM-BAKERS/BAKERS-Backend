package com.example.bakersbackend.domain.running.repository;

import com.example.bakersbackend.domain.running.entity.Running;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunningRepository extends JpaRepository<Running, Long> {
}