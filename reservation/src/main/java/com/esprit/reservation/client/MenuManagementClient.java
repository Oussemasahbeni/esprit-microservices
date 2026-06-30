package com.esprit.reservation.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "menu-management")
public interface MenuManagementClient {

    @GetMapping("/api/menus")
    MenuSnapshot getMenu();
}
