package com.company.ops.api.modules.system.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.Attachment;
import com.company.ops.api.modules.qualification.service.QualificationAttachmentService;
import com.company.ops.api.modules.system.dto.PersonalSettingsDtos.AccountProfile;
import com.company.ops.api.modules.system.dto.PersonalSettingsDtos.ChangeMyPasswordRequest;
import com.company.ops.api.modules.system.dto.PersonalSettingsDtos.MyCertificateRequest;
import com.company.ops.api.modules.system.dto.PersonalSettingsDtos.MyCertificateResponse;
import com.company.ops.api.modules.system.dto.PersonalSettingsDtos.PersonalOverviewResponse;
import com.company.ops.api.modules.system.dto.PersonalSettingsDtos.UpdateMyProfileRequest;
import com.company.ops.api.modules.system.dto.MfaDtos.BeginMfaSetupRequest;
import com.company.ops.api.modules.system.dto.MfaDtos.ConfirmMfaResponse;
import com.company.ops.api.modules.system.dto.MfaDtos.MfaSetupResponse;
import com.company.ops.api.modules.system.dto.MfaDtos.MfaStatusResponse;
import com.company.ops.api.modules.system.dto.MfaDtos.ProtectedMfaRequest;
import com.company.ops.api.modules.system.dto.MfaDtos.VerifyMfaRequest;
import com.company.ops.api.modules.system.security.UserPrincipal;
import com.company.ops.api.modules.system.service.MfaService;
import com.company.ops.api.modules.system.service.PersonalSettingsService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/personal")
public class PersonalSettingsController {
  private final PersonalSettingsService personalSettingsService;
  private final QualificationAttachmentService attachmentService;
  private final MfaService mfaService;

  public PersonalSettingsController(
      PersonalSettingsService personalSettingsService,
      QualificationAttachmentService attachmentService,
      MfaService mfaService
  ) {
    this.personalSettingsService = personalSettingsService;
    this.attachmentService = attachmentService;
    this.mfaService = mfaService;
  }

  @GetMapping
  public ApiResponse<PersonalOverviewResponse> overview(@AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.ok(personalSettingsService.overview(principal.id()));
  }

  @PutMapping("/profile")
  public ApiResponse<AccountProfile> updateProfile(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody UpdateMyProfileRequest request
  ) {
    return ApiResponse.ok(personalSettingsService.updateProfile(principal.id(), request));
  }

  @PutMapping("/password")
  public ApiResponse<Void> changePassword(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody ChangeMyPasswordRequest request
  ) {
    personalSettingsService.changePassword(principal.id(), request);
    return ApiResponse.ok();
  }

  @GetMapping("/mfa")
  public ApiResponse<MfaStatusResponse> mfaStatus(@AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.ok(mfaService.status(principal.id()));
  }

  @PostMapping("/mfa/setup")
  public ApiResponse<MfaSetupResponse> beginMfaSetup(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody BeginMfaSetupRequest request
  ) {
    return ApiResponse.ok(mfaService.beginSetup(principal.id(), request.currentPassword()));
  }

  @PostMapping("/mfa/enable")
  public ApiResponse<ConfirmMfaResponse> enableMfa(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody VerifyMfaRequest request
  ) {
    return ApiResponse.ok(mfaService.enable(principal.id(), request.code()));
  }

  @PostMapping("/mfa/recovery-codes")
  public ApiResponse<ConfirmMfaResponse> regenerateMfaRecoveryCodes(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody ProtectedMfaRequest request
  ) {
    return ApiResponse.ok(mfaService.regenerateRecoveryCodes(
        principal.id(), request.currentPassword(), request.code()));
  }

  @PostMapping("/mfa/disable")
  public ApiResponse<Void> disableMfa(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody ProtectedMfaRequest request
  ) {
    mfaService.disable(principal.id(), request.currentPassword(), request.code());
    return ApiResponse.ok();
  }

  @PostMapping("/certificates")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<MyCertificateResponse> createCertificate(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody MyCertificateRequest request
  ) {
    return ApiResponse.ok(personalSettingsService.createCertificate(principal.id(), request));
  }

  @PutMapping("/certificates/{id}")
  public ApiResponse<MyCertificateResponse> updateCertificate(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable UUID id,
      @Valid @RequestBody MyCertificateRequest request
  ) {
    return ApiResponse.ok(personalSettingsService.updateCertificate(principal.id(), id, request));
  }

  @DeleteMapping("/certificates/{id}")
  public ApiResponse<Void> deleteCertificate(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable UUID id
  ) {
    personalSettingsService.deleteCertificate(principal.id(), id);
    return ApiResponse.ok();
  }

  @PostMapping(value = "/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Attachment> uploadAttachment(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestPart MultipartFile file
  ) {
    personalSettingsService.assertEmployeeLinked(principal.id());
    return ApiResponse.ok(attachmentService.store(file));
  }
}
