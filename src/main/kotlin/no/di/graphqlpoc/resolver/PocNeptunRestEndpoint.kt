package no.di.graphqlpoc.resolver

import org.neo4j.driver.GraphDatabase
import org.neo4j.driver.Session
import org.neo4j.driver.AuthTokens
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import com.google.gson.JsonParser
import org.neo4j.driver.Driver

@RestController
@RequestMapping("/neptun")
class PocNeptunRestEndpoint {

    private val logger = LoggerFactory.getLogger(PocNeptunRestEndpoint::class.java)

    @GetMapping("/test")
    fun test(): Mono<String> {
        //write
        val neptunUrl = "bolt://db-neptune-1.cluster-chxpcmcfjtfp.eu-north-1.neptune.amazonaws.com:8182"
        val authUrl = "https://db-neptune-1.cluster-chxpcmcfjtfp.eu-north-1.neptune.amazonaws.com:8182"
        val region = "eu-north-1"
        val credentialsProvider = DefaultCredentialsProvider.create()

        logger.info("Connecting to Neptune database at $neptunUrl in region $region")

        val authTokenJson = NeptuneAuthToken.builder()
            .region(region)
            .url(authUrl)
            .credentialsProvider(credentialsProvider)
            .build()
            .getAuthToken()

        val jsonElement = JsonParser.parseString(authTokenJson)
        val authToken = jsonElement.asJsonObject["Authorization"].asJsonArray[0].asString

        val driver: Driver = GraphDatabase.driver(neptunUrl, AuthTokens.bearer(authToken))
        val session: Session = driver.session()

        val cypherQuery = """
            CREATE (n:Person {name: 'John Doe', age: 30})
            RETURN n
        """.trimIndent()

        logger.info("Executing Cypher query: $cypherQuery")

        return Mono.fromCallable {
            session.executeWrite { tx ->
                tx.run(cypherQuery).single().get(0).asString()
            }
        }.doOnError {
            logger.error("Error executing query: ${it.message}", it)
        }.doOnSuccess {
            logger.info("Query executed successfully")
        }.onErrorReturn("Query failed")
    }

    @GetMapping("/health")
    fun healthCheck(): Mono<String> {
        val neptunUrl = "bolt://db-neptune-1.cluster-ro-chxpcmcfjtfp.eu-north-1.neptune.amazonaws.com:8182"
        val authUrl = "https://db-neptune-1.cluster-ro-chxpcmcfjtfp.eu-north-1.neptune.amazonaws.com:8182"
        val region = "eu-north-1"
        val credentialsProvider = DefaultCredentialsProvider.create()

        /*
        The log message indicates that the AWS credentials were successfully found and used by the
        PocNeptunRestEndpoint class. This means that the DefaultCredentialsProvider was
        able to resolve the credentials, likely from your AWS Toolkit session or another configured source.

        Yes, based on the log message indicating that AWS credentials were successfully found, it confirms that
        you are logged in and the DefaultCredentialsProvider was able to resolve
        the credentials. This likely means that the credentials were obtained from your AWS Toolkit session or another configured source
         */
        try {
            val credentials = credentialsProvider.resolveCredentials()
            logger.info("AWS credentials found: ${credentials.accessKeyId()}")
        } catch (e: Exception) {
            logger.error("No valid AWS session found: ${e.message}")
            return Mono.error<String>(RuntimeException("No valid AWS session found"))
        }

        return Mono.fromCallable {
            val authTokenJson = NeptuneAuthToken.builder()
                .region(region)
                .url(authUrl)
                .credentialsProvider(credentialsProvider)
                .build()
                .getAuthToken()

            val jsonElement = JsonParser.parseString(authTokenJson)
            val authToken = jsonElement.asJsonObject["Authorization"].asJsonArray[0].asString

            val driver: Driver = GraphDatabase.driver(neptunUrl, AuthTokens.bearer(authToken))
            val session: Session = driver.session()

            session.run("RETURN 1").single().get(0).asInt()
        }.map { "Neptune is running" }
            .doOnError {
                logger.error("Error connecting to Neptune: ${it.message}", it)
            }.onErrorReturn("Neptune is not running")
    }
}