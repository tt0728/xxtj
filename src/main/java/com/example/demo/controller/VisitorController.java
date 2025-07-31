package com.example.demo.controller;

import com.example.demo.module.Approval;
import com.example.demo.module.Visitor;
import com.example.demo.module.VisitorAccount;
import com.example.demo.service.VisitorService;
import com.example.demo.service.VisitorAccountService;
import com.example.demo.service.MyService;
import com.example.demo.dto.VisitorRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/")
public class VisitorController {

    private final VisitorService visitorService;
    private final VisitorAccountService visitorAccountService; // 注入 VisitorAccountService
    private final MyService myService;

    @Autowired
    public VisitorController(VisitorService visitorService,
            VisitorAccountService visitorAccountService, MyService myService) {
        this.visitorService = visitorService;
        this.visitorAccountService = visitorAccountService; // 初始化 VisitorAccountService
        this.myService = myService;
    }

    // --- 页面路由 ---
    @GetMapping("/")
    public String home() {
        return "index.html";
    }

    //一个示例接口，用于触发异步方法
    @GetMapping("/api/public/trigger-async-task")
    public ResponseEntity<String> triggerAsyncTask() {
        System.out.println("Controller: 正在触发异步任务...");
        myService.doSomethingAsync(); // 【调用】MyService 中的异步方法
        System.out.println("Controller: 异步任务已触发，响应即将返回。");
        return ResponseEntity.ok("异步任务已在后台启动。请查看控制台日志。");
    }

    //另一个示例接口，用于触发同步方法（对比）
    @GetMapping("/api/public/trigger-sync-task")
    public ResponseEntity<String> triggerSyncTask() {
        System.out.println("Controller: 正在触发同步任务...");
        myService.doSomethingSync(); // 【调用】MyService 中的同步方法
        System.out.println("Controller: 同步任务已完成，响应即将返回。");
        return ResponseEntity.ok("同步任务已完成。请查看控制台日志。");
    }

