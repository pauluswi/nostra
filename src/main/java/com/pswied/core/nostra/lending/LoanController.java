package com.pswied.core.nostra.lending;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping("/loans")
    public ResponseEntity<LoanResponse> createLoan(@Valid @RequestBody LoanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.createLoan(request));
    }

    @GetMapping("/loans/{loanReference}")
    public LoanResponse getLoan(@PathVariable String loanReference) {
        return loanService.getLoan(loanReference);
    }

    @GetMapping("/loans/{loanReference}/schedule")
    public List<LoanScheduleEntryDto> getSchedule(@PathVariable String loanReference) {
        return loanService.getLoanSchedule(loanReference);
    }

    @PostMapping("/loans/{loanReference}/repayments")
    public LoanRepaymentResponse repay(@PathVariable String loanReference,
                                      @Valid @RequestBody LoanRepaymentRequest request) {
        return loanService.applyRepayment(loanReference, request);
    }
}
