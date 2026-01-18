package com.solicitation.serving

import com.solicitation.model.Candidate
import com.solicitation.storage.CandidateRepository
import com.solicitation.storage.StorageException
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Implementation of ServingAPI with low-latency candidate retrieval.
 * 
 * Provides single and batch customer queries with channel-specific filtering,
 * ranking, and optional eligibility refresh.
 * 
 * @property candidateRepository Repository for candidate storage
 * @property rankingStrategy Strategy for ranking candidates
 * @property eligibilityChecker Optional checker for real-time eligibility refresh
 * @property fallbackHandler Handler for graceful degradation
 */
class ServingAPIImpl(
    private val candidateRepository: CandidateRepository,
    private val rankingStrategy: RankingStrategy,
    private val eligibilityChecker: EligibilityChecker? = null,
    private val fallbackHandler: FallbackHandler? = null
) : ServingAPI {
    
    override fun getCandidatesForCustomer(request: GetCandidatesRequest): GetCandidatesResponse {
        val startTime = System.currentTimeMillis()
        
        try {
            logger.debug { "Getting candidates for customer: ${request.customerId}" }
            
            // Validate request
            validateRequest(request)
            
            // Query candidates from storage
            val candidates = queryCandidates(request)
            
            // Apply channel filtering if specified
            val filteredCandidates = if (request.channel != null) {
                filterByChannel(candidates, request.channel)
            } else {
                candidates
            }
            
            // Refresh eligibility if requested
            val eligibleCandidates = if (request.refreshEligibility && eligibilityChecker != null) {
                refreshEligibility(filteredCandidates)
            } else {
                filteredCandidates
            }
            
            // Apply ranking
            val rankedCandidates = rankingStrategy.rank(
                eligibleCandidates,
                request.channel,
                request.customerId
            )
            
            // Apply limit
            val limitedCandidates = rankedCandidates.take(request.limit)
            
            // Remove scores if not requested
            val finalCandidates = if (!request.includeScores) {
                limitedCandidates.map { it.copy(scores = null) }
            } else {
                limitedCandidates
            }
            
            val latency = System.currentTimeMillis() - startTime
            
            logger.info { "Retrieved ${finalCandidates.size} candidates for customer ${request.customerId} in ${latency}ms" }
            
            return GetCandidatesResponse(
                candidates = finalCandidates,
                metadata = ResponseMetadata(
                    totalCount = candidates.size,
                    filteredCount = finalCandidates.size,
                    eligibilityRefreshed = request.refreshEligibility && eligibilityChecker != null
                ),
                latencyMs = latency
            )
            
        } catch (e: Exception) {
            logger.error(e) { "Failed to get candidates for customer ${request.customerId}" }
            
            // Try fallback if available
            if (fallbackHandler != null) {
                return fallbackHandler.handleFailure(request, e, System.currentTimeMillis() - startTime)
            }
            
            throw ServingException("Failed to get candidates", e)
        }
    }
    
    override fun getCandidatesForCustomers(request: BatchGetCandidatesRequest): BatchGetCandidatesResponse {
        val startTime = System.currentTimeMillis()
        
        try {
            logger.debug { "Getting candidates for ${request.customerIds.size} customers" }
            
            // Validate request
            validateBatchRequest(request)
            
            // Query candidates for each customer
            val results = mutableMapOf<String, List<Candidate>>()
            
            for (customerId in request.customerIds) {
                try {
                    val customerRequest = GetCandidatesRequest(
                        customerId = customerId,
                        channel = request.channel,
                        program = request.program,
                        marketplace = request.marketplace,
                        limit = request.limit,
                        includeScores = request.includeScores,
                        refreshEligibility = false // Don't refresh in batch mode
                    )
                    
                    val response = getCandidatesForCustomer(customerRequest)
                    results[customerId] = response.candidates
                    
                } catch (e: Exception) {
                    logger.warn(e) { "Failed to get candidates for customer $customerId in batch" }
                    results[customerId] = emptyList()
                }
            }
            
            val latency = System.currentTimeMillis() - startTime
            
            logger.info { "Retrieved candidates for ${results.size} customers in ${latency}ms" }
            
            return BatchGetCandidatesResponse(
                results = results,
                metadata = ResponseMetadata(
                    totalCount = results.values.sumOf { it.size },
                    filteredCount = results.values.sumOf { it.size }
                ),
                latencyMs = latency
            )
            
        } catch (e: Exception) {
            logger.error(e) { "Failed to get candidates for batch request" }
            throw ServingException("Failed to get candidates for batch", e)
        }
    }
    
    /**
     * Validates a single customer request.
     */
    private fun validateRequest(request: GetCandidatesRequest) {
        require(request.customerId.isNotBlank()) { "Customer ID must not be blank" }
        require(request.marketplace.isNotBlank()) { "Marketplace must not be blank" }
        require(request.limit > 0) { "Limit must be positive" }
        require(request.limit <= 100) { "Limit must not exceed 100" }
    }
    
    /**
     * Validates a batch request.
     */
    private fun validateBatchRequest(request: BatchGetCandidatesRequest) {
        require(request.customerIds.isNotEmpty()) { "Customer IDs must not be empty" }
        require(request.customerIds.size <= 100) { "Batch size must not exceed 100" }
        require(request.marketplace.isNotBlank()) { "Marketplace must not be blank" }
        require(request.limit > 0) { "Limit must be positive" }
    }
    
    /**
     * Queries candidates from storage based on request parameters.
     */
    private fun queryCandidates(request: GetCandidatesRequest): List<Candidate> {
        return try {
            if (request.program != null && request.channel != null) {
                // Use GSI for program+channel query
                candidateRepository.queryByProgramAndChannel(
                    programId = request.program,
                    channelId = request.channel,
                    limit = request.limit * 2 // Query more for filtering
                )
            } else {
                // For now, return empty list if we can't query efficiently
                // In production, this would use a different query strategy
                emptyList()
            }
        } catch (e: StorageException) {
            logger.error(e) { "Storage query failed" }
            throw e
        }
    }
    
    /**
     * Filters candidates by channel eligibility.
     */
    private fun filterByChannel(candidates: List<Candidate>, channel: String): List<Candidate> {
        return candidates.filter { candidate ->
            candidate.attributes.channelEligibility[channel] == true
        }
    }
    
    /**
     * Refreshes eligibility for candidates.
     */
    private fun refreshEligibility(candidates: List<Candidate>): List<Candidate> {
        return candidates.filter { candidate ->
            eligibilityChecker?.isEligible(candidate) ?: true
        }
    }
}
