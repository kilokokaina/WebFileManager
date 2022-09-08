package com.example.webfilemanager.controller;

import com.example.webfilemanager.service.MediaTypeUtils;
import com.example.webfilemanager.service.ZipService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.ZipOutputStream;

@Slf4j
@Controller
public class MainController {
    private final ServletContext servletContext;
    public final static String DEFAULT_PATH = "/Users";

    @Autowired
    public MainController(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @GetMapping("/")
    public String hello() {
        return "redirect:/path";
    }

    @GetMapping("/path")
    public String directoryList(@RequestParam(defaultValue = DEFAULT_PATH) String filePath,
                                              Model model) {
        if (!filePath.contains(DEFAULT_PATH)) return "redirect:/";

        File file = new File(filePath);
        if (file.isDirectory()) {
            String[] fileParentPath = file.getParent().split("/");
            StringBuilder lastFilePath = new StringBuilder();

            Arrays.stream(fileParentPath).toList().forEach(item -> lastFilePath.append("/").append(item));

            if (fileParentPath.length < 6) model.addAttribute("file", file);
            else {
                File[] resultPathString = {
                        new File(DEFAULT_PATH),
                        new File(String.valueOf(lastFilePath))
                };
                model.addAttribute("parentList", resultPathString);
            }

            model.addAttribute("fileList", getFileList(filePath));
            model.addAttribute("title", file.getName());
        } else return String.format("redirect:/download?filePath=%s",
                    file.getAbsolutePath());

        return "index";
    }

    @GetMapping("/download")
    public @ResponseBody ResponseEntity<InputStreamResource> downloadFile(
            @RequestParam String filePath) throws IOException {
        MediaType mediaType = MediaTypeUtils.getMediaTypeForFileName(servletContext, filePath);

        File file = new File(filePath);
        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + file.getName())
                .contentType(mediaType)
                .contentLength(file.length())
                .body(resource);
    }

    @GetMapping("/makezip")
    public String makeZip(@RequestParam String filePath) throws IOException{
        File file = new File(filePath);

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(
                new FileOutputStream(file.getAbsolutePath().concat(".zip")))) {
            ZipService.zipFolder(file, file.getName(), zipOutputStream);
        }

        return String.format("redirect:/path?filePath=%s",
                file.getParent());
    }

    private boolean isHiddenFolder(String fileName) {
        String[] symbols = fileName.split("");
        return symbols[0].equals(".");
    }

    private ArrayList<File> getFileList(String file) {
        File fileDir = new File(file);
        ArrayList<File> fileList = new ArrayList<>();

        if (fileDir.isDirectory()) {
            for(File item : Objects.requireNonNull(fileDir.listFiles())) {
                if (!isHiddenFolder(item.getName())) {
                    fileList.add(item);
                }
            }
        }

        return fileList;
    }
}
