package org.example.dln.service;

import org.example.dln.config.CacheNames;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 包名：org.example.dln.service
 * 类名：CacheInvalidationService
 * 类描述：统一处理业务数据变更后的缓存清理。
 * 创建人：@author Rain_润
 */
@Service
public class CacheInvalidationService {
    @Autowired
    private CacheManager cacheManager;

    /**
    * 清理知识库相关展示缓存。
     * @param userId 用户ID
     * @param knowledgeBaseId 知识库ID
    */
    public void evictKnowledgeBaseCaches(Long userId, Long knowledgeBaseId) {
        if (userId == null || knowledgeBaseId == null) {
            return;
        }

        Runnable evictAction = () -> {
            String cacheKey = userId + ":" + knowledgeBaseId;
            evict(CacheNames.KNOWLEDGE_BASE_TREE, cacheKey);
            evict(CacheNames.KNOWLEDGE_GRAPH, cacheKey);
            evict(CacheNames.NOTE_LINK_CANDIDATES, cacheKey);
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evictAction.run();
                }
            });
            return;
        }

        evictAction.run();
    }

    /**
    * 清理指定缓存项。
     * @param cacheName 缓存名称
     * @param key 缓存键
    */
    private void evict(String cacheName, String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }
}
