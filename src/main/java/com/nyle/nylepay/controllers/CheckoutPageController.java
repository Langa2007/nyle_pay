package com.nyle.nylepay.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Serves the hosted checkout HTML page for any payment reference.
 *
 * GET /checkout/{reference}  → serves static/checkout/index.html
 *                              with ?ref={reference} appended so checkout.js can read it.
 *
 * The URL that MerchantService generates is:
 *   https://pay.nylepay.com/checkout/{reference}
 * or in development:
 *   https://nyle-pay.onrender.com/checkout/{reference}
 *
 * Spring Boot serves static files from /static automatically.
 * This controller only exists to forward the path-variable URL to the static page.
 */
@Controller
public class CheckoutPageController {

    @GetMapping("/checkout/{reference}")
    public String checkoutPage(@PathVariable String reference,
                                jakarta.servlet.http.HttpServletResponse response)
            throws java.io.IOException {
        // Redirect to the static HTML page, passing the reference as a query param
        // so checkout.js can pick it up via URLSearchParams.
        response.sendRedirect("/checkout/index.html?ref=" + reference);
        return null;
    }
}
