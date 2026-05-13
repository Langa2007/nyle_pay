package com.nyle.nylepay.services.chain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Centralised configuration for all supported EVM chains.
 *
 * Supported chains (all share the same secp256k1 key pair / address format):
 *   ETHEREUM  — most battle-tested L1
 *   POLYGON   — EVM L2, low gas, widely used
 *   ARBITRUM  — Ethereum L2, Nitro stack, very high security
 *   BASE      — Coinbase L2 (OP Stack), institutional-grade security
 *
 * Token contracts are on Ethereum mainnet by default; bridge addresses
 * for L2 chains are configured separately per chain.
 *
 * RPC URLs are injected from application.properties so they can point to
 * Alchemy / Infura / public RPCs without code changes.
 */
@Component
public class ChainConfig {


    @Value("${crypto.rpc.ethereum:https://eth-mainnet.g.alchemy.com/v2/demo}")
    private String ethereumRpc;

    @Value("${crypto.rpc.polygon:https://polygon-mainnet.g.alchemy.com/v2/demo}")
    private String polygonRpc;

    @Value("${crypto.rpc.arbitrum:https://arb-mainnet.g.alchemy.com/v2/demo}")
    private String arbitrumRpc;

    @Value("${crypto.rpc.base:https://mainnet.base.org}")
    private String baseRpc;


    /** USDT – Tether: most liquid stablecoin, audit history since 2014 */
    public static final String USDT_CONTRACT = "0xdAC17F958D2ee523a2206206994597C13D831ec7";

    /** USDC – Circle: regulatory-grade stablecoin, monthly audited reserves */
    public static final String USDC_CONTRACT = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48";

    /** DAI – MakerDAO: decentralised over-collateralised stablecoin, formally verified */
    public static final String DAI_CONTRACT  = "0x6B175474E89094C44Da98b954EedeAC495271d0F";

    /** Chain IDs used when signing raw transactions. */
    public static final Map<String, Long> CHAIN_IDS = Map.of(
        "ETHEREUM", 1L,
        "POLYGON",  137L,
        "ARBITRUM", 42161L,
        "BASE",     8453L
    );

    /** ERC-20 standard transfer function signature (first 4 bytes of keccak256). */
    public static final String ERC20_TRANSFER_SIG = "0xa9059cbb";

    /** Supported chain names */
    public static final Set<String> SUPPORTED_CHAINS = Set.of("ETHEREUM", "POLYGON", "ARBITRUM", "BASE");

    /** Supported ERC-20 token symbols */
    public static final Set<String> SUPPORTED_TOKENS = Set.of("ETH", "USDT", "USDC", "DAI", "MATIC", "BNB");

    public String getRpcUrl(String chain) {
        return switch (chain.toUpperCase()) {
            case "ETHEREUM" -> ethereumRpc;
            case "POLYGON"  -> polygonRpc;
            case "ARBITRUM" -> arbitrumRpc;
            case "BASE"     -> baseRpc;
            default -> throw new IllegalArgumentException("Unsupported chain: " + chain);
        };
    }

    public long getChainId(String chain) {
        Long id = CHAIN_IDS.get(chain.toUpperCase());
        if (id == null) throw new IllegalArgumentException("No chain ID for: " + chain);
        return id;
    }

    /**
     * Returns the ERC-20 contract address for a given token symbol,
     * or null if the asset is the chain's native token (ETH, MATIC, etc.).
     */
    public String getContractAddress(String symbol) {
        return switch (symbol.toUpperCase()) {
            case "USDT" -> USDT_CONTRACT;
            case "USDC" -> USDC_CONTRACT;
            case "DAI"  -> DAI_CONTRACT;
            default     -> null; // native coin
        };
    }
}
