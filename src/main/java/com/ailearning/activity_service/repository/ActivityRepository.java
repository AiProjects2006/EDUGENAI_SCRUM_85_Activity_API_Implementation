package com.ailearning.activity_service.repository;

import com.ailearning.activity_service.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
}