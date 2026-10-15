package com.luoyx.lapis.admin.lock;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

/**
 * 基于redisson的分布式锁实现
 *
 * @author Alan.Luo
 */
@Slf4j
public class RedissonLock implements Lock {

    @Resource
    private RedissonClient redissonClient;

    @Override
    public boolean tryLock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        return lock.tryLock();
    }

    @Override
    public boolean tryLock(String lockKey, long waitTime, long leaseTime) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            return lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
        } catch (Exception e) {
            // 2026-10 修正：原先这里只把 e.getMessage() 拼进新异常，丢失了原始堆栈，
            // 排查加锁失败原因时非常困难。现在保留原始异常作为 cause。
            String msg = String.format("尝试加锁出现异常：%s", e.getMessage());
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        // 必须先判断"当前线程是否持有锁"，避免释放了别的线程持有的锁
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    @Override
    public <U> boolean tryLock(String lockKey, long waitTime, long leaseTime, boolean fair, U obj,
                               Consumer<U> action,
                               Consumer<Exception> errorCallBack) {
        RLock lock;
        if (fair) {
            lock = redissonClient.getFairLock(lockKey);
        } else {
            lock = redissonClient.getLock(lockKey);
        }
        try {
            boolean lockResult = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (!lockResult) {
                return false;
            }
            action.accept(obj);
        } catch (Exception exception) {
            errorCallBack.accept(exception);
        } finally {
            // 2026-10 修正：原判断为 lock.isLocked()，在"租约到期、锁已被其他线程持有"的场景下
            // 会误释放别人的锁并抛出 IllegalMonitorStateException。
            // 正确做法是只释放"当前线程仍持有"的锁。
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return true;
    }
}
