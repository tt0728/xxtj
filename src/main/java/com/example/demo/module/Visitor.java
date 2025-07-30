package com.example.demo.module;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Visitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long visitorAccountId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false, unique = true) // 身份证号通常是唯一的
    private String idCard;
    @Column(nullable = false) // 如果数据库要求非空，这里也设置非空
    private String visitedPerson;
    @Column(nullable = false) // 如果数据库要求非空，这里也设置非空
    private String visitedPersonDepartment;
    private LocalDate visitDate;
    private LocalTime visitTimeStart;
    private LocalTime visitTimeEnd;
    private String purpose;

    @Enumerated(EnumType.STRING)
    private Approval status = Approval.PENDING; // 默认待审核

    private String comments; // 审批意见

    // 进出记录
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;

    @OneToMany(mappedBy = "visitor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Companion> companions;

    public Approval getStatus() {
        return status;
    }

    public void setStatus(Approval status) {
        this.status = status;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(LocalDateTime entryTime) {
        this.entryTime = entryTime;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }

    public void setExitTime(LocalDateTime exitTime) {
        this.exitTime = exitTime;
    }

    // public UsageStatus getUsageStatus() { return usageStatus; }
    // public void setUsageStatus(UsageStatus usageStatus) { this.usageStatus =
    // usageStatus; }
}