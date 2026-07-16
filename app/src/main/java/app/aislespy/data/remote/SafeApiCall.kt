package app.aislespy.data.remote

import app.aislespy.data.remote.dto.ProductResponseDto
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Maps a Retrofit [Response] (or thrown network/serialization failure) to [ProductApiResult]
 * per API_CONTRACTS.md error table.
 */
suspend fun safeProductApiCall(
    block: suspend () -> Response<ProductResponseDto>,
): ProductApiResult {
    return try {
        val response = block()
        mapProductResponse(response)
    } catch (e: SocketTimeoutException) {
        ProductApiResult.Error(e.message ?: "Request timed out")
    } catch (e: SerializationException) {
        ProductApiResult.Error(e.message ?: "Malformed JSON")
    } catch (e: HttpException) {
        mapHttpCode(e.code(), e.message())
    } catch (e: IOException) {
        ProductApiResult.Error(e.message ?: "Network error")
    } catch (e: Exception) {
        // Malformed JSON from converter often surfaces as RuntimeException wrapping SerializationException
        val cause = e.cause
        when {
            cause is SerializationException || e is SerializationException ->
                ProductApiResult.Error(e.message ?: "Malformed JSON")
            else -> ProductApiResult.Error(e.message ?: "Network error")
        }
    }
}

internal fun mapProductResponse(response: Response<ProductResponseDto>): ProductApiResult {
    val code = response.code()
    when (code) {
        404 -> return ProductApiResult.NotFound
        429 -> return ProductApiResult.Error(TOO_MANY_REQUESTS_MESSAGE)
    }
    if (code in 500..599) {
        return ProductApiResult.Error(response.message().ifBlank { "Server error ($code)" })
    }
    if (!response.isSuccessful) {
        return ProductApiResult.Error(response.message().ifBlank { "HTTP $code" })
    }

    val body = response.body()
        ?: return ProductApiResult.NotFound

    return when {
        body.status == 1 && body.product != null -> ProductApiResult.Found(body)
        body.status == 0 || body.product == null -> ProductApiResult.NotFound
        else -> ProductApiResult.NotFound
    }
}

private fun mapHttpCode(code: Int, message: String?): ProductApiResult {
    return when (code) {
        404 -> ProductApiResult.NotFound
        429 -> ProductApiResult.Error(TOO_MANY_REQUESTS_MESSAGE)
        in 500..599 -> ProductApiResult.Error(message?.ifBlank { null } ?: "Server error ($code)")
        else -> ProductApiResult.Error(message?.ifBlank { null } ?: "HTTP $code")
    }
}

/** Exact user-facing copy for rate limiting (API_CONTRACTS.md). */
const val TOO_MANY_REQUESTS_MESSAGE = "Too many requests—try again shortly"
