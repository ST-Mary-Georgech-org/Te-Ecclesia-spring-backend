package org.teEcclesia.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@ComponentScan(basePackages = ["org.teEcclesia"])
@EnableJpaRepositories(basePackages = ["org.teEcclesia"])
@EntityScan(basePackages = ["org.teEcclesia"])
@EnableScheduling
@EnableAsync
class TeEcclesiaApplication

fun main(args: Array<String>) {
	runApplication<TeEcclesiaApplication>(*args)
}
