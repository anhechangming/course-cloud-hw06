//
//
//
//package com.zjgsu.cyd.coursecloud.enrollment.service;
//
//import com.zjgsu.cyd.coursecloud.enrollment.client.CatalogClient;
//import com.zjgsu.cyd.coursecloud.enrollment.client.UserClient;
//import com.zjgsu.cyd.coursecloud.enrollment.dto.CourseDto;
//import com.zjgsu.cyd.coursecloud.enrollment.dto.StudentDto;
//import com.zjgsu.cyd.coursecloud.enrollment.exception.ServiceUnavailableException;
//import com.zjgsu.cyd.coursecloud.enrollment.model.EnrollmentRecord;
//import com.zjgsu.cyd.coursecloud.enrollment.repository.EnrollmentRepository;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//import java.util.Map;
//
//@Service
//@Transactional
//public class EnrollmentService {
//
//    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);
//
//    private final UserClient userClient;
//    private final CatalogClient catalogClient;
//    private final EnrollmentRepository repository;
//
//    public EnrollmentService(@Qualifier("userClient") UserClient userClient,
//                             @Qualifier("catalogClient") CatalogClient catalogClient, EnrollmentRepository repository) {
//        this.userClient = userClient;
//        this.catalogClient = catalogClient;
//        this.repository = repository;
//    }
//
//    public EnrollmentRecord enroll(String courseId, String studentId) {
//        log.info("开始选课: studentId={}, courseId={}", studentId, courseId);
//
//        // 1. 检查重复选课
//        if (repository.existsByCourseIdAndStudentId(courseId, studentId)) {
//            log.warn("学生已选该课程: studentId={}, courseId={}", studentId, courseId);
//            throw new IllegalStateException("Student is already enrolled in this course");
//        }
//
//        // 2. Feign调用User Service验证学生
//        log.info("调用 user-service 验证学生: studentId={}", studentId);
//        Map<String, Object> studentResponse = userClient.getStudentByStudentId(studentId);
//
//        String userInstance = String.format("%s:%s",
//                studentResponse.get("hostname"), studentResponse.get("port"));
//        log.info("选课请求 | studentId: {} | 路由到User Service实例: {}", studentId, userInstance);
//
//        // 解析返回结果
//        if ("ERROR".equals(studentResponse.get("status"))) {
//            String message = (String) studentResponse.get("message");
//            log.error("用户服务返回错误: {}", message);
//            throw new IllegalArgumentException("用户服务不可用: " + message);
//        }
//
//        // 提取学生信息（可选）
//        Map<String, Object> studentData = (Map<String, Object>) studentResponse.get("data");
//        log.info("学生验证成功，响应来自端口: {}, 主机名: {}",
//                studentResponse.get("port"), studentResponse.get("hostname"));
//
//        // 3. Feign调用Catalog Service验证课程
//        log.info("调用 catalog-service 验证课程: courseId={}", courseId);
//        Map<String, Object> courseResponse = catalogClient.getCourse(courseId);
//
//        String catalogInstance = String.format("%s:%s",
//                courseResponse.get("hostname"), courseResponse.get("port"));
//        log.info("选课请求 | courseId: {} | 路由到Catalog Service实例: {}", courseId, catalogInstance);
//
//        // 解析返回结果
//        if ("ERROR".equals(courseResponse.get("status"))) {
//            String message = (String) courseResponse.get("message");
//            log.error("课程服务返回错误: {}", message);
//            throw new IllegalArgumentException("课程服务不可用: " + message);
//        }
//
//        // 提取课程容量信息
//        Map<String, Object> courseData = (Map<String, Object>) courseResponse.get("data");
//        Integer capacity = (Integer) courseData.get("capacity");
//        Integer enrolled = (Integer) courseData.get("enrolled");
//        log.info("课程验证成功，响应来自端口: {}, 主机名: {}",
//                courseResponse.get("port"), courseResponse.get("hostname"));
//
//        // 验证课程容量
//        if (enrolled != null && capacity != null && enrolled >= capacity) {
//            log.warn("课程已满: courseId={}, capacity={}, enrolled={}", courseId, capacity, enrolled);
//            throw new IllegalStateException("Course capacity reached");
//        }
//
//        // 4. 创建选课记录
//        EnrollmentRecord record = new EnrollmentRecord(courseId, studentId);
//        EnrollmentRecord saved = repository.save(record);
//
//        log.info("选课成功: studentId={}, courseId={}, enrollmentId={}", studentId, courseId, saved.getId());
//        return saved;
//    }
//
//
//
//
//
//
//    // 以下查询方法无修改，保留原有逻辑
//    @Transactional(readOnly = true)
//    public List<EnrollmentRecord> listByCourse(String courseId) {
//        log.debug("查询课程的选课记录: courseId={}", courseId);
//        return repository.findByCourseId(courseId);
//    }
//
//    @Transactional(readOnly = true)
//    public List<EnrollmentRecord> listByStudent(String studentId) {
//        log.debug("查询学生的选课记录: studentId={}", studentId);
//        return repository.findByStudentId(studentId);
//    }
//
//    @Transactional(readOnly = true)
//    public List<EnrollmentRecord> listAll() {
//        log.debug("查询所有选课记录");
//        return repository.findAll();
//    }
//}
//

