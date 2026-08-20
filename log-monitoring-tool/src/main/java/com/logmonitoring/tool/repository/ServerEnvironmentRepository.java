package com.logmonitoring.tool.repository;

import com.logmonitoring.tool.model.ServerEnvironment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServerEnvironmentRepository extends JpaRepository<ServerEnvironment, Long> {
}