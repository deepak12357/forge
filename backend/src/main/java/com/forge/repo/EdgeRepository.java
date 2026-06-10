package com.forge.repo;

import com.forge.model.entity.EdgeEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EdgeRepository extends JpaRepository<EdgeEntity, Long> {
  List<EdgeEntity> findByRepoId(Long repoId);
}
