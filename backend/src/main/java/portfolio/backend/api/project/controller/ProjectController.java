package portfolio.backend.api.project.controller;

import com.amazonaws.services.s3.AmazonS3;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import portfolio.backend.api.artist.entity.Artist;
import portfolio.backend.api.imageupload.service.ProjectS3Service;
import portfolio.backend.api.project.entity.Project;
import portfolio.backend.api.project.entity.ProjectImages;
import portfolio.backend.api.project.repository.ProjectRepository;

import portfolio.backend.api.project.exception.ResourceNotFoundException; // Import statement for ResourceNotFoundException
import portfolio.backend.authentication.api.repository.user.UserRepository;
import portfolio.backend.authentication.api.service.UserService;
import springfox.documentation.annotations.ApiIgnore;


import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;


@Api(tags = {"Project"})
@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectRepository projectRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final AmazonS3 s3Client;
    private final ProjectS3Service projectS3Service;

    @Autowired
    public ProjectController(ProjectRepository projectRepository, UserRepository userRepository, UserService userService, AmazonS3 s3Client, ProjectS3Service projectS3Service) {
        this.projectRepository = projectRepository;
        this.userService = userService;
        this.userRepository = userRepository;
        this.s3Client = s3Client;
        this.projectS3Service = projectS3Service;
    }

    // 특정 프로젝트 PK Param GET
    @GetMapping("/{id}")
    public Project getProjectById(@PathVariable Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트 ID 찾을 수 없음: " + id));
        return project;
    }

    // GET 프로젝트 시간순
    @GetMapping()
    public List<Project> getAllProjectsByCreatedDate() {
        List<Project> projects = projectRepository.findAllByOrderByCreatedDateDesc();
        return projects;
    }

    // GET 프로젝트 데드라인 정렬
    @GetMapping("/sorted")
    public List<Project> getAllProjectsByApplicationDeadline() {
        Sort sort = Sort.by(Sort.Order.asc("applicationDeadline"), Sort.Order.desc("liked"));
        return projectRepository.findByApplicationDeadlineAfter(LocalDate.now(), sort);
    }

    // 새로운 프로젝트 POST
    @PostMapping
    public Project createProject(
            @RequestParam String projectName,
            @RequestParam Project.ProjectCategory projectArtCategory,
            @RequestParam(defaultValue="Unknown") String location,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate projectDeadline,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate applicationDeadline,
            @RequestParam List<Project.RequiredCategory> requiredCategory,
            @RequestParam(defaultValue = "true") Boolean swipeAlgorithm,
            @RequestParam(value = "image") List<MultipartFile> projectImageFiles,
            @RequestParam String description,
            @RequestParam(defaultValue = "true") Boolean ongoingStatus,
            @RequestParam(defaultValue = "both") String remoteStatus,
            @RequestParam(defaultValue = "0") Long requiredPeople,
            @ApiIgnore Authentication authentication) {

        org.springframework.security.core.userdetails.User principal = (org.springframework.security.core.userdetails.User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = authentication.getName();

        Project project = new Project();
        Set<ProjectImages> projectImages = new HashSet<>();

        for (MultipartFile projectImageFile : projectImageFiles) {
            try {
                String extraKey = projectS3Service.projectSaveUploadFile(projectImageFile);
                URL projectImageUrl = s3Client.getUrl("atogato", extraKey);

                ProjectImages projectImage = new ProjectImages();
                projectImage.setImageUrl(projectImageUrl.toString());
                projectImage.setProject(project);
                projectImages.add(projectImage);
            }catch(IOException e) {
                e.printStackTrace();
            }
        }

        project.setUserId(userId);
        project.setProjectName(projectName);
        project.setProjectArtCategory(projectArtCategory);
        project.setLocation(location);
        project.setCreatedDate(LocalDate.now());
        project.setOngoingStatus(ongoingStatus);
        project.setRemoteStatus(remoteStatus);
        project.setProjectDeadline(projectDeadline);
        project.setApplicationDeadline(applicationDeadline);
        project.setRequiredCategory(requiredCategory);
        project.setRequiredPeople(requiredPeople);
        project.setSwipeAlgorithm(swipeAlgorithm);
        project.setDescription(description);

        return projectRepository.save(project);
    }

    // 특정 프로젝트 PUT
    @PatchMapping("/{id}")
    public ResponseEntity<?> updateProject(@PathVariable Long id, @RequestParam Map<String, Object> updates) {
        Project existingProject = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트 ID 찾을 수 없음: " + id));

        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        if (!existingProject.getUserId().equals(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("프로젝트 관리자가 아닙니다");
        }

        if (updates.containsKey("projectName")) {
            existingProject.setProjectName((String) updates.get("projectName"));
        }
        if (updates.containsKey("projectArtCategory")) {
            existingProject.setProjectArtCategory(Project.ProjectCategory.valueOf((String) updates.get("projectArtCategory")));
        }
        if (updates.containsKey("location")) {
            existingProject.setLocation((String) updates.get("location"));
        }
        if (updates.containsKey("projectDeadline")) {
            existingProject.setProjectDeadline(LocalDate.parse((String) updates.get("projectDeadline")));
        }
        if (updates.containsKey("applicationDeadline")) {
            existingProject.setApplicationDeadline(LocalDate.parse((String) updates.get("applicationDeadline")));
        }
        if (updates.containsKey("requiredPeople")) {
            existingProject.setRequiredPeople(Long.valueOf((String) updates.get("requiredPeople")));
        }
        if (updates.containsKey("requiredCategory")) {
            List<Project.RequiredCategory> categories = Arrays.asList((Project.RequiredCategory[]) updates.get("requiredCategory"));
            existingProject.setRequiredCategory(categories);
        }
        if (updates.containsKey("swipeAlgorithm")) {
            existingProject.setSwipeAlgorithm(Boolean.parseBoolean((String) updates.get("swipeAlgorithm")));
        }
        if (updates.containsKey("description")) {
            existingProject.setDescription((String) updates.get("description"));
        }
        if (updates.containsKey("ongoingStatus")) {
            existingProject.setOngoingStatus(Boolean.parseBoolean((String) updates.get("ongoingStatus")));
        }
        if (updates.containsKey("remoteStatus")) {
            existingProject.setRemoteStatus((String) updates.get("remoteStatus"));
        }

        return ResponseEntity.ok(projectRepository.save(existingProject));
    }

    // 특정 프레제트 DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트 ID 찾을 수 없음: " + id));
        if (!project.getUserId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("프로젝트 관리자가 아닙니다");
        }

        projectRepository.delete(project);
        return ResponseEntity.ok("프로젝트 삭제 성공");

    }

}
