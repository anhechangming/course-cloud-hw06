////package com.zjgsu.cyd.coursecloud.enrollment.service;
////
////import com.zjgsu.cyd.coursecloud.enrollment.model.EnrollmentRecord;
////import com.zjgsu.cyd.coursecloud.enrollment.repository.EnrollmentRepository;
////import org.slf4j.Logger;
////import org.slf4j.LoggerFactory;
////import org.springframework.stereotype.Service;
////import org.springframework.transaction.annotation.Transactional;
////import org.springframework.web.client.HttpClientErrorException;
////import org.springframework.web.client.RestTemplate;
////
////import java.util.List;
////import java.util.Map;
////
////@Service
////@Transactional
////public class EnrollmentService {
////
////    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);
////
////    private final RestTemplate restTemplate;
////    private final EnrollmentRepository repository;
////
////    // ⭐⭐⭐ 关键修改：不再使用 @Value 读取 URL，直接使用服务名
////    private static final String USER_SERVICE_URL = "http://user-service";  // Nacos 服务名
////    private static final String CATALOG_SERVICE_URL = "http://catalog-service";  // Nacos 服务名
////
////    public EnrollmentService(RestTemplate restTemplate, EnrollmentRepository repository) {
////        this.restTemplate = restTemplate;
////        this.repository = repository;
////    }
////
////    public EnrollmentRecord enroll(String courseId, String studentId) {
////        log.info("开始选课: studentId={}, courseId={}", studentId, courseId);
////
////        // Check if already enrolled
////        if (repository.existsByCourseIdAndStudentId(courseId, studentId)) {
////            log.warn("学生已选该课程: studentId={}, courseId={}", studentId, courseId);
////            throw new IllegalStateException("Student is already enrolled in this course");
////        }
////
////        // 1. ⭐ 通过服务名调用 user-service
////        try {
////            String userUrl = USER_SERVICE_URL + "/api/students/studentId/" + studentId;
////            log.info("调用 user-service 验证学生: {}", userUrl);
////
////            Map<String, Object> studentResponse = restTemplate.getForObject(userUrl, Map.class);
////            log.info("学生验证成功，响应来自端口: {}", studentResponse.get("port"));  // ✅ 日志显示负载均衡
////
////        } catch (HttpClientErrorException.NotFound e) {
////            log.error("学生不存在: {}", studentId);
////            throw new IllegalArgumentException("Student not found: " + studentId);
////        } catch (Exception e) {
////            log.error("验证学生时出错: {}", e.getMessage(), e);
////            throw new RuntimeException("Error verifying student with user-service: " + e.getMessage());
////        }
////
////        // 2. ⭐ 通过服务名调用 catalog-service
////        try {
////            String courseUrl = CATALOG_SERVICE_URL + "/api/courses/" + courseId;
////            log.info("调用 catalog-service 验证课程: {}", courseUrl);
////
////            Map<String, Object> courseResponse = restTemplate.getForObject(courseUrl, Map.class);
////
////            if (courseResponse == null) {
////                log.error("课程不存在: {}", courseId);
////                throw new IllegalArgumentException("Course not found: " + courseId);
////            }
////
////            log.info("课程验证成功，响应来自端口: {}", courseResponse.get("port"));  // ✅ 日志显示负载均衡
////
////            // 处理嵌套的 data 结构
////            Object dataObj = courseResponse.get("data");
////            Map<String, Object> courseData = dataObj instanceof Map ? (Map<String, Object>) dataObj : courseResponse;
////
////            Integer capacity = (Integer) courseData.get("capacity");
////            Integer enrolled = (Integer) courseData.get("enrolled");
////
////            log.debug("课程容量检查: capacity={}, enrolled={}", capacity, enrolled);
////
////            if (enrolled != null && capacity != null && enrolled >= capacity) {
////                log.warn("课程已满: courseId={}, capacity={}, enrolled={}", courseId, capacity, enrolled);
////                throw new IllegalStateException("Course capacity reached");
////            }
////
////        } catch (HttpClientErrorException.NotFound e) {
////            log.error("课程不存在: {}", courseId);
////            throw new IllegalArgumentException("Course not found: " + courseId);
////        } catch (IllegalStateException | IllegalArgumentException e) {
////            throw e;
////        } catch (Exception e) {
////            log.error("验证课程时出错: {}", e.getMessage(), e);
////            throw new RuntimeException("Error verifying course with catalog-service: " + e.getMessage());
////        }
////
////        // 3. Create enrollment record
////        EnrollmentRecord record = new EnrollmentRecord(courseId, studentId);
////        EnrollmentRecord saved = repository.save(record);
////
////        log.info("选课成功: studentId={}, courseId={}, enrollmentId={}", studentId, courseId, saved.getId());
////
////        return saved;
////    }
////
////    @Transactional(readOnly = true)
////    public List<EnrollmentRecord> listByCourse(String courseId) {
////        log.debug("查询课程的选课记录: courseId={}", courseId);
////        return repository.findByCourseId(courseId);
////    }
////
////    @Transactional(readOnly = true)
////    public List<EnrollmentRecord> listByStudent(String studentId) {
////        log.debug("查询学生的选课记录: studentId={}", studentId);
////        return repository.findByStudentId(studentId);
////    }
////
////    @Transactional(readOnly = true)
////    public List<EnrollmentRecord> listAll() {
////        log.debug("查询所有选课记录");
////        return repository.findAll();
////    }
////}
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
//
//@Service
//@Transactional
//public class EnrollmentService {
//
//    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);
//
//    // ⭐ 关键修改1：移除 RestTemplate，注入 Feign Client
//    private final UserClient userClient;
//    private final CatalogClient catalogClient;
//    private final EnrollmentRepository repository;
//
//    // ⭐ 关键修改2：构造函数注入 Feign Client（无需 URL 常量，直接用服务名调用）
//    public EnrollmentService(UserClient userClient,CatalogClient catalogClient, EnrollmentRepository repository) {
//        this.userClient = userClient;
//        this.catalogClient = catalogClient;
//        this.repository = repository;
//    }
//
//    public EnrollmentRecord enroll(String courseId, String studentId) {
//        log.info("开始选课: studentId={}, courseId={}", studentId, courseId);
//
//        // 1. 检查是否已选课（原有逻辑保留）
//        if (repository.existsByCourseIdAndStudentId(courseId, studentId)) {
//            log.warn("学生已选该课程: studentId={}, courseId={}", studentId, courseId);
//            throw new IllegalStateException("Student is already enrolled in this course");
//        }
//
//        // 2. ⭐ Feign 调用 User Service 验证学生（替代 RestTemplate）
//        try {
//            log.info("调用 user-service 验证学生: studentId={}", studentId);
//            // 注意：若 UserClient 的 getStudent 入参为 Long，需将 String 转 Long（根据你的 UserClient 定义调整）
//            StudentDto studentDto = userClient.getStudent(Long.valueOf(studentId));
//
//            // 验证返回结果（DTO 已承载 port/hostname，用于负载均衡日志）
//            if (studentDto == null) {
//                log.error("学生不存在: {}", studentId);
//                throw new IllegalArgumentException("Student not found: " + studentId);
//            }
//            log.info("学生验证成功，响应来自端口: {}, 主机名: {}", studentDto.getPort(), studentDto.getHostname());
//
//        } catch (IllegalArgumentException e) {
//            // 学生不存在异常直接抛出
//            throw e;
//        } catch (ServiceUnavailableException e) {
//            // ⭐ 捕获 Feign Fallback 抛出的服务不可用异常
//            log.error("用户服务不可用: {}", e.getMessage());
//            throw new RuntimeException("选课失败：" + e.getMessage());
//        } catch (Exception e) {
//            log.error("验证学生时出错: {}", e.getMessage(), e);
//            throw new RuntimeException("Error verifying student with user-service: " + e.getMessage());
//        }
//
//        // 3. ⭐ Feign 调用 Catalog Service 验证课程（替代 RestTemplate）
//        try {
//            log.info("调用 catalog-service 验证课程: courseId={}", courseId);
//            // 注意：若 CatalogClient 的 getCourse 入参为 Long，需将 String 转 Long（根据你的 CatalogClient 定义调整）
//            CourseDto courseDto = catalogClient.getCourse(Long.valueOf(courseId));
//
//            // 验证课程是否存在（DTO 直接承载数据，无需解析 Map）
//            if (courseDto == null) {
//                log.error("课程不存在: {}", courseId);
//                throw new IllegalArgumentException("Course not found: " + courseId);
//            }
//            log.info("课程验证成功，响应来自端口: {}, 主机名: {}", courseDto.getPort(), courseDto.getHostname());
//
//            // 验证课程容量（DTO 直接获取字段，无需解析嵌套 Map）
//            Integer capacity = courseDto.getCapacity();
//            Integer enrolled = courseDto.getEnrolled();
//            log.debug("课程容量检查: capacity={}, enrolled={}", capacity, enrolled);
//
//            if (enrolled != null && capacity != null && enrolled >= capacity) {
//                log.warn("课程已满: courseId={}, capacity={}, enrolled={}", courseId, capacity, enrolled);
//                throw new IllegalStateException("Course capacity reached");
//            }
//
//        } catch (IllegalArgumentException | IllegalStateException e) {
//            // 课程相关异常直接抛出
//            throw e;
//        } catch (ServiceUnavailableException e) {
//            // ⭐ 捕获 Feign Fallback 抛出的服务不可用异常
//            log.error("课程服务不可用: {}", e.getMessage());
//            throw new RuntimeException("选课失败：" + e.getMessage());
//        } catch (Exception e) {
//            log.error("验证课程时出错: {}", e.getMessage(), e);
//            throw new RuntimeException("Error verifying course with catalog-service: " + e.getMessage());
//        }
//
//        // 4. 创建选课记录（原有逻辑保留）
//        EnrollmentRecord record = new EnrollmentRecord(courseId, studentId);
//        EnrollmentRecord saved = repository.save(record);
//
//        log.info("选课成功: studentId={}, courseId={}, enrollmentId={}", studentId, courseId, saved.getId());
//
//        return saved;
//    }
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



