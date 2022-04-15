package com.example.mall.order.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@Controller
public class HelloController {

    // 仅用于测试
    @GetMapping(value = "/{page}.html")
    public String listPage(@PathVariable("page") String page) {
        return page;
    }

}
