package com.lendlogic;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping("/health")
    public Map<String, String> healthCheck() {
        return Map.of("status", "LendLogic API is running");
    }

    @PostMapping("/recommend")
    public LoanService.LoanResult recommendLoan(@RequestBody LoanService.LoanInput input) {
        return loanService.recommend(input);
    }

    @PostMapping("/amortization")
    public List<LoanService.ScheduleEntry> generateSchedule(
            @RequestBody LoanService.LoanInput input) {
        return loanService.generateAmortizationSchedule(input);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidInput(
            IllegalArgumentException exception) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", exception.getMessage()));
    }
}