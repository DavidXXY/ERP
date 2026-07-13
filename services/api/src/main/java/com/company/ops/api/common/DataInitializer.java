package com.company.ops.api.common;

import com.company.ops.api.modules.system.domain.SystemPermission;
import com.company.ops.api.modules.system.domain.SystemOrganization;
import com.company.ops.api.modules.system.domain.SystemRole;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.repository.SystemPermissionRepository;
import com.company.ops.api.modules.system.repository.SystemOrganizationRepository;
import com.company.ops.api.modules.system.repository.SystemRoleRepository;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
  private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
  private static final String[][] PERMISSIONS = {
    {"crm:dashboard:view","CRM仪表盘查看","crm"},
    {"crm:customer:view","客户查看","crm"},{"crm:customer:create","客户创建","crm"},{"crm:customer:update","客户修改","crm"},{"crm:customer:delete","客户删除","crm"},
    {"crm:opportunity:view","商机查看","crm"},{"crm:opportunity:create","商机创建","crm"},{"crm:opportunity:update","商机修改","crm"},{"crm:opportunity:delete","商机删除","crm"},
    {"crm:quote:view","报价查看","crm"},{"crm:quote:create","报价创建","crm"},{"crm:quote:update","报价修改","crm"},{"crm:quote:delete","报价删除","crm"},{"crm:quote:submit","报价提交审批","crm"},{"crm:quote:approve","报价审批","crm"},{"crm:quote:customer-result","客户结果登记","crm"},{"crm:quote:convert","转合同","crm"},
    {"crm:contract:view","合同查看","crm"},{"crm:contract:create","合同创建","crm"},{"crm:contract:update","合同修改","crm"},{"crm:contract:delete","合同删除","crm"},
    {"crm:followup:view","跟进查看","crm"},{"crm:followup:create","跟进创建","crm"},{"crm:followup:delete","跟进删除","crm"},
    {"crm:renewal:view","续约查看","crm"},
    {"crm:receivable:view","应收查看","crm"},{"crm:receivable:invoice","开票登记","crm"},{"crm:receivable:settle","回款核销","crm"},{"crm:receivable:update","应收修改","crm"},
    {"crm:profile:view","客户画像","crm"},
    {"procurement:view","采购查看","procurement"},{"procurement:supplier:create","供应商创建","procurement"},{"procurement:purchase:create","采购创建","procurement"},{"procurement:request:approve","采购审批","procurement"},{"procurement:order:receive","采购收货","procurement"},{"procurement:payable:view","应付查看","procurement"},
    {"inventory:view","库存查看","inventory"},{"inventory:part:create","物料创建","inventory"},{"inventory:movement:create","库存移动","inventory"},{"inventory:issue:create","领料创建","inventory"},{"inventory:return:create","退料创建","inventory"},
    {"project:view","项目查看","project"},{"project:create","项目创建","project"},{"project:approve","项目审批","project"},
    {"office:view","OA查看","office"},{"office:approval:view","审批查看","office"},{"office:approval:create","审批创建","office"},{"office:approval:process","审批处理","office"},
    {"office:expense:view","报销查看","office"},{"office:expense:create","报销创建","office"},
    {"office:outsource:view","外包查看","office"},{"office:outsource:create","外包创建","office"},{"office:outsource:complete","外包完成","office"},
    {"office:document:view","文档查看","office"},{"office:document:upload","文档上传","office"},{"office:document:delete","文档删除","office"},
    {"office:notification:view","消息查看","office"},{"office:audit:view","审计查看","office"},
    {"qualification:view","资质查看","qualification"},{"qualification:company:view","公司资质查看","qualification"},{"qualification:company:manage","公司资质管理","qualification"},
    {"qualification:employee:view","人员资质查看","qualification"},{"qualification:employee:manage","人员资质管理","qualification"},
    {"qualification:certificate:view","证书查看","qualification"},{"qualification:certificate:manage","证书管理","qualification"},
    {"qualification:tender:view","投标查看","qualification"},{"qualification:warning:view","预警查看","qualification"},
    {"finance:view","财务查看","finance"},{"finance:receivable:view","财务应收查看","finance"},{"finance:receivable:invoice","财务开票","finance"},{"finance:receivable:collect","财务收款","finance"},
    {"finance:payable:view","应付查看","finance"},{"finance:payment:apply","付款申请","finance"},{"finance:payment:approve","付款审批","finance"},{"finance:payment:execute","付款执行","finance"},
    {"finance:ledger:view","总账查看","finance"},
    {"system:view","系统查看","system"},{"system:user:view","用户查看","system"},{"system:user:create","用户创建","system"},{"system:user:update","用户修改","system"},{"system:user:delete","用户删除","system"},{"system:user:reset-password","密码重置","system"},
    {"system:role:view","角色查看","system"},{"system:role:create","角色创建","system"},{"system:role:update","角色修改","system"},{"system:role:delete","角色删除","system"},
    {"system:permission:view","权限查看","system"},{"system:permission:create","权限创建","system"},{"system:permission:update","权限修改","system"},{"system:permission:delete","权限删除","system"},
    {"system:organization:view","组织查看","system"},{"system:organization:create","组织创建","system"},{"system:organization:update","组织修改","system"},{"system:organization:delete","组织删除","system"},
    {"dashboard:view","驾驶舱查看","dashboard"},
    {"risk:view","风险中心查看","risk"},{"risk:update","风险处理","risk"},
    {"workforce:view","排班查看","hr"},
    {"hr:employee:view","员工查看","hr"},{"hr:employee:manage","员工管理","hr"},
    {"maintenance:view","维护查看","maintenance"},{"maintenance:plan:manage","维护计划管理","maintenance"},{"maintenance:order:manage","工单管理","maintenance"},
    {"maintenance:order:delete","工单删除","maintenance"},{"maintenance:certificate:view","人员证书查看","maintenance"},{"maintenance:schedule:view","人员排班查看","maintenance"},
  };
  private final SystemUserRepository userRepo;
  private final SystemRoleRepository roleRepo;
  private final SystemPermissionRepository permRepo;
  private final SystemOrganizationRepository orgRepo;
  private final PasswordEncoder encoder;
  
  public DataInitializer(SystemUserRepository userRepo, SystemRoleRepository roleRepo, SystemPermissionRepository permRepo, SystemOrganizationRepository orgRepo, PasswordEncoder encoder) {
    this.userRepo = userRepo; this.roleRepo = roleRepo; this.permRepo = permRepo; this.orgRepo = orgRepo; this.encoder = encoder;
  }
  
  @Override
  public void run(String... args) {
    // Check if permissions exist. If not, create all permissions and ADMIN role + user
    // Always check user first (perm table might not exist yet on fresh H2)
    if (userRepo.existsByUsername("admin")) {
      // Admin already exists - still ensure any missing permissions are seeded
      seedMissingPermissions();
      grantAllPermissionsToAdmin();
      log.info("Admin user already exists, skipping data initialization");
      return;
    }
    
    log.info("Seeding permissions and admin user...");
    String[][] perms = PERMISSIONS;
    
    // Create permissions
    for (String[] p : perms) {
      SystemPermission sp = new SystemPermission();
      sp.setCode(p[0]); sp.setName(p[1]); sp.setModule(p[2]); sp.setBuiltIn(false);
      permRepo.save(sp);
    }
    log.info("Created {} permissions", perms.length);
    
    // Create ADMIN role with ALL permissions
    SystemRole adminRole = roleRepo.findByCodeAndTenantId("ADMIN", "default").orElseGet(() -> {
      SystemRole r = new SystemRole();
      r.setCode("ADMIN"); r.setName("系统管理员"); r.setDataScope("ALL"); r.setBuiltIn(true);
      roleRepo.save(r);
      log.info("Empty ADMIN role created, will assign permissions");
      return r;
    });
    // Grant all permissions to ADMIN role (outside lambda to avoid effectively-final issue)
    var allPerms = permRepo.findAll();
    adminRole.getPermissions().addAll(allPerms);
    roleRepo.save(adminRole);
    log.info("Granted {} permissions to ADMIN role", allPerms.size());
    
    // Create admin user
    if (!userRepo.existsByUsername("admin")) {
      SystemUser admin = new SystemUser();
      admin.setUsername("admin"); admin.setDisplayName("系统管理员");
      admin.setPasswordHash(encoder.encode("Admin@123"));
      admin.setPhone("13800000000"); admin.setEmail("admin@company.com");
      admin.setEnabled(true); admin.getRoles().add(adminRole);
      userRepo.save(admin);
      log.info("Admin user created (admin/Admin@123)");
    }
    
    // === Organization Structure ===
    if (orgRepo.count() == 0) {
      log.info("Creating organization structure...");
      SystemOrganization root = new SystemOrganization();
      root.setCode("ROOT"); root.setName("工程运维公司"); root.setType("COMPANY"); root.setEnabled(true); root.setSortOrder(0);
      root = orgRepo.save(root);
      
      SystemOrganization deptExec = createOrg("EXEC_OFFICE", "总经办", "DEPARTMENT", 1, root);
      SystemOrganization deptFinance = createOrg("FINANCE", "财务部", "DEPARTMENT", 2, root);
      SystemOrganization deptHr = createOrg("HR", "人事行政部", "DEPARTMENT", 3, root);
      SystemOrganization deptTech = createOrg("TECH", "技术研发部", "DEPARTMENT", 4, root);
      SystemOrganization deptSales = createOrg("SALES", "市场销售部", "DEPARTMENT", 5, root);
      SystemOrganization deptProc = createOrg("PROCUREMENT", "采购部", "DEPARTMENT", 6, root);
      SystemOrganization deptProj = createOrg("PROJECT", "项目部", "DEPARTMENT", 7, root);
      SystemOrganization deptOps = createOrg("OPS", "运维服务部", "DEPARTMENT", 8, root);
      
      createOrg("ACCOUNTING", "会计组", "TEAM", 1, deptFinance);
      createOrg("CASHIER", "出纳组", "TEAM", 2, deptFinance);
      createOrg("FRONTEND", "前端组", "TEAM", 1, deptTech);
      createOrg("BACKEND", "后端组", "TEAM", 2, deptTech);
      createOrg("PROJ_A", "项目一组", "TEAM", 1, deptProj);
      createOrg("PROJ_B", "项目二组", "TEAM", 2, deptProj);
      log.info("Created {} departments with sub-teams", orgRepo.count());
    }
    
    // === Roles (beyond ADMIN) ===
    if (!roleRepo.findByCodeAndTenantId("SALES", "default").isPresent()) {
      log.info("Creating standard roles...");
      Map<String, SystemPermission> permMap = new HashMap<>();
      permRepo.findAll().forEach(p -> permMap.put(p.getCode(), p));
      
      createRole("SALES", "销售经理", "DEPT_AND_SUB", permMap,
        "dashboard:view", "crm:dashboard:view",
        "crm:customer:view", "crm:customer:create", "crm:customer:update",
        "crm:opportunity:view", "crm:opportunity:create", "crm:opportunity:update",
        "crm:quote:view", "crm:quote:create", "crm:quote:update", "crm:quote:submit", "crm:quote:approve", "crm:quote:customer-result", "crm:quote:convert",
        "crm:contract:view", "crm:followup:view", "crm:followup:create",
        "crm:renewal:view", "crm:receivable:view", "crm:profile:view");
      
      createRole("FINANCE", "财务专员", "ALL", permMap,
        "dashboard:view", "finance:view",
        "finance:receivable:view", "finance:receivable:invoice", "finance:receivable:collect",
        "finance:payable:view", "finance:payment:apply", "finance:payment:approve", "finance:payment:execute",
        "finance:ledger:view", "crm:receivable:view", "crm:receivable:invoice", "crm:receivable:settle",
        "procurement:payable:view");
      
      createRole("HR", "人事专员", "ALL", permMap,
        "dashboard:view", "qualification:view",
        "qualification:employee:view", "qualification:employee:manage",
        "qualification:certificate:view", "qualification:certificate:manage",
        "workforce:view");
      
      createRole("PROCUREMENT", "采购专员", "DEPT_AND_SUB", permMap,
        "dashboard:view", "procurement:view",
        "procurement:supplier:create", "procurement:purchase:create", "procurement:request:approve",
        "procurement:order:receive", "procurement:payable:view",
        "inventory:view", "inventory:part:create");
      
      createRole("PROJECT_MGR", "项目经理", "DEPT_AND_SUB", permMap,
        "dashboard:view", "project:view", "project:create", "project:approve",
        "inventory:view", "inventory:issue:create", "inventory:return:create",
        "procurement:view", "office:view", "office:approval:view", "office:approval:create");
      
      createRole("OPS", "运维工程师", "SELF", permMap,
        "dashboard:view", "maintenance:view", "maintenance:plan:manage", "maintenance:order:manage", "maintenance:certificate:view", "maintenance:schedule:view",
        "inventory:view", "inventory:issue:create",
        "office:view", "office:approval:view", "office:approval:create", "office:expense:create");
      
      createRole("VIEWER", "普通员工", "SELF", permMap,
        "dashboard:view", "office:view", "office:expense:create", "office:notification:view");
      
      log.info("Standard roles created");
    }
  }

  private void seedMissingPermissions() {
    for (String[] p : PERMISSIONS) {
      if (!permRepo.existsByCodeAndTenantId(p[0], "default")) {
        SystemPermission sp = new SystemPermission();
        sp.setCode(p[0]); sp.setName(p[1]); sp.setModule(p[2]); sp.setBuiltIn(false);
        permRepo.save(sp);
        log.info("  Added missing permission: {}", p[0]);
      }
    }
  }

  private void grantAllPermissionsToAdmin() {
    roleRepo.findByCodeAndTenantId("ADMIN", "default").ifPresent(adminRole -> {
      adminRole.getPermissions().addAll(permRepo.findAll());
      roleRepo.save(adminRole);
    });
  }
  
  private SystemOrganization createOrg(String code, String name, String type, int sort, SystemOrganization parent) {
    SystemOrganization o = new SystemOrganization();
    o.setCode(code); o.setName(name); o.setType(type); o.setSortOrder(sort); o.setEnabled(true);
    if (parent != null) o.setParent(parent);
    return orgRepo.save(o);
  }
  
  private void createRole(String code, String name, String scope, Map<String, SystemPermission> permMap, String... permCodes) {
    SystemRole r = new SystemRole();
    r.setCode(code); r.setName(name); r.setDataScope(scope); r.setBuiltIn(false);
    r = roleRepo.save(r);
    for (String pc : permCodes) {
      SystemPermission p = permMap.get(pc);
      if (p != null) r.getPermissions().add(p);
    }
    roleRepo.save(r);
    log.info("  Role created: {} ({})", name, permCodes.length + " permissions");
  }
}
