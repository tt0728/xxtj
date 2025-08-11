package com.example.demo.module;

public enum Approval {
    PENDING, // 待审核
    APPROVED, // 已通过
    REJECTED, // 已拒绝
    UNUSED, // 初始未使用的状态
    ENTERED, // 已进入
    COMPLETED, // 已完成访问
    EXPIRED // 预约已过期
}