//
//package com.zjgsu.cyd.coursecloud.enrollment.service;
//
//import com.zjgsu.cyd.coursecloud.enrollment.client.CatalogClient;
//import com.zjgsu.cyd.coursecloud.enrollment.client.UserClient;
//import com.zjgsu.cyd.coursecloud.enrollment.exception.BusinessException;
//import com.zjgsu.cyd.coursecloud.enrollment.exception.ServiceUnavailableException;
//import com.zjgsu.cyd.coursecloud.enrollment.model.EnrollmentRecord;
//import com.zjgsu.cyd.coursecloud.enrollment.repository.EnrollmentRepository;
//import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Slf4j
//@Service
//@Transactional
//public class EnrollmentService {
//
//
//    private final UserClient userClient;
//    private final CatalogClient catalogClient;
//    private final EnrollmentRepository repository;
//
//    // 修正构造函数（解决字段初始化顺序问题）
//    public EnrollmentService(
//                             UserClient userClient,
//                             CatalogClient catalogClient,
//                             EnrollmentRepository repository) {
//
//        this.userClient = userClient;
//        this.catalogClient = catalogClient;
//        this.repository = repository;
//    }
//
//    public EnrollmentRecord enroll(String courseId, String studentId) {
//        log.info("开始选课: studentId={}, courseId={}", studentId, courseId);
//
//        // 1. 检查重复选课
//        if (repository.existsByCourseIdAndStudentId(courseId, studentId)) {
//            log.warn("学生已选该课程: studentId={}, courseId={}", studentId, courseId);
//            throw new BusinessException("Student is already enrolled in this course", HttpStatus.BAD_REQUEST);
//        }
//
//        // 2. 调用 User Service 并处理降级响应
//        Map<String, Object> studentResponse = verifyStudent(studentId);
//        if ("ERROR".equals(studentResponse.get("status"))) {
//            String errorMsg = (String) studentResponse.get("message");
//            log.error("用户服务降级触发: {}", errorMsg);
//            // 抛出自定义异常，交给全局异常处理器处理
//            throw new ServiceUnavailableException("用户服务暂时不可用: " + errorMsg);
//        }
//
//        String userInstance = String.format("%s:%s",
//                studentResponse.get("hostname"), studentResponse.get("port"));
//        log.info("选课请求 | studentId: {} | 路由到User Service实例: {}", studentId, userInstance);
//
//        // 3. 调用 Catalog Service 并处理降级响应
//        Map<String, Object> courseResponse = verifyCourse(courseId);
//        if ("ERROR".equals(courseResponse.get("status"))) {
//            String errorMsg = (String) courseResponse.get("message");
//            log.error("课程服务降级触发: {}", errorMsg);
//            throw new ServiceUnavailableException("课程服务暂时不可用: " + errorMsg);
//        }
//
//        String catalogInstance = String.format("%s:%s",
//                courseResponse.get("hostname"), courseResponse.get("port"));
//        log.info("选课请求 | courseId: {} | 路由到Catalog Service实例: {}", courseId, catalogInstance);
//
//        // 验证课程容量
//        Map<String, Object> courseData = (Map<String, Object>) courseResponse.get("data");
//        Integer capacity = (Integer) courseData.get("capacity");
//        Integer enrolled = (Integer) courseData.get("enrolled");
//
//        if (enrolled != null && capacity != null && enrolled >= capacity) {
//            log.warn("课程已满: courseId={}, capacity={}, enrolled={}", courseId, capacity, enrolled);
//            throw new BusinessException("Course capacity reached", HttpStatus.BAD_REQUEST);
//        }
//
//        // 4. 创建选课记录
//        EnrollmentRecord record = new EnrollmentRecord(courseId, studentId);
//        EnrollmentRecord saved = repository.save(record);
//
//        log.info("选课成功: studentId={}, courseId={}, enrollmentId={}", studentId, courseId, saved.getId());
//        return saved;
//    }
//
//    // ⭐ 熔断器包装 User Service 调用（移除 RemoteValidationService 依赖，直接调用）
//    @CircuitBreaker(name = "user-service", fallbackMethod = "verifyStudentFallback")
//    public Map<String, Object> verifyStudent(String studentId) {
//        log.info("调用 user-service 验证学生: studentId={}", studentId);
//        return userClient.getStudentByStudentId(studentId);
//    }
//
//    // User Service Fallback 方法
//    public Map<String, Object> verifyStudentFallback(String studentId, Throwable throwable) {
//        log.error("⚠️ User Service 熔断降级触发 | studentId: {} | 原因: {}",
//                studentId, throwable.getMessage(), throwable);
//
//        Map<String, Object> fallback = new HashMap<>();
//        fallback.put("status", "ERROR");
//        fallback.put("fallback", true);
//        fallback.put("message", "用户服务暂时不可用（熔断触发）: " + throwable.getMessage());
//        fallback.put("port", "unknown");
//        fallback.put("hostname", "unknown");
//
//        Map<String, Object> data = new HashMap<>();
//        data.put("studentId", studentId);
//        fallback.put("data", data);
//
//        return fallback;
//    }
//
//    // ⭐ 熔断器包装 Catalog Service 调用
//    @CircuitBreaker(name = "catalog-service", fallbackMethod = "verifyCourseFallback")
//    public Map<String, Object> verifyCourse(String courseId) {
//        log.info("调用 catalog-service 验证课程: courseId={}", courseId);
//        return catalogClient.getCourse(courseId);
//    }
//
//    // Catalog Service Fallback 方法
//    public Map<String, Object> verifyCourseFallback(String courseId, Throwable throwable) {
//        log.error("⚠️ Catalog Service 熔断降级触发 | courseId: {} | 原因: {}",
//                courseId, throwable.getMessage(), throwable);
//
//        Map<String, Object> fallback = new HashMap<>();
//        fallback.put("status", "ERROR");
//        fallback.put("message", "课程服务暂时不可用（熔断触发）: " + throwable.getMessage());
//        fallback.put("port", "unknown");
//        fallback.put("hostname", "unknown");
//
//        Map<String, Object> data = new HashMap<>();
//        data.put("courseId", courseId);
//        fallback.put("data", data);
//
//        return fallback;
//    }
//
//    // 查询方法保持不变
//    @Transactional(readOnly = true)
//    public List<EnrollmentRecord> listByCourse(String courseId) {
//        log.debug("查询课程的选课记录: courseId={}", courseId);
//        return repository.findByCourseId(courseId);
//    }
//
//    @Transactional(readOnly = true)
//    public List<EnrollmentRecord> listByStudent(String studentId) {
//        log.debug("查询学生的选课记录: studentId={}", studentId);
//        return repository.findByStudentId(studentId);
//    }
//
//    @Transactional(readOnly = true)
//    public List<EnrollmentRecord> listAll() {
//        log.debug("查询所有选课记录");
//        return repository.findAll();
//    }
//}
package com.zjgsu.cyd.coursecloud.enrollment.service;

