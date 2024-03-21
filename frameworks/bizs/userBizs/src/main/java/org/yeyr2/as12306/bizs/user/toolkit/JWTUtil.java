package org.yeyr2.as12306.bizs.user.toolkit;

import com.alibaba.fastjson2.JSON;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.yeyr2.as12306.base.constant.UserConstant;
import org.yeyr2.as12306.bizs.user.core.UserInfoDTO;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class JWTUtil {
    private static final long EXPIRATION = 86400L;
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String ISS = "as12306";
    public static final String SECRET = "SECRETKEYas12306123fg23uiqregh980ewhf123rf213fdgf";

    // 生成用户访问token
    public static String generateAccessToken(UserInfoDTO userInfoDTO){
        Map<String, Object> customerUserMap = new HashMap<>();
        customerUserMap.put(UserConstant.USER_ID,userInfoDTO.getUserId());
        customerUserMap.put(UserConstant.USERNAME,userInfoDTO.getUsername());
        customerUserMap.put(UserConstant.USER_REAL_NAME,userInfoDTO.getRealName());
        customerUserMap.put(UserConstant.USER_TOKEN,userInfoDTO.getToken());
        String jwtToken = Jwts.builder()
                .signWith(SignatureAlgorithm.ES256,SECRET)
                .setIssuedAt(new Date())
                .setIssuer(ISS)
                .setSubject(JSON.toJSONString(customerUserMap))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION * 1000))
                .compact();
        return TOKEN_PREFIX + jwtToken;
    }

    /**
     * 解析用户 Token
     *
     * @param jwtToken 用户访问 Token
     * @param hasPrefix token前置信息是否未已经被处理
     * @return 用户信息
     */
    public static UserInfoDTO parseJwtToken(String jwtToken,boolean hasPrefix) {
        if (StringUtils.hasText(jwtToken)) {
            if(hasPrefix && !jwtToken.startsWith(TOKEN_PREFIX)){
                return null;
            }
            String actualJwtToken = jwtToken.replace(TOKEN_PREFIX, "");
            try {
                Claims claims = Jwts.parser().setSigningKey(SECRET).parseClaimsJws(actualJwtToken).getBody();
                Date expiration = claims.getExpiration();
                if (expiration.after(new Date())) {
                    String subject = claims.getSubject();
                    return JSON.parseObject(subject, UserInfoDTO.class);
                }
            } catch (ExpiredJwtException ignored) {
            } catch (Exception ex) {
                log.error("JWT Token解析失败，请检查", ex);
            }
        }
        return null;
    }
}
