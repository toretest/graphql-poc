package no.di.graphqlpoc

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GraphqlPocApplication

fun main(args: Array<String>) {
    runApplication<GraphqlPocApplication>(*args)
}
