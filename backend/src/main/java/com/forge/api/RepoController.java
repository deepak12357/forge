package com.forge.api;

import com.forge.model.dto.RepoIngestRequest;
import com.forge.service.RepoIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/repo")
public class RepoController {
  private final RepoIngestionService repoIngestionService;

  public RepoController(RepoIngestionService repoIngestionService) {
    this.repoIngestionService = repoIngestionService;
  }

  @PostMapping("/ingest")
  public ResponseEntity<String> ingest(@RequestBody RepoIngestRequest request) {

    repoIngestionService.ingest(request.getGitUrl());

    return ResponseEntity.ok("Repo ingestion triggered");
  }
}
