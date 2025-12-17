package org.example.oh_mock.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
public class UploadController {

    @PostMapping("/api/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) return "";

        // 저장 경로 설정 (실행 위치의 uploads 폴더)
        String uploadDir = new File("uploads").getAbsolutePath();
        new File(uploadDir).mkdirs();

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        file.transferTo(new File(uploadDir + "/" + fileName));

        // 접근 가능한 URL 반환
        return "/Oh_Mock/images/" + fileName;
    }
}