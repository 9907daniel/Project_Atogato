package portfolio.backend.api.project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import portfolio.backend.api.project.entity.Project;
import portfolio.backend.api.project.repository.ProjectRepository;

import portfolio.backend.api.project.exception.ResourceNotFoundException; // Import statement for ResourceNotFoundException
import portfolio.backend.authentication.api.entity.user.User;
import portfolio.backend.authentication.api.repository.user.UserRepository;
import portfolio.backend.authentication.api.service.UserService;
import springfox.documentation.annotations.ApiIgnore;


import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectRepository projectRepository;
    private final UserService userService;
    private final UserRepository userRepository;


    @Autowired // project Parameter 생성
    public ProjectController(ProjectRepository projectRepository, UserRepository userRepository, UserService userService) {
        this.projectRepository = projectRepository;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    // 전체 프로젝트 GET
    @GetMapping
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }


    // 특정 프로젝트 PK Param GET
    @GetMapping("/{id}")
    public Project getProjectById(@PathVariable Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ID not found: " + id));
    }


    // 새로운 프로젝트 POST
    @PostMapping
    public Project createProject(
            @RequestParam String projectName,
            @RequestParam String creatorArtCategory,
            @RequestParam(defaultValue="0") Long liked,
            @RequestParam(defaultValue="Unknown") String location,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadline,
            @RequestParam List<String> requiredCategory,
            @RequestParam(defaultValue = "true") Boolean swipeAlgorithm,
            @RequestParam(defaultValue = "None") String image,
            @RequestParam String description,
            @RequestParam(defaultValue = "true") Boolean ongoingStatus,
            @RequestParam(defaultValue = "both") String remoteStatus,
            @RequestParam(defaultValue = "0") List<Long> requiredPeople,
            @RequestParam(defaultValue = "0") Long participantId,
            @ApiIgnore Authentication authentication) {

        org.springframework.security.core.userdetails.User principal = (org.springframework.security.core.userdetails.User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

//        User user = userService.getUser(principal.getUsername());
        String userId = authentication.getName();

        Project project = new Project();

        project.setUserId(userId);
        project.setProjectName(projectName);
        project.setCreatorArtCategory(creatorArtCategory);
        project.setLiked(liked);
        project.setLocation(location);
        project.setCreatedDate(LocalDate.now());
        project.setOngoingStatus(ongoingStatus);
        project.setRemoteStatus(remoteStatus);
        project.setDeadline(deadline);
        project.setRequiredCategory(requiredCategory);
        project.setRequiredPeople((long) requiredCategory.size()); // Calculate the length of the requiredPeople list
        project.setSwipeAlgorithm(swipeAlgorithm);
        project.setImage(image);
        project.setDescription(description);
        project.setParticipantId(participantId);

        return projectRepository.save(project);
    }

    // 특정 프로젝트 PUT
    @PutMapping("/{id}")
    public Project updateProject(@PathVariable Long id, @RequestBody Project updatedProject) {
        Project existingProject = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ID not found: " + id));

        existingProject.setProjectName(updatedProject.getProjectName());
        existingProject.setCreatedDate(updatedProject.getCreatedDate());
        existingProject.setCreatorArtCategory(updatedProject.getCreatorArtCategory());
        existingProject.setLocation(updatedProject.getLocation());
        existingProject.setSwipeAlgorithm(updatedProject.getSwipeAlgorithm());
        existingProject.setLiked(updatedProject.getLiked());
        existingProject.setImage(updatedProject.getImage());
        existingProject.setRequiredCategory(updatedProject.getRequiredCategory());
        existingProject.setRequiredPeople((long) updatedProject.getRequiredCategory().size()); // Calculate the length of the requiredPeople list
        existingProject.setDeadline(updatedProject.getDeadline());
        existingProject.setOngoingStatus(updatedProject.getOngoingStatus());
        existingProject.setRemoteStatus(updatedProject.getRemoteStatus());
        existingProject.setDescription(updatedProject.getDescription());
        existingProject.setParticipantId(updatedProject.getParticipantId());
        return projectRepository.save(existingProject);
    }

    // 특정 프레제트 DELETE
    @DeleteMapping("/{id}")
    public void deleteProject(@PathVariable Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ID not found: " + id));
        projectRepository.delete(project);
    }


}
