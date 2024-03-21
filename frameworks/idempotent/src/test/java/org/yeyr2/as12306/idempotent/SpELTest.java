package org.yeyr2.as12306.idempotent;

import org.springframework.stereotype.Component;
import org.yeyr2.as12306.idempotent.annotation.Idempotent;
import org.yeyr2.as12306.idempotent.enums.IdempotentSceneEnum;
import org.yeyr2.as12306.idempotent.enums.IdempotentTypeEnum;

@Component
public class SpELTest {
    @Idempotent(
            uniqueKeyPrefix = "as12306-user:lock_passenger-alter:",
            key = "T(org.yeyr2.as12306.bizs.user.core.UserContext).getUsername()",
            type = IdempotentTypeEnum.SPEL,
            scene = IdempotentSceneEnum.RESTAPI
    )
    public void SpELKeyTest(String username){
        System.out.println(username);
    }
}
