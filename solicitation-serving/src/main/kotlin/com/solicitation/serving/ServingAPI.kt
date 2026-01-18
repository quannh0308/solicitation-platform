package com.solicitation.serving

import com.solicitation.model.Candidate

/**
 * Low-latency serving API for retrieving eligible candidates for customers.
 * 
 * Provides endpoints for single and batch customer queries with channel-specific
 * filtering and ranking.
 */
interface ServingAPI {
    
    /**
     * Retrieves candidates for a single customer.
     * 
     * @param request Request parameters for candidate retrieval
     * @return Response containing candidates and metadata
     */
    fun getCandidatesForCustomer(request: GetCandidatesRequest): GetCandidatesResponse
    
    /**
     * Retrieves candidates for multiple customers in a single request.
     * 
     * @param request Batch request parameters
     * @return Response containing candidates per customer
     */
    fun getCandidatesForCustomers(request: BatchGetCandidatesRequest): BatchGetCandidatesResponse
}

/**
 * Request for retrieving candidates for a single customer.
 * 
 * @property customerId Customer identifier
 * @property channel Optional channel filter (e.g., "email", "in-app", "push")
 * @property program Optional program filter
 * @property marketplace Marketplace identifier
 * @property limit Maximum number of candidates to return (default 10)
 * @property includeScores Whether to include score details in response
 * @property refreshEligibility Whether to perform real-time eligibility refresh
 */
data class GetCandidatesRequest(
    val customerId: String,
    val channel: String? = null,
    val program: String? = null,
    val marketplace: String,
    val limit: Int = 10,
    val includeScores: Boolean = false,
    val refreshEligibility: Boolean = false
)

/**
 * Response containing candidates for a single customer.
 * 
 * @property candidates List of eligible candidates
 * @property metadata Response metadata
 * @property latencyMs Request processing latency in milliseconds
 */
data class GetCandidatesResponse(
    val candidates: List<Candidate>,
    val metadata: ResponseMetadata,
    val latencyMs: Long
)

/**
 * Request for retrieving candidates for multiple customers.
 * 
 * @property customerIds List of customer identifiers
 * @property channel Optional channel filter
 * @property program Optional program filter
 * @property marketplace Marketplace identifier
 * @property limit Maximum number of candidates per customer
 * @property includeScores Whether to include score details
 */
data class BatchGetCandidatesRequest(
    val customerIds: List<String>,
    val channel: String? = null,
    val program: String? = null,
    val marketplace: String,
    val limit: Int = 10,
    val includeScores: Boolean = false
)

/**
 * Response containing candidates for multiple customers.
 * 
 * @property results Map of customerId to their candidates
 * @property metadata Response metadata
 * @property latencyMs Request processing latency in milliseconds
 */
data class BatchGetCandidatesResponse(
    val results: Map<String, List<Candidate>>,
    val metadata: ResponseMetadata,
    val latencyMs: Long
)

/**
 * Metadata about the serving response.
 * 
 * @property totalCount Total number of candidates found
 * @property filteredCount Number of candidates after filtering
 * @property cacheHit Whether the response was served from cache
 * @property eligibilityRefreshed Whether eligibility was refreshed
 * @property degraded Whether the response is degraded due to failures
 */
data class ResponseMetadata(
    val totalCount: Int,
    val filteredCount: Int,
    val cacheHit: Boolean = false,
    val eligibilityRefreshed: Boolean = false,
    val degraded: Boolean = false
)

/**
 * Exception thrown when serving operations fail.
 */
class ServingException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
