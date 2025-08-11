package com.example.demo.service;

import com.example.demo.module.VisitorAccount;
import com.example.demo.repository.VisitorAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
//import org.springframework.util.DigestUtils;
import java.io.UnsupportedEncodingException;
import jakarta.servlet.http.HttpSession; // 导入 HttpSession
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class VisitorAccountService {

    private final VisitorAccountRepository visitorAccountRepository;

    @Autowired
    public VisitorAccountService(VisitorAccountRepository visitorAccountRepository) {
        this.visitorAccountRepository = visitorAccountRepository;
    }

    private String hashPasswordSHA256(String password) { // 这个方法必须在类内部
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes("UTF-8")); // 指定UTF-8编码

            // 将字节数组转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // 如果系统不支持 SHA-256 算法，这几乎不可能发生
            throw new RuntimeException("SHA-256 algorithm not found", e);
        } catch (UnsupportedEncodingException e) {
            // 如果 UTF-8 编码不支持，这几乎不可能发生
            throw new RuntimeException("UTF-8 encoding not supported", e);
        }
    }

    // 注册新的访客账号
    public VisitorAccount register(String username, String password) {
        if (visitorAccountRepository.findByUsername(username).isPresent()) {
            return null; // 用户名已存在
        }
        VisitorAccount newAccount = new VisitorAccount();
        newAccount.setUsername(username);
        String hashedForRegister = hashPasswordSHA256(password); // 调用哈希方法
        newAccount.setPassword(hashedForRegister);
        System.out.println("注册用户名: " + username + ", 注册密码哈希: " + hashedForRegister); // 打印注册时的哈希
        // newAccount.setPassword(hashPasswordSHA256(password));
        return visitorAccountRepository.save(newAccount);
    }

    // 认证访客登录
    public VisitorAccount authenticate(String username, String password, HttpSession session) {
        String hashedPasswordFromFrontend = password;
        System.out.println("后端接收到的用户名: " + username);
        System.out.println("后端计算的输入密码哈希: " + hashedPasswordFromFrontend); // 打印后端计算的哈希
        return visitorAccountRepository.findByUsername(username)
                .filter(account -> {
                    if (account == null) {
                        System.out.println("用户不存在: " + username);
                        return false;
                    }
                    System.out.println("数据库中存储的密码哈希: " + account.getPassword()); // 打印数据库中的哈希
                    boolean matches = account.getPassword().equals(hashedPasswordFromFrontend);
                    System.out.println("密码匹配结果: " + matches);
                    return matches;
                })
                .map(account -> {
                    // 登录成功，将用户ID存储到Session中
                    session.setAttribute("visitorAccountId", account.getId());
                    session.setAttribute("visitorUsername", account.getUsername());
                    System.out.println(
                            "Visitor logged in: " + account.getUsername() + ", Session ID: " + session.getId());
                    return account;
                })
                .orElse(null);
    }

    // 从 Session 获取当前登录的访客账号ID
    public Long getCurrentVisitorAccountId(HttpSession session) {
        return (Long) session.getAttribute("visitorAccountId");
    }

    // 登出访客账号
    public void logout(HttpSession session) {
        session.invalidate(); // 使当前会话失效
    }
}