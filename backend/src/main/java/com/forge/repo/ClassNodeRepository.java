package com.forge.repo;

import com.forge.model.entity.ClassNodeEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassNodeRepository extends JpaRepository<ClassNodeEntity, Long> {
  List<ClassNodeEntity> findByRepoId(Long repoId);
}
