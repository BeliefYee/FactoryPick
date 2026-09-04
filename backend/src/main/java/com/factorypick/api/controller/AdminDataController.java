package com.factorypick.api.controller;

import com.factorypick.api.domain.DataImport;
import com.factorypick.api.dto.ImportResult;
import com.factorypick.api.service.DataImportService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/admin/data")
public class AdminDataController {
    private final DataImportService service;
    public AdminDataController(DataImportService service) { this.service = service; }

    @PostMapping(value="/imports/csv", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult importCsv(@RequestPart("file") MultipartFile file) { return service.importCsv(file); }
    @GetMapping("/imports")
    public List<DataImport> history() { return service.history(); }
}
