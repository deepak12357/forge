package com.forge.repo;

import com.forge.model.entity.RepoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepoRepository extends JpaRepository<RepoEntity, Long> {}