package com.zjgsu.cyd.coursecloud.enrollment.service;

import com.zjgsu.cyd.coursecloud.enrollment.model.EnrollmentRecord;
import com.zjgsu.cyd.coursecloud.enrollment.repository.EnrollmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);

    // 本地测试：通过配置注入user/catalog服务的本地地址（替代Nacos服务名）
    @Value("${user.service.url:http://localhost:8081}")
    private String userServiceUrl;
    @Value("${catalog.service.url:http://localhost:8082}")
    private String catalogServiceUrl;

    private final RestTemplate restTemplate;
    private final EnrollmentRepository repository;

    // 构造函数：注入RestTemplate（移除Feign Client）
    public EnrollmentService(RestTemplate restTemplate, EnrollmentRepository repository) {
        this.restTemplate = restTemplate;
        this.repository = repository;
    }

    public EnrollmentRecord enroll(String courseId, String studentId) {
        log.info("开始选课: studentId={}, courseId={}", studentId, courseId);

        // 1. 检查是否已选课（原有逻辑保留）
        if (repository.existsByCourseIdAndStudentId(courseId, studentId)) {
            log.warn("学生已选该课程: studentId={}, courseId={}", studentId, courseId);
            throw new IllegalStateException("Student is already enrolled in this course");
        }

        // 2. 调用 User Service 验证学生（RestTemplate + 本地IP+端口）
        try {
            String userApiUrl = userServiceUrl + "/api/users/students/studentId/" + studentId;
            log.info("调用 user-service 验证学生: {}", userApiUrl);

            // 调用本地user-service接口（返回Map，兼容原有逻辑）
            Map<String, Object> studentResponse = restTemplate.getForObject(userApiUrl, Map.class);

            if (studentResponse == null || studentResponse.get("status").equals("ERROR")) {
                log.error("学生不存在: {}", studentId);
                throw new IllegalArgumentException("Student not found: " + studentId);
            }
            log.info("学生验证成功，响应来自端口: {}", studentResponse.get("port"));

        } catch (HttpClientErrorException.NotFound e) {
            log.error("学生不存在: {}", studentId);
            throw new IllegalArgumentException("Student not found: " + studentId);
        } catch (Exception e) {
            log.error("验证学生时出错: {}", e.getMessage(), e);
            throw new RuntimeException("Error verifying student with user-service: " + e.getMessage());
        }

        // 3. 调用 Catalog Service 验证课程（RestTemplate + 本地IP+端口）
        try {
            String courseApiUrl = catalogServiceUrl + "/api/courses/" + courseId;
            log.info("调用 catalog-service 验证课程: {}", courseApiUrl);

            // 调用本地catalog-service接口（返回Map，兼容原有逻辑）
            Map<String, Object> courseResponse = restTemplate.getForObject(courseApiUrl, Map.class);

            if (courseResponse == null || courseResponse.get("status").equals("ERROR")) {
                log.error("课程不存在: {}", courseId);
                throw new IllegalArgumentException("Course not found: " + courseId);
            }
            log.info("课程验证成功，响应来自端口: {}", courseResponse.get("port"));

            // 解析课程容量（兼容catalog-service的返回结构）
            Map<String, Object> courseData = (Map<String, Object>) courseResponse.get("data");
            Integer capacity = (Integer) courseData.get("capacity");
            Integer enrolled = (Integer) courseData.get("enrolled");

            log.debug("课程容量检查: capacity={}, enrolled={}", capacity, enrolled);
            if (enrolled != null && capacity != null && enrolled >= capacity) {
                log.warn("课程已满: courseId={}, capacity={}, enrolled={}", courseId, capacity, enrolled);
                throw new IllegalStateException("Course capacity reached");
            }

        } catch (HttpClientErrorException.NotFound e) {
            log.error("课程不存在: {}", courseId);
            throw new IllegalArgumentException("Course not found: " + courseId);
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("验证课程时出错: {}", e.getMessage(), e);
            throw new RuntimeException("Error verifying course with catalog-service: " + e.getMessage());
        }

        // 4. 创建选课记录（原有逻辑保留）
        EnrollmentRecord record = new EnrollmentRecord(courseId, studentId);
        EnrollmentRecord saved = repository.save(record);

        log.info("选课成功: studentId={}, courseId={}, enrollmentId={}", studentId, courseId, saved.getId());

        return saved;
    }

    // 以下查询方法无修改，保留原有逻辑
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