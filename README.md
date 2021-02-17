# solt
## Solidity Tool
![Build](https://github.com/hjubb/solt/workflows/Build/badge.svg?branch=main) ![GitHub tag (latest SemVer pre-release)](https://img.shields.io/github/v/tag/hjubb/solt?include_prereleases)

CLI tool to help you do two things:

1. Generate (`write`) a `solc --standard-json` compatible file **deterministically**
2. Automatically verify (`verify`) a deployed contract on [etherscan](https://etherscan.io).

## Install

### Linux
```bash
wget https://github.com/hjubb/solt/releases/download/v0.5.2/solt-linux-x64 -O ~/.local/bin/solt
chmod +x ~/.local/bin/solt
```

### Mac
```bash
wget https://github.com/hjubb/solt/releases/download/v0.5.2/solt-mac -O ~/.local/bin/solt
chmod +x ~/.local/bin/solt
```

## API

### Write

Creates a `standard-json` compatible file.

`solt write [directory or solidity file]`

Optional flags
* `--no-optimization` `-no-opt` Flag for whether to exclude optimization in the output
* `--runs` `-r` [number] Number of optimization runs, only used when optimizing, by default 200
* `--npm` Flag for whether this project uses npm style imports (node_modules)
* `--output` [string] Output file name (defaults to solc-input-[file].json)
* `--help` `-h` Usage info

Note that you are expected to run this from the root of your Solidity project. That is, from the same directory where "node_modules" can be found. Otherwise, certain features like the `--npm` flag will not work.

### Verify

Verifies contract on [etherscan](https://etherscan.io)

`solt verify [output.json] [contract address] [contract name in output-file] [--compiler solc-version]`

Required flags:
* `--compiler` `-c` [string] the compiler version used, e.g. 'v0.6.12'

Optional flags:
* `--license` `-l` [string] License according to etherscan, valid codes 1-12 where 1=No License .. 12=Apache 2.0, see https://etherscan.io/contract-license-types
* `--network` `-n` [string] The network name [rinkeby, kovan etc], defaults to mainnet
* `--infura` [string] Optional infura API key (if the shared one is rate limited)
* `--etherscan` [string] Optional etherscan API Key (if the shared one is rate limited)
* `--help` `-h` Usage info

### Usage example

1. Generate `standard-json` input format

```bash
git clone https://github.com/MainframeHQ/mainframe-lending-protocol.git /tmp/example
cd /tmp/example
yarn install # installs node dependencies

solt write contracts --npm

# solc-input-contracts.json is generated
```

2. Verify on etherscan

```bash
solt verify solc-input-contracts.json <deployed address> <contract name>

# You can obtain your contract name using the following command
# $ cat solc-input-contracts.json | jq .sources | jq 'keys'
```

## Why does this exist?

Flattening your solidity code before verifying them on etherscan [is not recommended](https://twitter.com/ethchris/status/1296121526601875456). Yet it seems to be the most common approach for verification on etherscan.

While this is fine for small projects, you run into weird edge cases where flattening your code will [yield different bytecodes from the deployed ones](https://github.com/UMAprotocol/protocol/issues/1807) as the complexity of your code grows. This could be due to the usage of experimental features such as `ABIEncoderV2` in your source code, and/or the usage of abosolute file paths when compiling your contracts (see [here](https://github.com/kendricktan/etherscan-verification-horrors)).

One approach that has worked consistently was using the [compiler standard-json](https://solidity.readthedocs.io/en/v0.6.12/using-the-compiler.html#compiler-input-and-output-json-description) input method, which is also [used by buidler](https://github.com/nomiclabs/buidler/pull/416) behind the scenes.

`solt` was built as we wanted a static-binary that could:

1. Generate a `standard-json` file for `solc`
2. Be configured without any `.config.js` file
3. Programmatically obtain the abi encoded constructor args to ease the etherscan verification process

## Sponsor

Did `solt` save you engineering time? Any donations will be greatly appreciated!

[:heart: Sponsor](https://github.com/sponsors/hjubb)
