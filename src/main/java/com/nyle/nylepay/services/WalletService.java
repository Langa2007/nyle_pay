package com.nyle.nylepay.services;

import com.nyle.nylepay.models.User;
import com.nyle.nylepay.models.Wallet;
import com.nyle.nylepay.exceptions.InsufficientBalanceException;
import com.nyle.nylepay.exceptions.ResourceNotFoundException;
import com.nyle.nylepay.repositories.UserRepository;
import com.nyle.nylepay.repositories.WalletRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

@Service
public class WalletService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final Web3j web3j;

    public WalletService(UserRepository userRepository, WalletRepository walletRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.web3j = Web3j.build(
                new HttpService("https://mainnet.infura.io/v3/YOUR_INFURA_KEY"));
    }

    // ------------------------------------------------------------------------
    // CREATE WALLET
    // ------------------------------------------------------------------------
    public Map<String, Object> createCryptoWallet(Long userId) throws Exception {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SecureRandom random = new SecureRandom();
        ECKeyPair keyPair = Keys.createEcKeyPair(random);

        String privateKey = keyPair.getPrivateKey().toString(16);
        String publicKey = keyPair.getPublicKey().toString(16);
        String address = "0x" + Keys.getAddress(keyPair);

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(Wallet::new);

        wallet.setUserId(userId);

        // Fix: always provide currency code and amount
        wallet.getBalances().putIfAbsent("ETH", new Wallet.Balance(BigDecimal.ZERO));
        wallet.getBalances().putIfAbsent("KSH", new Wallet.Balance(BigDecimal.ZERO));
        wallet.getBalances().putIfAbsent("USD", new Wallet.Balance(BigDecimal.ZERO));

        walletRepository.save(wallet);

        user.setCryptoAddress(address);
        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("address", address);
        response.put("publicKey", publicKey);
        response.put("message", "Crypto wallet created successfully");
        response.put("privateKey", privateKey); // ⚠ Don't expose in prod

        return response;
    }

    // ------------------------------------------------------------------------
    // ADD BALANCE
    // ------------------------------------------------------------------------
    @Transactional
    public void addBalance(Long userId, String currency, BigDecimal amount) {

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        wallet.getBalances().merge(
                currency,
                new Wallet.Balance(amount),
                (existing, incoming) -> {
                    existing.setAmount(existing.getAmount().add(incoming.getAmount()));
                    return existing;
                });

        walletRepository.save(wallet);
    }

    // ------------------------------------------------------------------------
    // SUBTRACT BALANCE
    // ------------------------------------------------------------------------
    @Transactional
    public void subtractBalance(Long userId, String currency, BigDecimal amount) {

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        Wallet.Balance balance = wallet.getBalances().get(currency);

        if (balance == null || balance.getAmount().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(currency, amount.doubleValue(), balance == null ? 0.0 : balance.getAmount().doubleValue());
        }

        balance.setAmount(balance.getAmount().subtract(amount));
        walletRepository.save(wallet);
    }

    // ------------------------------------------------------------------------
    // GET ALL BALANCES
    // ------------------------------------------------------------------------
    public Map<String, BigDecimal> getBalances(Long userId) {

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        Map<String, BigDecimal> result = new HashMap<>();
        wallet.getBalances().forEach(
                (currency, balance) -> result.put(currency, balance.getAmount()));

        return result;
    }

    // ------------------------------------------------------------------------
    // GET SINGLE BALANCE
    // ------------------------------------------------------------------------
    public BigDecimal getBalance(Long userId, String currency) {

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        Wallet.Balance balance = wallet.getBalances().get(currency);
        return balance == null ? BigDecimal.ZERO : balance.getAmount();
    }

    // ------------------------------------------------------------------------
    // TRANSFER BETWEEN USERS
    // ------------------------------------------------------------------------
    @Transactional
    public Map<String, Object> transferBetweenUsers(
            Long fromUserId,
            Long toUserId,
            String currency,
            BigDecimal amount) {

        userRepository.findById(fromUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));
        userRepository.findById(toUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));

        subtractBalance(fromUserId, currency, amount);
        addBalance(toUserId, currency, amount);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Transfer successful");
        response.put("senderBalance", getBalance(fromUserId, currency));
        response.put("receiverBalance", getBalance(toUserId, currency));

        return response;
    }

    // ------------------------------------------------------------------------
    // CONVERT CURRENCY
    // ------------------------------------------------------------------------
    @Transactional
    public Map<String, Object> convertCurrency(
            Long userId,
            String fromCurrency,
            String toCurrency,
            BigDecimal amount) {

        BigDecimal rate;

        if (fromCurrency.equals("ETH") && toCurrency.equals("USD")) {
            rate = BigDecimal.valueOf(3000);
        } else if (fromCurrency.equals("USD") && toCurrency.equals("ETH")) {
            rate = BigDecimal.ONE.divide(
                    BigDecimal.valueOf(3000),
                    8,
                    RoundingMode.HALF_UP);
        } else if (fromCurrency.equals("KSH") && toCurrency.equals("USD")) {
            rate = BigDecimal.valueOf(0.0067);
        } else if (fromCurrency.equals("USD") && toCurrency.equals("KSH")) {
            rate = BigDecimal.valueOf(150);
        } else {
            rate = BigDecimal.ONE;
        }

        BigDecimal converted = amount.multiply(rate);
        BigDecimal fee = converted.multiply(BigDecimal.valueOf(0.01));
        BigDecimal finalAmount = converted.subtract(fee);

        subtractBalance(userId, fromCurrency, amount);
        addBalance(userId, toCurrency, finalAmount);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("fromCurrency", fromCurrency);
        response.put("toCurrency", toCurrency);
        response.put("amount", amount);
        response.put("rate", rate);
        response.put("finalAmount", finalAmount);

        return response;
    }
}
