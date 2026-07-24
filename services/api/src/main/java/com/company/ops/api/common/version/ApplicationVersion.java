package com.company.ops.api.common.version;

import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.stereotype.Component;

@Component
public class ApplicationVersion {

  private static final String DEVELOPMENT_VERSION = "dev";

  private final String productVersion;
  private final String commitId;
  private final String buildTime;

  public ApplicationVersion(
      @Value("${ops.version:}") String configuredVersion,
      @Value("${ops.commit:}") String configuredCommit,
      @Value("${ops.build-time:}") String configuredBuildTime,
      Optional<BuildProperties> buildProperties,
      Optional<GitProperties> gitProperties) {
    this.productVersion = firstNonBlank(
        configuredVersion,
        buildProperties.map(BuildProperties::getVersion).orElse(""),
        DEVELOPMENT_VERSION);
    this.commitId = normalizeCommit(firstNonBlank(
        configuredCommit,
        gitProperties.map(GitProperties::getShortCommitId).orElse("")));
    this.buildTime = firstNonBlank(
        configuredBuildTime,
        buildProperties.map(BuildProperties::getTime).map(Instant::toString).orElse(""));
  }

  public String getProductVersion() {
    return productVersion;
  }

  public String getCommitId() {
    return commitId;
  }

  public String getBuildTime() {
    return buildTime;
  }

  public String getDisplayVersion() {
    return commitId.isBlank() ? productVersion : productVersion + "+" + commitId;
  }

  private static String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return "";
  }

  private static String normalizeCommit(String commit) {
    String value = commit == null ? "" : commit.trim();
    if (value.matches("[0-9a-fA-F]{9,40}")) {
      return value.substring(0, 8);
    }
    return value;
  }
}
