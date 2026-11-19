package com.zyh.sleep.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtUtilTest {

    private static final String SECRET = "sleep-tracker-test-secret-key-0123456789";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expireHours", 72L);
    }

    @Test
    @DisplayName("生成的 token 能解析出原来的 userId")
    void generateAndParse() {
        String token = jwtUtil.generate(1L);
        assertEquals(Long.valueOf(1L), jwtUtil.parseUserId(token));
    }

    @Test
    @DisplayName("内容被篡改的 token 验签失败，返回 null")
    void tamperedToken() {
        String token = jwtUtil.generate(2L);
        String[] parts = token.split("\\.");
        char first = parts[1].charAt(0);
        parts[1] = (first == 'A' ? 'B' : 'A') + parts[1].substring(1);
        String tampered = parts[0] + "." + parts[1] + "." + parts[2];
        assertNull(jwtUtil.parseUserId(tampered));
    }

    @Test
    @DisplayName("用其他密钥签发的 token 验签失败")
    void wrongSecret() {
        JwtUtil other = new JwtUtil();
        ReflectionTestUtils.setField(other, "secret", "another-secret-key-abcdefghijklmnopqrstuvwxyz");
        ReflectionTestUtils.setField(other, "expireHours", 72L);

        String token = other.generate(1L);
        assertNull(jwtUtil.parseUserId(token));
    }

    @Test
    @DisplayName("已过期的 token 返回 null")
    void expiredToken() {
        ReflectionTestUtils.setField(jwtUtil, "expireHours", -1L);
        String token = jwtUtil.generate(1L);
        assertNull(jwtUtil.parseUserId(token));
    }

    @Test
    @DisplayName("非法字符串返回 null，不抛异常")
    void invalidToken() {
        assertNull(jwtUtil.parseUserId("not-a-token"));
    }
}
