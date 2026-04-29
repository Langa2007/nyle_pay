package com.nyle.nylepay.config;

import com.nyle.nylepay.models.Merchant;
import com.nyle.nylepay.repositories.MerchantRepository;
import com.nyle.nylepay.utils.EncryptionUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class MerchantAuthFilter extends OncePerRequestFilter {

    private final MerchantRepository merchantRepository;
    private final EncryptionUtils encryptionUtils;

    public MerchantAuthFilter(MerchantRepository merchantRepository, EncryptionUtils encryptionUtils) {
        this.merchantRepository = merchantRepository;
        this.encryptionUtils = encryptionUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only intercept Headless Merchant API calls
        if (!path.startsWith("/api/v1/merchant")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer npy_sec_")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Missing or invalid Authorization header. Expected Bearer npy_sec_...\"}");
            return;
        }

        String rawSecretKey = authHeader.substring(7); // Remove "Bearer "

        // TODO: In production with thousands of merchants, use Redis to cache rawSecretKey -> MerchantId
        // or store a deterministic SHA-256 hash of the secret key in the database for O(1) lookups.
        List<Merchant> merchants = merchantRepository.findAll();
        Optional<Merchant> authenticatedMerchant = merchants.stream()
                .filter(m -> "ACTIVE".equals(m.getStatus()))
                .filter(m -> rawSecretKey.equals(encryptionUtils.decrypt(m.getEncryptedSecretKey())))
                .findFirst();

        if (authenticatedMerchant.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Invalid or inactive Merchant API Key.\"}");
            return;
        }

        Merchant merchant = authenticatedMerchant.get();

        // Create authentication token and set in context
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                merchant,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_MERCHANT_API"))
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
