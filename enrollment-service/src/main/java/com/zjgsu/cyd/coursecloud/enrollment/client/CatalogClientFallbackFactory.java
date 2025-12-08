package com.zjgsu.cyd.coursecloud.enrollment.client;


import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;


//
//
//@Slf4j
//@Component
//public class CatalogClientFallbackFactory implements FallbackFactory<CatalogClient> {
//
//
//    @Override
//    public CatalogClient create(Throwable cause) {
//        return new CatalogClient() {
//            @Override
//            public Map<String, Object> getCourse(String id) {
//                log.error("⚠️ CatalogClient fallback triggered for courseId: {}, 原因: {}",
//                        id, cause.getMessage(), cause);
//
//                Map<String, Object> fallback = new HashMap<>();
//                fallback.put("status", "ERROR");
//                fallback.put("message", "课程服务暂时不可用: " + cause.getMessage());
//                fallback.put("port", "unknown");
//                fallback.put("hostname", "unknown");
//
//                return fallback;
//            }
//        };
//    }
//
//}



@Slf4j
@Component
public class CatalogClientFallbackFactory implements FallbackFactory<CatalogClient> {

    @Override
    public CatalogClient create(Throwable cause) {
        // ⭐ 在创建 Fallback 时就记录日志
        log.error("=================================================");
        log.error("🔥 CatalogClientFallbackFactory 被调用！");
        log.error("🔥 异常类型: {}", cause.getClass().getName());
        log.error("🔥 异常信息: {}", cause.getMessage());
        log.error("=================================================", cause);

        return new CatalogClient() {
            @Override
            public Map<String, Object> getCourse(String id) {
                // ⭐ 每次调用 Fallback 方法也记录日志
                log.error("✅✅✅ CatalogClient Fallback 触发！courseId: {}", id);
                log.error("✅ 原因: {}", cause.getMessage());

                Map<String, Object> fallback = new HashMap<>();
                fallback.put("status", "ERROR");
                fallback.put("fallback", true);
                fallback.put("message", "课程服务暂时不可用: " + cause.getMessage());
                fallback.put("port", "unknown");
                fallback.put("hostname", "fallback-instance");

                // ⭐ 关键：必须包含 data 字段，否则业务代码会 NPE
                Map<String, Object> data = new HashMap<>();
                data.put("courseId", id);
                data.put("capacity", 100);  // 默认容量
                data.put("enrolled", 0);    // 默认已选人数
                fallback.put("data", data);

                log.error("✅ 返回降级响应: {}", fallback);
                return fallback;
            }
        };
    }
}