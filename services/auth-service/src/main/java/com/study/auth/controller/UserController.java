package com.study.auth.controller;

import com.study.auth.common.Result;
import com.study.auth.model.dto.PageResult;
import com.study.auth.model.dto.UserDTO;
import com.study.auth.model.entity.User;
import com.study.auth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Value("${app.upload.dir:./avatars}")
    private String uploadDir;

    @GetMapping("/list")
    public Result<List<UserDTO>> list(@RequestParam Long tenantId) {
        return Result.ok(userService.list(tenantId));
    }

    @GetMapping("/page")
    public Result<PageResult<UserDTO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam Long tenantId) {
        return Result.ok(userService.page(page, size, tenantId));
    }

    @GetMapping("/{id:[0-9]+}")
    public Result<UserDTO> getById(@PathVariable Long id) {
        return Result.ok(userService.getById(id));
    }

    @PostMapping
    public Result<?> create(@RequestBody User user) {
        userService.create(user);
        return Result.ok();
    }

    @PutMapping
    public Result<?> update(@RequestBody User user) {
        userService.update(user);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/password")
    public Result<?> updatePassword(@PathVariable Long id, @RequestBody String newPassword) {
        userService.updatePassword(id, newPassword);
        return Result.ok();
    }

    @GetMapping("/{id}/roles")
    public Result<List<Long>> getUserRoles(@PathVariable Long id) {
        return Result.ok(userService.getUserRoleIds(id));
    }

    @PutMapping("/{id}/roles")
    public Result<?> assignRoles(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        userService.assignRoles(id, body.get("roleIds"));
        return Result.ok();
    }

    @GetMapping("/profile")
    public Result<UserDTO> profile(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(userService.getProfile(userId));
    }

    @PostMapping("/avatar")
    public Result<?> uploadAvatar(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("file") MultipartFile file) {
        try {
            String ext = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
            String filename = userId + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            Path dir = Paths.get(uploadDir);
            if (!Files.exists(dir)) Files.createDirectories(dir);
            file.transferTo(dir.resolve(filename).toFile());
            userService.updateAvatar(userId, "/api/users/avatar-file/" + filename);
            return Result.ok(filename);
        } catch (Exception e) {
            return Result.fail(500, "头像上传失败");
        }
    }

    @GetMapping("/avatar-file/{filename}")
    public ResponseEntity<Resource> getAvatarFile(@PathVariable String filename) {
        try {
            Path file = Paths.get(uploadDir).resolve(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                String contentType = filename.endsWith(".png") ? "image/png" : "image/jpeg";
                return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
