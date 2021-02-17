package com.github.hjubb.solt

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.http.ContentType.*
import io.ktor.utils.io.core.*
import kotlinx.cli.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import pw.binom.io.file.File
import pw.binom.io.file.isExist
import pw.binom.io.file.read
import pw.binom.io.readText
import pw.binom.io.use
import pw.binom.io.utf8Reader

@ExperimentalCli
class Verifier : Subcommand("verify", "Verify the contracts via Etherscan's HTTP API") {

    companion object {
        const val SOLC_VERSION_URL = "https://raw.githubusercontent.com/ethereum/solc-bin/gh-pages/bin/list.txt"

        fun infuraUrl(network: String, projectId: String?) =
            "https://$network.infura.io/v3/${projectId ?: BuildKonfig.infura}"

        fun etherscanUrl(network: String) =
            "https://api${if (network != "mainnet") "-$network" else ""}.etherscan.io/api"


    }

    private val fileName by argument(
        ArgType.String,
        description = "standard input json file to upload for verification"
    )

    private val contractAddress by argument(
        ArgType.String,
        fullName = "address",
        description = "the deployed contract address '0x...'"
    )

    private val contractName by argument(
        ArgType.String,
        fullName = "name",
        description = "the contract's name as it appears in standard json input, with an optional name mapping [name:mappedname]"
    )

    private val compilerVersion by option(
        ArgType.String,
        fullName = "compiler",
        shortName = "c",
        description = "the compiler version used, e.g. 'v0.6.12'"
    ).required()

    private val licenseType by option(
        ArgType.Int,
        fullName = "license",
        shortName = "l",
        description = "license according to etherscan, valid codes 1-12 where 1=No License .. 12=Apache 2.0, see https://etherscan.io/contract-license-types"
    )

    private val network by option(
        ArgType.String,
        shortName = "n",
        description = "the network name [rinkeby, kovan etc], or none for mainnet"
    ).default("mainnet")

    private val infuraApi by option(
        ArgType.String,
        fullName = "infura",
        description = "optional infura API key (if the shared one is rate limited)"
    )

    private val etherscanApi by option(
        ArgType.String,
        fullName = "etherscan",
        description = "optional etherscan API Key (if the shared one is rate limited)"
    )

    override fun execute() {
        Executor().execute(this::verify)
    }

    private suspend fun verify() {

        HttpClient {
            install(JsonFeature) {
                serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                })
            }
        }.use { client ->

            val file = File(fileName)

            if (!file.isExist) {
                throw Exception("please provide a valid path to a standard json input")
            }

            if (!contractAddress.startsWith("0x")) {
                throw Exception("please provide a valid address")
            }

            if (licenseType != null && licenseType !in 0..12) {
                throw Exception("supported license types are 0 .. 12")
            }

            val (searchName, rename) = contractName.split(':').extractNames()

            val input = file.read().use {
                it.utf8Reader().readText()
            }

            val inputJson = Json.decodeFromString(BigSolInput.serializer(), input)

            val (foundContract, foundName) = inputJson.sources.entries.mapNotNull {
                when {
                    it.key.contains(searchName, true) -> {
                        it to it.key.getSolidityName()
                    }
                    it.value.findContractName()?.contains(searchName, true) == true -> {
                        it to it.value.findContractName()!!
                    }
                    else -> {
                        null
                    }
                }
            }.firstOrNull() ?: throw Exception("couldn't find $searchName in the json content")

            val remappingName = if (rename.isNotBlank()) rename else foundName
            println("verifying ${foundContract.key} with name \"$remappingName\"")

            val contractMapping = foundContract.let {
                "${it.key}:$remappingName"
            }

            // get available solc versions and try to match in etherscan format
            val versions = client.get<String>(SOLC_VERSION_URL)
            val selectedVersion = versions.lineSequence().firstOrNull {
                it.contains(compilerVersion)
            }?.substringAfter("soljson-")?.substringBeforeLast(".js")
                ?: throw Exception("please provide a valid solc version")

            println("using solc version: $selectedVersion")

            // get infura info for the code
            val code = client.post<WrappedString>(infuraUrl(network, infuraApi)) {
                contentType(Application.Json)
                body = InfuraRequest(
                    jsonrpc = "2.0",
                    method = "eth_getCode",
                    params = listOf(
                        contractAddress, // address
                        "latest" // block
                    ),
                    id = 1
                )
            }

            // get the transaction list for address
            val txList = client.get<EtherscanTxListResult> {
                url(etherscanUrl(network))
                parameter("module", "account")
                parameter("action", "txlist")
                parameter("address", contractAddress)
                parameter("sort", "asc")
                parameter("apikey", etherscanApi ?: BuildKonfig.ether)
            }
            val tx = txList.result.first()

            val constructor = tx.input.substringAfter(code.result.substringAfter("0x"))

            println("constructor = $constructor")

            val guid = client.submitForm<EtherscanVerificationResult>(
                etherscanUrl(network),
                parametersOf(
                    "apikey" to listOf(etherscanApi ?: BuildKonfig.ether),
                    "module" to listOf("contract"),
                    "action" to listOf("verifysourcecode"),
                    "contractaddress" to listOf(contractAddress),
                    "sourceCode" to listOf(input),
                    "codeformat" to listOf("solidity-standard-json-input"),
                    "licenseType" to (licenseType?.let { listOf(it.toString()) } ?: listOf()),
                    "compilerversion" to listOf(selectedVersion),
                    "constructorArguements" to listOf(constructor),
                    "contractname" to listOf(contractMapping)
                )
            )

            if (guid.status == "0") { // error
                println(guid.result)
                return
            }

            println("waiting for verification result...")

            lateinit var verifyResult: EtherscanGuidResult
            var retries = 0
            do {
                delay(2000)
                verifyResult = client.submitForm(
                    etherscanUrl(network),
                    parametersOf(
                        "apikey" to listOf(etherscanApi ?: BuildKonfig.ether),
                        "guid" to listOf(guid.result),
                        "module" to listOf("contract"),
                        "action" to listOf("checkverifystatus")
                    ),
                    true
                )
            } while (verifyResult.result.toLowerCase().contains("pending") && retries++ <= 10)

            println(verifyResult.result)

        }
    }

    fun List<String>.extractNames(): List<String> {
        return if (size < 2) listOf(first(),"")
        else take(2)
    }

}

