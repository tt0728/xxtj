package com.example.demo.service;

import com.example.demo.module.Approval;
import com.example.demo.module.Companion;
import com.example.demo.module.Visitor;
import com.example.demo.repository.VisitorRepository;
import com.example.demo.dto.VisitorRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.time.LocalDateTime;
//import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class VisitorService {

    private final VisitorRepository visitorRepository;

    @Autowired
    public VisitorService(VisitorRepository visitorRepository) {
        this.visitorRepository = visitorRepository;
    }

    // 访客预约提交，现在接受 visitorAccountId
    //@Transactional
    public Visitor submitAppointment(VisitorRequest request, Long visitorAccountId) {
        Visitor visitor = new Visitor();
        visitor.setVisitorAccountId(visitorAccountId); // 设置关联的访客账号ID
        visitor.setName(request.getName());
        visitor.setPhone(request.getPhone());
        visitor.setIdCard(request.getIdCard());
        visitor.setVisitedPerson(request.getVisitedPerson());
        visitor.setVisitedPersonDepartment(request.getVisitedPersonDepartment());
        visitor.setVisitDate((request.getVisitDate()));
        visitor.setVisitTimeStart((request.getVisitTimeStart()));
        visitor.setVisitTimeEnd((request.getVisitTimeEnd()));
        visitor.setPurpose(request.getPurpose());
        visitor.setStatus(Approval.PENDING); // 默认待审核
        visitor.setStatus(Approval.UNUSED);
        // 处理同行人
        if (request.getCompanions() != null && !request.getCompanions().isEmpty()) {
            List<Companion> companions = request.getCompanions().stream()
                    .map(dto -> {
                        Companion companion = new Companion();
                        companion.setName(dto.getName());
                        companion.setPhone(dto.getPhone());
                        companion.setIdCard(dto.getIdCard());
                        companion.setVisitor(visitor); // 设置关联的访客
                        return companion;
                    })
                    .collect(Collectors.toList());
            visitor.setCompanions(companions);
        }

        return visitorRepository.save(visitor);
    }

    // 获取所有待审核或已审核的访客列表 (管理员使用)
    public List<Visitor> getPendingOrApprovedVisitors() {
        return visitorRepository.findByStatusIn(List.of(Approval.PENDING, Approval.APPROVED));
    }

    // 根据访客账号ID获取所有预约
    public List<Visitor> getVisitorsByAccountId(Long visitorAccountId) {
        return visitorRepository.findByVisitorAccountId(visitorAccountId);
    }

    // 获取访客详情
    public Optional<Visitor> getVisitorDetails(Long id) {
        return visitorRepository.findById(id);
    }

    //@Transactional
    public Optional<Visitor> approveOrRejectVisitor(Long id, String newStatus, String comments) {
        return visitorRepository.findById(id)
                .map(visitor -> {
                    if ("APPROVED".equalsIgnoreCase(newStatus)) {
                        visitor.setStatus(Approval.APPROVED);
                    } else if ("REJECTED".equalsIgnoreCase(newStatus)) {
                        visitor.setStatus(Approval.REJECTED);
                    }
                    visitor.setComments(comments);
                    return visitorRepository.save(visitor);
                });
    }

    //@Transactional
    public Optional<Visitor> recordEntry(String qrCodeContent) {
        // 假设qrCodeContent是Visitor的ID
        try {
            Long visitorId = Long.parseLong(qrCodeContent);
            return visitorRepository.findById(visitorId)
                    .map(visitor -> {
                        if (Approval.APPROVED.equals(visitor.getStatus())
                                || Approval.ENTERED.equals(visitor.getStatus())) {
                            visitor.setEntryTime(LocalDateTime.now());
                            visitor.setStatus(Approval.ENTERED); // 更新状态为已进入
                            visitorRepository.save(visitor);
                            System.out.println("访客 " + visitor.getName() + " (ID: " + visitorId + ") 成功进入。");
                            return visitor;
                        } else if (Approval.APPROVED.equals(visitor.getStatus())
                                && Approval.ENTERED.equals(visitor.getStatus())) {
                            // 已经进入过，可以记录再次进入或者提示已进入
                            System.out.println("访客 " + visitor.getName() + " (ID: " + visitorId + ") 已经进入。");
                            return visitor; // 返回当前访客，表示操作成功
                        } else {
                            System.out.println("访客 " + visitor.getName() + " (ID: " + visitorId + ") 状态不允许进入: "
                                    + visitor.getStatus());
                            return null; // 返回null表示不允许进入
                        }
                    });
        } catch (NumberFormatException e) {
            System.err.println("QR Code 内容不是有效的访客ID: " + qrCodeContent);
            return Optional.empty();
        }
    }

    //@Transactional
    public Optional<Visitor> recordExit(String qrCodeContent) {
        // 假设qrCodeContent是Visitor的ID
        try {
            Long visitorId = Long.parseLong(qrCodeContent);
            return visitorRepository.findById(visitorId)
                    .map(visitor -> {
                        if (Approval.ENTERED.equals(visitor.getStatus())) {
                            visitor.setExitTime(LocalDateTime.now());
                            visitor.setStatus(Approval.COMPLETED); // 更新状态为已完成访问
                            visitorRepository.save(visitor);
                            System.out.println("访客 " + visitor.getName() + " (ID: " + visitorId + ") 成功离开。");
                            return visitor;
                        } else {
                            System.out.println("访客 " + visitor.getName() + " (ID: " + visitorId + ") 状态不允许离开: "
                                    + visitor.getStatus());
                            return null; // 返回null表示不允许离开
                        }
                    });
        } catch (NumberFormatException e) {
            System.err.println("QR Code 内容不是有效的访客ID: " + qrCodeContent);
            return Optional.empty();
        }
    }

    // 获取所有状态的访客用于管理员审核
    public List<Visitor> getAllVisitorsForAdminReview() {
        return visitorRepository.findAll();
    }

    // 根据访客ID生成二维码图片（Base64编码字符串）
    public String generateVisitorQrCode(Long visitorId) {
        // 二维码中存储的内容，通常是访客的唯一标识
        String qrCodeContent = String.valueOf(visitorId); // 简单起见，直接用ID

        int width = 200; // 二维码图片宽度
        int height = 200; // 二维码图片高度
        String format = "png"; // 图片格式

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8"); // 字符编码
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // 容错级别
        hints.put(EncodeHintType.MARGIN, 2); // 边距

        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    qrCodeContent,
                    BarcodeFormat.QR_CODE,
                    width,
                    height,
                    hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, format, outputStream);
            byte[] qrCodeBytes = outputStream.toByteArray();

            // 将字节数组转换为Base64编码字符串
            return java.util.Base64.getEncoder().encodeToString(qrCodeBytes);

        } catch (WriterException | IOException e) {
            System.err.println("生成二维码失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // 获取单个访客的信息，以便生成二维码时能查到该访客的状态
    public Optional<Visitor> getVisitorById(Long id) {
        return visitorRepository.findById(id);
    }
}
