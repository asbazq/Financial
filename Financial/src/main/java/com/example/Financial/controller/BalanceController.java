package com.example.Financial.controller;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.Financial.model.Account;
import com.example.Financial.service.BalanceService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@RestController
@RequestMapping("/account")
// JpaRepository를 사용한다면 데이터베이스 레벨에서 적절한 락을 사용하여 동시성을 관리, thread-safe를 위해 ConcurrentHashMap 을 사용할 필요X
public class BalanceController {

    private final BalanceService balanceService;
    // 각 키에 대해 개별적인 동기화를 수행하여, 입금과 출금 요청은 동시에 올 수 있고, 요청온 차례대로 실행
    private final ConcurrentHashMap<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    // 생성자 주입
    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping("{id}/balance")
    public ResponseEntity<Account> getBalance(@PathVariable Long id) {
        return ResponseEntity.ok(balanceService.getBalance(id));
    }

    @PostMapping("{id}/deposit")
    public ResponseEntity<Account> deposit(@PathVariable Long id, @RequestBody Long amount) {
        // 계정 ID를 키로 하여 해당 ID의 ReentrantLock을 값으로 저장
        ReentrantLock lock = locks.computeIfAbsent(id, k -> new ReentrantLock());
        // lock.tryLock()은 즉시 잠금가능 시 true를 반환하고, 그렇지 않으면 false를 반환
        if (lock.tryLock()) {
            try {
                Account account = balanceService.deposit(id, amount);
                return ResponseEntity.ok(account);
            } finally {
                // finally 블록을 사용하여, 작업이 성공하든 예외가 발생하든 간에 잠금을 해제
                lock.unlock();
            }
        } else {
            // 잠금을 못했을 경우, 즉 동일한 계정에 대한 입금 요청이 동시에 처리X
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
    }

    @PostMapping("{id}/withdraw")
    public ResponseEntity<Account> withdraw(@PathVariable Long id, @RequestBody Long amount) {
        ReentrantLock lock = locks.computeIfAbsent(id, k -> new ReentrantLock());
        // 동기화 기법으로 스레드는 잠금을 획득할 때까지 대기하며, 잠금을 보유한 스레드만 그 이후의 코드를 실행
        // 잔고 출금 요청이 동시에 2개 이상 올 경우 차례대로 실행
        lock.lock();
        try {
            Account account = balanceService.withdraw(id, amount);
            return ResponseEntity.ok(account);
        } finally {
            lock.unlock();
        }
    }
}