package com.pswied.core.nostra.account;

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
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/accounts")
    public List<AccountDto> listAccounts() {
        return accountService.listAccounts();
    }

    @GetMapping("/accounts/{accountNumber}")
    public AccountDto getAccount(@PathVariable String accountNumber) {
        return accountService.getAccount(accountNumber);
    }

    @PostMapping("/transfers")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(accountService.transfer(request));
    }

    @GetMapping("/transfers/{reference}")
    public TransferResponse getTransfer(@PathVariable String reference) {
        return accountService.getTransfer(reference);
    }
}
