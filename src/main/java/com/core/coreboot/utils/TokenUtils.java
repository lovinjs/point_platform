package com.core.coreboot.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.core.coreboot.common.Constant;
import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.user.entity.User;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * Token 工具类
 */
public class TokenUtils {

    /**
     * 生成 token
     * @param user 用户实体类
     * @return token
     */
    public static String generateToken(User user) {
        Algorithm algorithm = Algorithm.HMAC256(Constant.JWT_KEY);
        return JWT.create()
            .withClaim(Constant.USER_ID, user.getId())
            .withClaim(Constant.USER_NAME, user.getName())
            .withClaim(Constant.USER_SIGNATURE, user.getSignature())
            .withClaim(Constant.USER_ROLE, user.getRole())
            .withExpiresAt(new Date(System.currentTimeMillis() + Constant.EXPIRE_TIME_MILLIS))
            .sign(algorithm);
    }

    /**
     * 获取真实 token
     * @param request HttpServletRequest
     * @return token
     */
    public static String getToken(HttpServletRequest request) {
        String token = null;
        String authToken = request.getHeader("Authorization");
        if (authToken != null && authToken.startsWith("Bearer ")) {
            token = authToken.substring(7);
        }
        return token;
    }

    /**
     * 验证 token
     * @param token token
     * @return 用户实体类
     */
    public static User verifyToken(String token) {
        Algorithm algorithm = Algorithm.HMAC256(Constant.JWT_KEY);
        JWTVerifier jwtVerifier = JWT.require(algorithm).build();
        try {
            DecodedJWT jwt = jwtVerifier.verify(token);
            return User.builder()
                    .id(jwt.getClaim(Constant.USER_ID).asInt())
                    .name(jwt.getClaim(Constant.USER_NAME).asString())
                    .signature(jwt.getClaim(Constant.USER_SIGNATURE).asString())
                    .role(jwt.getClaim(Constant.USER_ROLE).asInt())
                    .build();
        } catch (TokenExpiredException e) {
            throw new CustomException(ExceptionEnum.TOKEN_EXPIRED);
        } catch (JWTDecodeException e) {
            throw new CustomException(ExceptionEnum.TOKEN_WRONG);
        }
    }
}
