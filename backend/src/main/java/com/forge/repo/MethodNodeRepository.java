package com.forge.repo;

import com.forge.model.entity.MethodNodeEntity;
import com.forge.model.entity.ClassNodeEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MethodNodeRepository extends JpaRepository<MethodNodeEntity, Long> {
  List<MethodNodeEntity> findByClassNodeIn(List<ClassNodeEntity> classNodes);
}
