package com.kanimc.repository;

import com.kanimc.entity.SoftwareSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SoftwareSettingRepository extends JpaRepository<SoftwareSetting, Long> {
}
