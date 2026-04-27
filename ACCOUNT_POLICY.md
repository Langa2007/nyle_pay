# NylePay Account & Privacy Policy

This document outlines how NylePay manages user account identifiers, security, and data lifecycle in compliance with Kenyan laws (Data Protection Act, 2019).

## 1. Account Creation & Identifiers

### The NylePay Account Number
Each user is assigned a unique **NylePay Account Number** upon successful registration and KYC verification.

- **Format**: Alphanumeric
- **Structure**: `NPY-` followed by 6-9 random alphanumeric characters (e.g., `NPY-A7B9X2`).
- **Generation**: The suffix is generated using a cryptographically secure random generator to ensure unpredictability.
- **Purpose**: This number acts as the primary identifier for P2P transfers within the NylePay ecosystem, similar to a bank account number.

### Privacy of Identifiers
- **Masking**: In the UI and public receipts, the account number is partially masked (e.g., `NPY-A7****`).
- **Internal Only**: The full account number is never shared with third parties (merchants, aggregators) unless explicitly authorized by the user for a specific settlement.
- **Phone Number Privacy**: Per Kenyan regulations, we do not expose a user's M-Pesa or mobile number to other users. Internal P2P transfers are conducted via the `NylePay Account Number` or a verified `Alias/Username`.

## 2. No-Recycling Policy (Kenyan Law Compliance)

In accordance with the **Kenyan Data Protection Act (2019)** and CBK guidelines, NylePay enforces a strict **Zero-Recycling Policy**.

> **The Rule**: Once a NylePay Account Number is issued to a person, it is permanently linked to that individual's identity (ID/Passport) in our archives.

- **Inactive Accounts**: If a user stops using NylePay, the account is marked as `INACTIVE`.
- **Closure**: If a user requests account deletion, the number is `RETIRED`. It will **NEVER** be reassigned to a new user, even decades later.
- **Rationale**: Recycling account numbers poses a massive security risk (impersonation, misdirected funds) and violates the user's right to privacy regarding their historical financial footprint.

## 3. Account Inheritance & Succession (Case of Death)

NylePay provides a secure framework for the transfer of funds in the event of a user's demise, following Kenyan succession laws.

### Next of Kin (NoK)
Users are encouraged to nominate a **Next of Kin** during the KYC process.

### The Claim Process
1. **Notification**: A legal representative or NoK must notify NylePay of the user's death with a certified **Death Certificate**.
2. **Verification**: NylePay verifies the document with the relevant authorities.
3. **Succession Documents**: In line with the *Law of Succession Act (Cap 160)*, the claimant must provide:
   - Letters of Administration (if intestate) OR
   - Grant of Probate (if there was a will).
4. **Fund Transfer**: Once verified, the funds are moved to the claimant's verified bank account or NylePay wallet.
5. **Account Retirement**: After the balance is cleared, the original account is permanently retired (not recycled).

## 4. Security Measures
- **Encryption**: All PII (Personally Identifiable Information) is encrypted at rest using AES-256.
- **Access Control**: Only authorized compliance officers can view full user details, and every access is logged in an immutable **Audit Trail**.
- **Data Residency**: All user data is stored in compliance with local data residency requirements where applicable.
