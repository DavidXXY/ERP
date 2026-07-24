package com.company.ops.api.common.version;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;

class ApplicationVersionTest {

  @Test
  void usesBuildAndGitPropertiesWhenNoOverridesAreConfigured() {
    Properties buildValues = new Properties();
    buildValues.setProperty("version", "1.2.3");
    buildValues.setProperty("time", "2026-07-24T10:15:30Z");
    Properties gitValues = new Properties();
    gitValues.setProperty("commit.id.abbrev", "abcdef12");

    ApplicationVersion version = new ApplicationVersion(
        "",
        "",
        "",
        Optional.of(new BuildProperties(buildValues)),
        Optional.of(new GitProperties(gitValues)));

    assertThat(version.getProductVersion()).isEqualTo("1.2.3");
    assertThat(version.getCommitId()).isEqualTo("abcdef12");
    assertThat(version.getBuildTime()).isEqualTo(Instant.parse("2026-07-24T10:15:30Z").toString());
    assertThat(version.getDisplayVersion()).isEqualTo("1.2.3+abcdef12");
  }

  @Test
  void configuredValuesOverrideGeneratedBuildInformation() {
    Properties buildValues = new Properties();
    buildValues.setProperty("version", "1.2.3");
    Properties gitValues = new Properties();
    gitValues.setProperty("commit.id", "abcdef1234567890");

    ApplicationVersion version = new ApplicationVersion(
        "2.0.0",
        "1234567890abcdef",
        "2026-07-24T12:00:00Z",
        Optional.of(new BuildProperties(buildValues)),
        Optional.of(new GitProperties(gitValues)));

    assertThat(version.getDisplayVersion()).isEqualTo("2.0.0+12345678");
    assertThat(version.getBuildTime()).isEqualTo("2026-07-24T12:00:00Z");
  }

  @Test
  void fallsBackToDevelopmentVersionWithoutBuildInformation() {
    ApplicationVersion version = new ApplicationVersion(
        "", "", "", Optional.empty(), Optional.empty());

    assertThat(version.getDisplayVersion()).isEqualTo("dev");
    assertThat(version.getBuildTime()).isEmpty();
  }
}
