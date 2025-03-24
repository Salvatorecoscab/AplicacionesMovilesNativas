package com.example.helloworld

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class HelloWorldApplication

fun main(args: Array<String>) {
	runApplication<HelloWorldApplication>(*args)
}

@RestController
@RequestMapping("/api")
class HelloWorldController {
	@GetMapping("/hello")
	fun helloWorld(): Message {
		return Message("Hola Mundo")
	}
}

data class Message(val message: String)