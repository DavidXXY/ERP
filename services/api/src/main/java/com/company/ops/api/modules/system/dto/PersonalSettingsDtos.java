package com.company.ops.api.modules.system.dto;

import com.company.ops.api.modules.qualification.dto.QualificationDtos.Attachment;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class PersonalSettingsDtos {
  private PersonalSettingsDtos() {}

  public record PersonalOverviewResponse(
      AccountProfile account,
      EmployeeProfile employee,
      List<MyCertificateResponse> certificates,
      List<MyContractResponse> contracts
  ) {}

  public record AccountProfile(
      UUID id, String username, String displayName, String phone, String email,
      String organizationName, boolean enabled
  ) {}

  public record EmployeeProfile(
      UUID id, String name, String workNo, String organizationName, String organizationPath,
      String position, LocalDate entryDate, String employmentStatus
  ) {}

  public record UpdateMyProfileRequest(
      @NotBlank @Size(max = 80) String displayName,
      @Size(max = 40) String phone,
      @Email @Size(max = 120) String email
  ) {}

  public record ChangeMyPasswordRequest(
      @NotBlank String currentPassword,
      @NotBlank @Size(min = 8, max = 100) String newPassword
  ) {}

  public record MyCertificateRequest(
      @NotBlank @Size(max = 200) String name,
      @Size(max = 100) String type,
      @Size(max = 180) String certificateNo,
      @Size(max = 180) String specialty,
      LocalDate issueDate,
      LocalDate validTo,
      LocalDate reviewDate,
      List<Attachment> attachments,
      @Size(max = 1000) String remark
  ) {}

  public record MyCertificateResponse(
      UUID id, String name, String type, String certificateNo, String specialty,
      LocalDate issueDate, LocalDate validTo, LocalDate reviewDate,
      boolean companyRegistered, boolean availableForTender, boolean locked,
      String status, Long daysLeft, List<Attachment> attachments, String remark
  ) {}

  public record MyContractResponse(
      UUID id, String contractNo, String contractType, LocalDate signDate,
      LocalDate startDate, LocalDate endDate, LocalDate probationEndDate,
      String status, List<Attachment> attachments, String remark
  ) {}
}
