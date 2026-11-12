package com.kanimc.repository;

import com.kanimc.entity.AccountGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountGroupRepository extends JpaRepository<AccountGroup, Long> {
    java.util.Optional<AccountGroup> findByGroupCode(Integer groupCode);
}
