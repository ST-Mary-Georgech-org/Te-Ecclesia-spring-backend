package org.teEcclesia.api

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class DocsController {
    @GetMapping("/")
    fun home(): String = "redirect:/swagger-ui/index.html"
}