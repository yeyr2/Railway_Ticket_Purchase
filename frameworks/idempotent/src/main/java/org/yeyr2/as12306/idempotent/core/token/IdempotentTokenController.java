package org.yeyr2.as12306.idempotent.core.token;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yeyr2.as12306.convention.result.Result;
import org.yeyr2.as12306.web.Results;

// 基于token验证请求幂等性控制器
@RestController
@RequiredArgsConstructor
public class IdempotentTokenController {
    private final IdempotentTokenService idempotentTokenService;

    // 请求token
    @GetMapping("/token")
    public Result<String> createToken(){
        return Results.success(idempotentTokenService.createToken());
    }
}