    // 获取访客预约详情 (包含同行人) - 【通常只对管理员开放】
    @GetMapping("/api/admin/visitors/{id}/details")
    @ResponseBody
    public ResponseEntity<Visitor> getVisitorDetails(@PathVariable Long id) {
        return visitorService.getVisitorDetails(id)
                .map(visitor -> new ResponseEntity<>(visitor, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // 审批访客预约 (通过或不通过) - 【通常只对管理员开放】
    @PutMapping("/api/admin/visitors/{id}/review")
    @ResponseBody
    public ResponseEntity<Visitor> reviewVisitor(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String newStatus = payload.get("status");
        String comments = payload.get("comments");
        if (newStatus == null || (!"APPROVED".equalsIgnoreCase(newStatus) && !"REJECTED".equalsIgnoreCase(newStatus))) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return visitorService.approveOrRejectVisitor(id, newStatus, comments)
                .map(visitor -> new ResponseEntity<>(visitor, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // 访客注册接口
    @PostMapping("/api/public/visitor-accounts/register")
    @ResponseBody
    public ResponseEntity<Map<String, String>> registerVisitorAccount(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            return new ResponseEntity<>(Map.of("message", "Username and password cannot be empty"),
                    HttpStatus.BAD_REQUEST);
        }

        VisitorAccount newAccount = visitorAccountService.register(username, password);
        if (newAccount != null) {
            return new ResponseEntity<>(
                    Map.of("message", "Registration successful", "accountId", newAccount.getId().toString()),
                    HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(Map.of("message", "Username already exists"), HttpStatus.CONFLICT); // 409
                                                                                                            // Conflict
        }
    }

    // 访客登录接口
    @PostMapping("/api/public/visitor-accounts/login")
    @ResponseBody
    public ResponseEntity<Map<String, String>> visitorLogin(@RequestBody Map<String, String> payload,
            HttpSession session) {
        String username = payload.get("username");
        String password = payload.get("password");

        VisitorAccount account = visitorAccountService.authenticate(username, password, session);
        if (account != null) {
            return new ResponseEntity<>(Map.of("message", "Login successful"), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(Map.of("message", "Invalid username or password"), HttpStatus.UNAUTHORIZED);
        }
    }

    // 访客登出接口
    @PostMapping("/api/public/visitor-accounts/logout")
    @ResponseBody
    public ResponseEntity<Map<String, String>> visitorLogout(HttpSession session) {
        visitorAccountService.logout(session);
        return new ResponseEntity<>(Map.of("message", "Logout successful"), HttpStatus.OK);
    }

    // 访客预约提交 - 现在需要关联到登录的访客账号
    @PostMapping("/api/public/visitors/register")
    @ResponseBody
    public ResponseEntity<Visitor> registerVisitor(@RequestBody VisitorRequest request, HttpSession session) {
        Long visitorAccountId = visitorAccountService.getCurrentVisitorAccountId(session);
        if (visitorAccountId == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED); // 未登录
        }
        // 将 visitorAccountId 传递给 Service
        Visitor registeredVisitor = visitorService.submitAppointment(request, visitorAccountId); // 修改Service方法
        return new ResponseEntity<>(registeredVisitor, HttpStatus.CREATED);
    }

    // 来访人员查询自己的预约状态 - 只查询当前登录用户的预约
    @GetMapping("/api/public/visitors/my-appointments")
    @ResponseBody
    public ResponseEntity<List<Visitor>> queryMyVisitors(HttpSession session) {
        Long visitorAccountId = visitorAccountService.getCurrentVisitorAccountId(session);
        if (visitorAccountId == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED); // 未登录
        }
        // 调用 Service 层根据 visitorAccountId 查询访客
        List<Visitor> visitors = visitorService.getVisitorsByAccountId(visitorAccountId); // 新增Service方法
        return new ResponseEntity<>(visitors, HttpStatus.OK);
    }

    // --- 公共 API (扫码进出) ---
    // 这些接口通常不直接关联到登录用户，而是通过二维码内容识别访客
    @PostMapping("/api/public/visitors/entry")
    @ResponseBody
    public ResponseEntity<Visitor> recordEntry(@RequestBody Map<String, String> payload) {
        String qrCodeContent = payload.get("qrCodeContent");
        if (qrCodeContent == null || qrCodeContent.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Optional<Visitor> updatedVisitor = visitorService.recordEntry(qrCodeContent);
        if (updatedVisitor.isPresent()) {
            return new ResponseEntity<>(updatedVisitor.get(), HttpStatus.OK);
        } else {
            // 根据recordEntry方法返回null的情况，这里返回400或403
            // 如果是因为ID无效（NumberFormatException），那是BAD_REQUEST
            // 如果是因为状态不允许，那是FORBIDDEN
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/api/public/visitors/exit")
    @ResponseBody
    public ResponseEntity<Visitor> recordExit(@RequestBody Map<String, String> payload) {
        String qrCodeContent = payload.get("qrCodeContent");
        if (qrCodeContent == null || qrCodeContent.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Optional<Visitor> updatedVisitor = visitorService.recordExit(qrCodeContent);
        if (updatedVisitor.isPresent()) {
            return new ResponseEntity<>(updatedVisitor.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST); // 简化处理
        }
    }

    // 获取待审核和已通过的访客列表
    @GetMapping("/api/admin/visitors/pending-approved")
    @ResponseBody
    public ResponseEntity<List<Visitor>> getPendingOrApprovedVisitors() {
        return ResponseEntity.ok(visitorService.getPendingOrApprovedVisitors());
    }

    // 暴露所有访客数据给管理员
    @GetMapping("/admin/visitors/all")
    @ResponseBody
    public ResponseEntity<List<Visitor>> getAllVisitorsForAdmin() {
        return ResponseEntity.ok(visitorService.getAllVisitorsForAdminReview());
    }

    // 生成访客二维码的接口
    @GetMapping("/api/admin/visitors/{visitorId}/qrcode")
    public ResponseEntity<Map<String, String>> generateQrCodeForVisitor(@PathVariable Long visitorId) {
        Optional<Visitor> visitorOptional = visitorService.getVisitorById(visitorId);

        if (visitorOptional.isPresent()) {
            Visitor visitor = visitorOptional.get();
            // 确保只有 APPROVED 状态的访客才能生成二维码
            if (Approval.APPROVED.equals(visitor.getStatus())) {
                String qrCodeBase64 = visitorService.generateVisitorQrCode(visitorId);
                if (qrCodeBase64 != null) {
                    return new ResponseEntity<>(Map.of("qrCodeImage", qrCodeBase64), HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(Map.of("message", "Failed to generate QR code."),
                            HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } else {
                return new ResponseEntity<>(Map.of("message", "Visitor status is not APPROVED."), HttpStatus.FORBIDDEN); // 403
                                                                                                                         // Forbidden
            }
        } else {
            return new ResponseEntity<>(Map.of("message", "Visitor not found."), HttpStatus.NOT_FOUND);
        }
    }
}
