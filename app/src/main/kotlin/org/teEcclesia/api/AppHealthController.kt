package org.teEcclesia.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/app/health")
class AppHealthController {
    @GetMapping("/")
    fun home(): String = "app OK"
}