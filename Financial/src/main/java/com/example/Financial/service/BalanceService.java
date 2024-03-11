package com.example.Financial.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Financial.model.Account;
import com.example.Financial.repository.BalanceRepository;


@Service
public class BalanceService {
    // 간단한 데이터베이스의 역할을 수행하기 위해 HashMap 사용
    // private ConcurrentHashMap<Long, Account> db = new ConcurrentHashMap<>();
    private final BalanceRepository balanceRepository;

    public BalanceService(BalanceRepository balanceRepository) {
        this.balanceRepository = balanceRepository;
    }

    @Transactional
    // 클래스 레벨에서 동기화 -> 성능 저하, @Transactiona과 함께 사용 시 간섭 발생 위험
    // public synchronized Account deposit(Long id, Long amount) {
        public Account deposit(Long id, Long amount) {
        Account account = getBalance(id);
        // 각 계정 인스턴스에 대해 동기화 블록을 사용 -> 더 세밀하게 동기화할 수 있으며, 성능 저하를 최소화
        synchronized (account) {
            account.setBalance(account.getBalance() + amount);
            balanceRepository.save(id, account);
        }
        return account;
    }

    @Transactional
    public Account withdraw(Long id, Long amount) {
        Account account = getBalance(id);
        synchronized (account) {
            long newBalance = account.getBalance() - amount;
            if (newBalance < 0) {
                throw new IllegalArgumentException("Insufficient balance");
            }
            account.setBalance(newBalance);
            balanceRepository.save(id, account);
        }
        return account;
    }

    @Transactional
    public Account getBalance(Long id) {
        // return db.getOrDefault(id, new Account(id , 0L, System.currentTimeMillis(), System.nanoTime()));
        return balanceRepository.findById(id);
    }
}
