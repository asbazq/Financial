package com.example.Financial.repository;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.example.Financial.model.Account;

@Repository
public class BalanceRepository {
    // 간단한 데이터베이스의 역할을 수행하기 위해 HashMap 사용
    private final ConcurrentHashMap<Long, Account> db = new ConcurrentHashMap<>();

    public Account findById(Long id) {
        return db.computeIfAbsent(id, k -> new Account(k, 0L, System.currentTimeMillis(), System.nanoTime()));
    }

    public void save (Long id,Account account) {
        db.put(id, account);
    }
}
