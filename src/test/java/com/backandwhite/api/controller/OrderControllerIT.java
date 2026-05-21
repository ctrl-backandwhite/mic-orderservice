package com.backandwhite.api.controller;

import com.backandwhite.BaseIntegrationTest;
import com.backandwhite.core.test.JwtTestUtil;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OrderControllerIT extends BaseIntegrationTest {

    @Autowired
    private JwtTestUtil jwtTestUtil;

    @Test
    void getAllOrders_withAdminToken_contextLoadsAndResponds() {
        String adminToken = jwtTestUtil.getToken("admin-user", List.of("ROLE_ADMIN"));

        webTestClient.get().uri("/api/v1/orders").header("Authorization", adminToken)
                .header("X-nx036-auth", "internal-test-token").exchange().expectStatus().isOk();
    }

    @Test
    void getOrders_withoutToken_returns4xx() {
        webTestClient.get().uri("/api/v1/orders").exchange().expectStatus().is4xxClientError();
    }
}