import com.zjgsu.cyd.coursecloud.enrollment.client.CatalogClient;
import com.zjgsu.cyd.coursecloud.enrollment.client.UserClient;
import com.zjgsu.cyd.coursecloud.enrollment.exception.BusinessException;
import com.zjgsu.cyd.coursecloud.enrollment.exception.ServiceUnavailableException;
import com.zjgsu.cyd.coursecloud.enrollment.model.EnrollmentRecord;
import com.zjgsu.cyd.coursecloud.enrollment.repository.EnrollmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
public class EnrollmentService {

    private final UserClient userClient;
    private final CatalogClient catalogClient;
    private final EnrollmentRepository repository;

    public EnrollmentService(
            UserClient userClient,
            CatalogClient catalogClient,
            EnrollmentRepository repository) {
        this.userClient = userClient;
        this.catalogClient = catalogClient;
        this.repository = repository;
    }

    public EnrollmentRecord enroll(String courseId, String studentId) {
        log.info("开始选课: studentId={}, courseId={}", studentId, courseId);

        // 1. 检查重复选课
        if (repository.existsByCourseIdAndStudentId(courseId, studentId)) {
            log.warn("学生已选该课程: studentId={}, courseId={}", studentId, courseId);
            throw new BusinessException("Student is already enrolled in this course", HttpStatus.BAD_REQUEST);
        }

        // 2. 调用 User Service（Feign 会自动处理 Fallback）
        Map<String, Object> studentResponse = userClient.getStudentByStudentId(studentId);

        // ⭐ 检查是否触发了降级
        if ("ERROR".equals(studentResponse.get("status"))) {
            String errorMsg = (String) studentResponse.get("message");
            log.error("✅ 用户服务降级触发: {}", errorMsg);
            throw new ServiceUnavailableException("用户服务暂时不可用: " + errorMsg);
        }

        String userInstance = String.format("%s:%s",
                studentResponse.get("hostname"), studentResponse.get("port"));
        log.info("选课请求 | studentId: {} | 路由到User Service实例: {}", studentId, userInstance);

        // 3. 调用 Catalog Service（Feign 会自动处理 Fallback）
        Map<String, Object> courseResponse = catalogClient.getCourse(courseId);

        // ⭐ 检查是否触发了降级
        if ("ERROR".equals(courseResponse.get("status"))) {
            String errorMsg = (String) courseResponse.get("message");
            log.error("✅ 课程服务降级触发: {}", errorMsg);
            throw new ServiceUnavailableException("课程服务暂时不可用: " + errorMsg);
        }

        String catalogInstance = String.format("%s:%s",
                courseResponse.get("hostname"), courseResponse.get("port"));
        log.info("选课请求 | courseId: {} | 路由到Catalog Service实例: {}", courseId, catalogInstance);

        // 验证课程容量
        Map<String, Object> courseData = (Map<String, Object>) courseResponse.get("data");
        Integer capacity = (Integer) courseData.get("capacity");
        Integer enrolled = (Integer) courseData.get("enrolled");

        if (enrolled != null && capacity != null && enrolled >= capacity) {
            log.warn("课程已满: courseId={}, capacity={}, enrolled={}", courseId, capacity, enrolled);
            throw new BusinessException("Course capacity reached", HttpStatus.BAD_REQUEST);
        }

        // 4. 创建选课记录
        EnrollmentRecord record = new EnrollmentRecord(courseId, studentId);
        EnrollmentRecord saved = repository.save(record);

        log.info("选课成功: studentId={}, courseId={}, enrollmentId={}", studentId, courseId, saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<EnrollmentRecord> listByCourse(String courseId) {
        log.debug("查询课程的选课记录: courseId={}", courseId);
        return repository.findByCourseId(courseId);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentRecord> listByStudent(String studentId) {
        log.debug("查询学生的选课记录: studentId={}", studentId);
        return repository.findByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentRecord> listAll() {
        log.debug("查询所有选课记录");
        return repository.findAll();
    }
}
