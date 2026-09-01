package com.landoulsi.survey.testing

import com.landoulsi.survey.SurveyClient
import com.landoulsi.survey.SurveyNetworkException
import com.landoulsi.survey.model.SurveyDefinition
import com.landoulsi.survey.model.SurveyResponse
import kotlinx.coroutines.CompletableDeferred

/**
 * In-memory [SurveyClient] for tests, previews and demos — no network.
 *
 * Configure [definition] (returned by [fetchDefinition]) and optionally [fetchError] /
 * [submitError] to exercise failure paths. Calls are recorded in [fetchedUrls] and
 * [submissions].
 */
class FakeSurveyClient(
    var definition: SurveyDefinition? = null,
    var fetchError: Throwable? = null,
    var submitError: Throwable? = null,
) : SurveyClient {

    val fetchedUrls: MutableList<String> = mutableListOf()
    val submissions: MutableList<Submission> = mutableListOf()

    /**
     * When set, [submit] suspends on this until it completes — lets a test observe the
     * in-flight `submitting = true` state and probe re-entrancy.
     */
    var submitGate: CompletableDeferred<Unit>? = null

    var closed: Boolean = false
        private set

    data class Submission(val url: String, val response: SurveyResponse)

    override suspend fun fetchDefinition(url: String): SurveyDefinition {
        fetchedUrls += url
        fetchError?.let { throw it }
        return definition
            ?: throw SurveyNetworkException("FakeSurveyClient: no definition configured for $url")
    }

    override suspend fun submit(url: String, response: SurveyResponse) {
        submitGate?.await()
        submissions += Submission(url, response)
        submitError?.let { throw it }
    }

    override fun close() {
        closed = true
    }
}
