package com.example.demo.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MyService {

    @Async("taskExecutor")
    public void doSomethingAsync() {
        System.out.println("当前线程 (异步方法内)：" + Thread.currentThread().getName());
        // 模拟一个耗时操作
        try {
            Thread.sleep(3000); // 暂停 3 秒
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("异步任务被中断");
        }
        System.out.println("异步方法执行完毕。");
    }

    public void doSomethingSync() {
        System.out.println("当前线程 (同步方法内)：" + Thread.currentThread().getName());
        System.out.println("同步方法执行完毕。");
    }
}