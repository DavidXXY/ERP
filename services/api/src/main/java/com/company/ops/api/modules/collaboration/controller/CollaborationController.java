package com.company.ops.api.modules.collaboration.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.collaboration.dto.CollaborationDtos.*;
import com.company.ops.api.modules.collaboration.service.CollaborationService;
import com.company.ops.api.modules.collaboration.service.CollaborationGovernanceService;
import jakarta.validation.Valid;
import java.util.*; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;

@RestController @RequestMapping("/api/collaboration")
public class CollaborationController {
  private final CollaborationService service;
  private final CollaborationGovernanceService governance;
  public CollaborationController(CollaborationService service,CollaborationGovernanceService governance){this.service=service;this.governance=governance;}
  @GetMapping("/overview") @PreAuthorize("hasAnyAuthority('dashboard:view','project:view','office:approval:view')")
  public ApiResponse<Map<String,Object>> overview(@RequestParam(required=false) Integer year,@RequestParam(required=false) Integer month,@RequestParam(required=false) UUID departmentId){return ApiResponse.ok(service.overview(year,month,departmentId));}
  @GetMapping("/todos") @PreAuthorize("hasAnyAuthority('dashboard:view','project:view','office:approval:view')")
  public ApiResponse<List<Map<String,Object>>> todos(){return ApiResponse.ok(service.todos());}
  @GetMapping("/references") @PreAuthorize("hasAnyAuthority('project:view','system:user:view')")
  public ApiResponse<List<Map<String,Object>>> references(){return ApiResponse.ok(service.references());}
  @PostMapping("/responsibilities") @PreAuthorize("hasAnyAuthority('project:create','system:organization:update')")
  public ApiResponse<?> responsibility(@Valid @RequestBody ResponsibilityRequest request){return ApiResponse.ok(service.saveResponsibility(request));}
  @PostMapping("/handovers") @PreAuthorize("hasAnyAuthority('crm:contract:update','project:stage:update')")
  public ApiResponse<?> handover(@Valid @RequestBody HandoverRequest request){return ApiResponse.ok(service.createHandover(request));}
  @PostMapping("/handovers/{id}/accept") @PreAuthorize("hasAuthority('project:stage:update')")
  public ApiResponse<?> accept(@PathVariable UUID id,@Valid @RequestBody AcceptHandoverRequest request){return ApiResponse.ok(service.acceptHandover(id,request));}
  @PostMapping(value="/handovers/{id}/materials",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @PreAuthorize("hasAnyAuthority('crm:contract:update','project:stage:update')")
  public ApiResponse<?> material(@PathVariable UUID id,@RequestPart MultipartFile file){return ApiResponse.ok(service.uploadHandoverMaterial(id,file));}
  @GetMapping("/handovers/{id}/materials") @PreAuthorize("hasAnyAuthority('project:view','crm:contract:view')")
  public ApiResponse<?> materials(@PathVariable UUID id){return ApiResponse.ok(service.handoverMaterials(id));}
  @PostMapping("/staff") @PreAuthorize("hasAnyAuthority('hr:employee:manage','project:stage:update')")
  public ApiResponse<?> staff(@Valid @RequestBody StaffAssignmentRequest request){return ApiResponse.ok(service.assignStaff(request));}
  @PutMapping("/staff/{id}/hours") @PreAuthorize("hasAnyAuthority('hr:employee:manage','project:cost:create')")
  public ApiResponse<?> hours(@PathVariable UUID id,@Valid @RequestBody StaffHoursRequest request){return ApiResponse.ok(service.recordHours(id,request));}
  @PostMapping("/todos/{type}/{id}/actions") @PreAuthorize("hasAnyAuthority('dashboard:view','project:view','office:approval:view')")
  public ApiResponse<?> todoAction(@PathVariable String type,@PathVariable UUID id,@Valid @RequestBody TodoActionRequest request){return ApiResponse.ok(governance.actOnTodo(type,id,request));}
  @PostMapping("/timesheets") @PreAuthorize("hasAnyAuthority('project:view','hr:employee:manage','project:cost:create')")
  public ApiResponse<?> submitTimesheet(@Valid @RequestBody TimesheetSubmitRequest request){return ApiResponse.ok(governance.submitTimesheet(request));}
  @PostMapping("/timesheets/{id}/review") @PreAuthorize("hasAnyAuthority('hr:employee:manage','project:cost:create')")
  public ApiResponse<?> reviewTimesheet(@PathVariable UUID id,@Valid @RequestBody ReviewRequest request){return ApiResponse.ok(governance.reviewTimesheet(id,request));}
  @PostMapping("/timesheet-period-locks") @PreAuthorize("hasAnyAuthority('finance:view','hr:employee:manage')")
  public ApiResponse<?> lockPeriod(@Valid @RequestBody PeriodLockRequest request){return ApiResponse.ok(governance.lockPeriod(request));}
  @DeleteMapping("/timesheet-period-locks/{yearMonth}") @PreAuthorize("hasAnyAuthority('finance:view','hr:employee:manage')")
  public ApiResponse<?> unlockPeriod(@PathVariable String yearMonth,@RequestParam(required=false) String comment){governance.unlockPeriod(yearMonth,comment);return ApiResponse.ok(null);}
  @PostMapping("/budget-changes") @PreAuthorize("hasAnyAuthority('project:stage:update','project:cost:create')")
  public ApiResponse<?> budgetChange(@Valid @RequestBody BudgetChangeRequest request){return ApiResponse.ok(governance.requestBudgetChange(request));}
  @PostMapping("/budget-changes/{id}/review") @PreAuthorize("hasAnyAuthority('finance:view','project:cost:create')")
  public ApiResponse<?> reviewBudget(@PathVariable UUID id,@Valid @RequestBody ReviewRequest request){return ApiResponse.ok(governance.reviewBudgetChange(id,request));}
  @GetMapping(value="/export",produces="text/csv;charset=UTF-8") @PreAuthorize("hasAuthority('dashboard:view')")
  public ResponseEntity<byte[]> export(@RequestParam(required=false) Integer year,@RequestParam(required=false) Integer month,@RequestParam(required=false) UUID departmentId){
    byte[] bytes=("\uFEFF"+service.exportCsv(year,month,departmentId)).getBytes(StandardCharsets.UTF_8);
    return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=collaboration-dashboard.csv").contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(bytes);
  }
}
