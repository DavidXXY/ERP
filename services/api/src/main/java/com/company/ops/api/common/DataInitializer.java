package com.company.ops.api.common;

import com.company.ops.api.modules.system.domain.SystemPermission;
import com.company.ops.api.modules.system.domain.SystemRole;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.repository.SystemPermissionRepository;
import com.company.ops.api.modules.system.repository.SystemRoleRepository;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
  private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
  private final SystemUserRepository userRepo;
  private final SystemRoleRepository roleRepo;
  private final SystemPermissionRepository permRepo;
  private final PasswordEncoder encoder;
  
  public DataInitializer(SystemUserRepository userRepo, SystemRoleRepository roleRepo, SystemPermissionRepository permRepo, PasswordEncoder encoder) {
    this.userRepo = userRepo; this.roleRepo = roleRepo; this.permRepo = permRepo; this.encoder = encoder;
  }
  
  @Override
  public void run(String... args) {
    // Check if permissions exist. If not, create all permissions and ADMIN role + user
    if (permRepo.count() > 0) return;
    
    log.info("Seeding permissions and admin user...");
    String[][] perms = {
      {"crm:dashboard:view","CRM仪表盘查看","crm"},
      {"crm:customer:view","客户查看","crm"},{"crm:customer:create","客户创建","crm"},{"crm:customer:update","客户修改","crm"},{"crm:customer:delete","客户删除","crm"},
      {"crm:opportunity:view","商机查看","crm"},{"crm:opportunity:create","商机创建","crm"},{"crm:opportunity:update","商机修改","crm"},{"crm:opportunity:delete","商机删除","crm"},
      {"crm:quote:view","报价查看","crm"},{"crm:quote:create","报价创建","crm"},{"crm:quote:update","报价修改","crm"},{"crm:quote:delete","报价删除","crm"},{"crm:quote:submit","报价提交审批","crm"},
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
      {"workforce:view","排班查看","hr"},
      {"hr:employee:view","员工查看","hr"},{"hr:employee:manage","员工管理","hr"},
      {"maintenance:view","维护查看","maintenance"},{"maintenance:plan:manage","维护计划管理","maintenance"},{"maintenance:order:manage","工单管理","maintenance"},
    };
    
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
  }
}
