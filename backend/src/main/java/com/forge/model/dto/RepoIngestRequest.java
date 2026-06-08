package com.forge.model.dto;

public class RepoIngestRequest {
  private String gitUrl;

  public RepoIngestRequest() {}

  public String getGitUrl() {
    return gitUrl;
  }

  public void setGitUrl(String gitUrl) {
    this.gitUrl = gitUrl;
  }
}
