package no.di.graphqlpoc.resolver

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.signer.Aws4Signer
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams
import software.amazon.awssdk.http.SdkHttpFullRequest
import software.amazon.awssdk.http.SdkHttpMethod
import com.google.gson.Gson
import software.amazon.awssdk.auth.signer.internal.SignerConstant.AUTHORIZATION
import software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.HOST
import software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.X_AMZ_DATE
import software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.X_AMZ_SECURITY_TOKEN
import software.amazon.awssdk.regions.Region
import java.net.URI

class NeptuneAuthToken private constructor(
    private val region: String,
    val url: String,
    private val credentialsProvider: AwsCredentialsProvider
) {

    private val gson = Gson()

    fun getAuthToken(): String {
        val request = SdkHttpFullRequest.builder()
            .method(SdkHttpMethod.GET)
            .uri(URI.create(url))
            .encodedPath("/opencypher")
            .build()

        val signer = Aws4Signer.create()
        val signingParams = Aws4SignerParams.builder()
            .awsCredentials(credentialsProvider.resolveCredentials())
            .signingName(SERVICE_NAME)
            .signingRegion(Region.of(region))
            .build()

        val signedRequest = signer.sign(request, signingParams)

        return getAuthInfoJson(signedRequest)
    }

    private fun getAuthInfoJson(request: SdkHttpFullRequest): String {
        val obj = HashMap<String, Any?>()
        obj[AUTHORIZATION] = request.headers()[AUTHORIZATION]
        obj[HTTP_METHOD_HDR] = request.method().name
        obj[X_AMZ_DATE] = request.headers()[X_AMZ_DATE]
        obj[HOST] = request.headers()[HOST]
        obj[X_AMZ_SECURITY_TOKEN] = request.headers()[X_AMZ_SECURITY_TOKEN]
        return gson.toJson(obj)
    }

    companion object {
        private const val SERVICE_NAME = "neptune-db"
        private const val HTTP_METHOD_HDR = "HttpMethod"

        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private lateinit var region: String
        private lateinit var url: String
        private lateinit var credentialsProvider: AwsCredentialsProvider

        fun region(region: String) = apply { this.region = region }
        fun url(url: String) = apply { this.url = url }
        fun credentialsProvider(credentialsProvider: AwsCredentialsProvider) =
            apply { this.credentialsProvider = credentialsProvider }

        fun build() = NeptuneAuthToken(region, url, credentialsProvider)
    }
}