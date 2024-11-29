package no.di.graphqlpoc.resolver

import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class HelloWorldResolver {

    @QueryMapping
    fun helloWorld(): String {
        return "Hello, World!"
    }
}