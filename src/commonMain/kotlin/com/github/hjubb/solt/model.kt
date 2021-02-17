package com.github.hjubb.solt

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import pw.binom.io.file.File

@Serializable
data class Content(val content: String) {

    companion object {
        val contractRegex =
            Regex("contract (\\w+)")
    }

    fun findContractName() : String? {
        return content.lineSequence()
            .mapNotNull {
                contractRegex.find(it)
            }
            .map { result ->
                result.groups[1]!!.value
            }.firstOrNull()
    }
}

@Serializable
data class BigSolInput(
    val language: String,
    val sources: Map<String, Content>,
    val settings: JsonObject
)

fun String.getSolidityName() = this.substringAfterLast(File.SEPARATOR).substringBefore(".sol")

@Serializable
data class EtherscanGuidResult(
    val result: String
)

@Serializable
data class InfuraRequest(
    val jsonrpc: String,
    val method: String,
    val params: List<String>,
    val id: Int
)

@Serializable
data class WrappedString(val result: String)

@Serializable
data class EtherscanTxListResult(
    val result: List<EtherscanTx>
)

@Serializable
data class EtherscanTx(
    val blockNumber: String,
    val hash: String,
    val input: String
)

@Serializable
data class EtherscanVerificationResult(
    val status: String,
    val result: String,
    val message: String
)