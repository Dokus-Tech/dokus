# Peppol E-Invoicing Integration

**Last Updated:** October 2025
**Compliance Deadline:** January 1, 2026 (Belgium)
**Status:** Integration Ready

---

## Table of Contents

1. [Overview](#overview)
2. [Belgium 2026 Mandate](#belgium-2026-mandate)
3. [Integration Approach](#integration-approach)
4. [UBL 2.1 Format](#ubl-21-format)
5. [Implementation Guide](#implementation-guide)
6. [Testing](#testing)
7. [Compliance Checklist](#compliance-checklist)

---

## Overview

**Peppol** (Pan-European Public Procurement Online) is an international e-invoicing network enabling businesses to send and receive electronic invoices across borders. Belgium mandates Peppol for all B2B transactions starting January 1, 2026.

### What is Peppol?

- **E-Invoicing Network**: Standardized electronic invoice exchange
- **International Standard**: Used across Europe and globally
- **Mandatory in Belgium**: Required for all B2B invoices from 2026
- **UBL 2.1 Format**: Universal Business Language XML standard
- **Access Points**: Gateway providers that handle transmission

### Key Benefits

✅ **Compliance**: Meets Belgian legal requirements
✅ **Faster Payment**: Invoices delivered instantly
✅ **Reduced Costs**: No printing, postage, or manual entry
✅ **Proof of Delivery**: Guaranteed delivery confirmation
✅ **Automated Processing**: Machine-readable invoices

---

## Belgium 2026 Mandate

### Legal Requirements

**Effective Date:** January 1, 2026

**Who Must Comply:**
- All businesses issuing B2B invoices in Belgium
- Freelancers and sole proprietors
- Companies of all sizes

**Penalties for Non-Compliance:**
- **First Offense**: €1,500 fine
- **Subsequent Offenses**: €5,000 fine per violation

### Transition Timeline

```
2025 Q1-Q3: Preparation Phase
├─ Choose Access Point provider
├─ Implement UBL XML generation
└─ Test with pilot customers

2025 Q4: Testing Phase
├─ Full integration testing
├─ Client onboarding
└─ Backup procedures

2026 Jan 1: Mandatory Compliance
└─ All B2B invoices via Peppol
```

---

## Integration Approach

### Recommended: Partner with Access Point Provider

Dokus partners with **Pagero** or **EDICOM** for Peppol transmission.

**Why Partner (vs Self-Host)?**

| Approach | Cost | Time | Complexity | Recommendation |
|----------|------|------|------------|----------------|
| **Partner** | €2K setup + €0.10-0.30/invoice | 2-4 weeks | Low | ✅ **Recommended** |
| **Self-Host** | €50K+ development | 6-12 months | Very High | ❌ Not recommended |

**Partner Benefits:**
- ✅ Quick setup (2-4 weeks vs 6-12 months)
- ✅ Guaranteed compliance
- ✅ No infrastructure maintenance
- ✅ Automatic updates for new regulations
- ✅ Technical support
- ✅ Proven reliability

### Integration Architecture

```
┌──────────────────────────────────────────────────┐
│              DOKUS APPLICATION                    │
├──────────────────────────────────────────────────┤
│                                                   │
│  1. User creates invoice                         │
│  2. Validate invoice data                        │
│  3. Generate UBL 2.1 XML                         │
│  4. Send to Access Point API                     │
│                                                   │
└────────────────┬─────────────────────────────────┘
                 │
                 │ HTTPS POST
                 │
┌────────────────▼─────────────────────────────────┐
│         ACCESS POINT (Pagero/EDICOM)             │
├──────────────────────────────────────────────────┤
│                                                   │
│  5. Validate UBL against Peppol BIS 3.0          │
│  6. Transmit to recipient's Access Point         │
│  7. Return delivery confirmation                 │
│                                                   │
└────────────────┬─────────────────────────────────┘
                 │
                 │ Peppol Network
                 │
┌────────────────▼─────────────────────────────────┐
│    RECIPIENT'S ACCESS POINT                      │
├──────────────────────────────────────────────────┤
│                                                   │
│  8. Receive invoice                              │
│  9. Deliver to recipient's accounting system     │
│                                                   │
└──────────────────────────────────────────────────┘
```

---

## UBL 2.1 Format

### Sample UBL Invoice

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Invoice xmlns="urn:oasis:names:specification:ubl:schema:xsd:Invoice-2"
         xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"
         xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2">

  <!-- Invoice Header -->
  <cbc:CustomizationID>urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0</cbc:CustomizationID>
  <cbc:ProfileID>urn:fdc:peppol.eu:2017:poacc:billing:01:1.0</cbc:ProfileID>
  <cbc:ID>INV-2025-001</cbc:ID>
  <cbc:IssueDate>2025-10-20</cbc:IssueDate>
  <cbc:DueDate>2025-11-19</cbc:DueDate>
  <cbc:InvoiceTypeCode>380</cbc:InvoiceTypeCode>
  <cbc:DocumentCurrencyCode>EUR</cbc:DocumentCurrencyCode>

  <!-- Supplier (Seller) -->
  <cac:AccountingSupplierParty>
    <cac:Party>
      <cbc:EndpointID schemeID="BE:VAT">BE0123456789</cbc:EndpointID>
      <cac:PartyName>
        <cbc:Name>My Freelance Business</cbc:Name>
      </cac:PartyName>
      <cac:PostalAddress>
        <cbc:StreetName>Main Street 123</cbc:StreetName>
        <cbc:CityName>Brussels</cbc:CityName>
        <cbc:PostalZone>1000</cbc:PostalZone>
        <cac:Country>
          <cbc:IdentificationCode>BE</cbc:IdentificationCode>
        </cac:Country>
      </cac:PostalAddress>
      <cac:PartyTaxScheme>
        <cbc:CompanyID>BE0123456789</cbc:CompanyID>
        <cac:TaxScheme>
          <cbc:ID>VAT</cbc:ID>
        </cac:TaxScheme>
      </cac:PartyTaxScheme>
    </cac:Party>
  </cac:AccountingSupplierParty>

  <!-- Customer (Buyer) -->
  <cac:AccountingCustomerParty>
    <cac:Party>
      <cbc:EndpointID schemeID="BE:VAT">BE0987654321</cbc:EndpointID>
      <cac:PartyName>
        <cbc:Name>Client Company BVBA</cbc:Name>
      </cac:PartyName>
      <cac:PostalAddress>
        <cbc:StreetName>Business Avenue 456</cbc:StreetName>
        <cbc:CityName>Antwerp</cbc:CityName>
        <cbc:PostalZone>2000</cbc:PostalZone>
        <cac:Country>
          <cbc:IdentificationCode>BE</cbc:IdentificationCode>
        </cac:Country>
      </cac:PostalAddress>
      <cac:PartyTaxScheme>
        <cbc:CompanyID>BE0987654321</cbc:CompanyID>
        <cac:TaxScheme>
          <cbc:ID>VAT</cbc:ID>
        </cac:TaxScheme>
      </cac:PartyTaxScheme>
    </cac:Party>
  </cac:AccountingCustomerParty>

  <!-- Payment Terms -->
  <cac:PaymentMeans>
    <cbc:PaymentMeansCode>30</cbc:PaymentMeansCode>
    <cac:PayeeFinancialAccount>
      <cbc:ID>BE68539007547034</cbc:ID>
    </cac:PayeeFinancialAccount>
  </cac:PaymentMeans>

  <!-- Tax Total -->
  <cac:TaxTotal>
    <cbc:TaxAmount currencyID="EUR">630.00</cbc:TaxAmount>
    <cac:TaxSubtotal>
      <cbc:TaxableAmount currencyID="EUR">3000.00</cbc:TaxableAmount>
      <cbc:TaxAmount currencyID="EUR">630.00</cbc:TaxAmount>
      <cac:TaxCategory>
        <cbc:ID>S</cbc:ID>
        <cbc:Percent>21.00</cbc:Percent>
        <cac:TaxScheme>
          <cbc:ID>VAT</cbc:ID>
        </cac:TaxScheme>
      </cac:TaxCategory>
    </cac:TaxSubtotal>
  </cac:TaxTotal>

  <!-- Monetary Totals -->
  <cac:LegalMonetaryTotal>
    <cbc:LineExtensionAmount currencyID="EUR">3000.00</cbc:LineExtensionAmount>
    <cbc:TaxExclusiveAmount currencyID="EUR">3000.00</cbc:TaxExclusiveAmount>
    <cbc:TaxInclusiveAmount currencyID="EUR">3630.00</cbc:TaxInclusiveAmount>
    <cbc:PayableAmount currencyID="EUR">3630.00</cbc:PayableAmount>
  </cac:LegalMonetaryTotal>

  <!-- Invoice Line Item -->
  <cac:InvoiceLine>
    <cbc:ID>1</cbc:ID>
    <cbc:InvoicedQuantity unitCode="HUR">40</cbc:InvoicedQuantity>
    <cbc:LineExtensionAmount currencyID="EUR">3000.00</cbc:LineExtensionAmount>
    <cac:Item>
      <cbc:Description>Software development services</cbc:Description>
      <cbc:Name>Development Hours</cbc:Name>
      <cac:ClassifiedTaxCategory>
        <cbc:ID>S</cbc:ID>
        <cbc:Percent>21.00</cbc:Percent>
        <cac:TaxScheme>
          <cbc:ID>VAT</cbc:ID>
        </cac:TaxScheme>
      </cac:ClassifiedTaxCategory>
    </cac:Item>
    <cac:Price>
      <cbc:PriceAmount currencyID="EUR">75.00</cbc:PriceAmount>
    </cac:Price>
  </cac:InvoiceLine>

</Invoice>
```

---

## Implementation Guide

### Step 1: Generate UBL XML

```kotlin
class PeppolInvoiceGenerator {
    fun generateUBL(invoice: Invoice, client: Client, tenant: Tenant): String {
        return buildXml {
            element("Invoice", XMLNS_INVOICE) {
                // Customization
                element("CustomizationID") {
                    text("urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0")
                }

                // Profile
                element("ProfileID") {
                    text("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0")
                }

                // Invoice details
                element("ID") { text(invoice.invoiceNumber) }
                element("IssueDate") { text(invoice.issueDate.toString()) }
                element("DueDate") { text(invoice.dueDate.toString()) }
                element("InvoiceTypeCode") { text("380") }  // Commercial invoice
                element("DocumentCurrencyCode") { text(invoice.currency) }

                // Supplier
                element("AccountingSupplierParty", XMLNS_CAC) {
                    element("Party", XMLNS_CAC) {
                        element("EndpointID", XMLNS_CBC) {
                            attribute("schemeID", "BE:VAT")
                            text(tenant.vatNumber)
                        }
                        // Add more supplier details...
                    }
                }

                // Customer
                element("AccountingCustomerParty", XMLNS_CAC) {
                    element("Party", XMLNS_CAC) {
                        element("EndpointID", XMLNS_CBC) {
                            attribute("schemeID", "BE:VAT")
                            text(client.vatNumber)
                        }
                        // Add more customer details...
                    }
                }

                // Tax totals
                element("TaxTotal", XMLNS_CAC) {
                    element("TaxAmount", XMLNS_CBC) {
                        attribute("currencyID", invoice.currency)
                        text(invoice.vatAmount.toString())
                    }
                    // Add tax subtotals...
                }

                // Monetary totals
                element("LegalMonetaryTotal", XMLNS_CAC) {
                    element("LineExtensionAmount", XMLNS_CBC) {
                        attribute("currencyID", invoice.currency)
                        text(invoice.subtotalAmount.toString())
                    }
                    element("TaxExclusiveAmount", XMLNS_CBC) {
                        attribute("currencyID", invoice.currency)
                        text(invoice.subtotalAmount.toString())
                    }
                    element("TaxInclusiveAmount", XMLNS_CBC) {
                        attribute("currencyID", invoice.currency)
                        text(invoice.totalAmount.toString())
                    }
                    element("PayableAmount", XMLNS_CBC) {
                        attribute("currencyID", invoice.currency)
                        text(invoice.totalAmount.toString())
                    }
                }

                // Invoice lines
                invoice.items.forEachIndexed { index, item ->
                    element("InvoiceLine", XMLNS_CAC) {
                        element("ID", XMLNS_CBC) { text((index + 1).toString()) }
                        element("InvoicedQuantity", XMLNS_CBC) {
                            attribute("unitCode", getUnitCode(item))
                            text(item.quantity.toString())
                        }
                        element("LineExtensionAmount", XMLNS_CBC) {
                            attribute("currencyID", invoice.currency)
                            text(item.lineTotal.toString())
                        }
                        // Add item details...
                    }
                }
            }
        }
    }
}
```

### Step 2: Send to Access Point

```kotlin
class PeppolTransmissionService(
    private val accessPointApiUrl: String,
    private val apiKey: String
) {
    suspend fun sendInvoice(
        ublXml: String,
        senderPeppolId: String,
        recipientPeppolId: String
    ): PeppolTransmissionResult {
        val httpClient = HttpClient()

        return try {
            val response = httpClient.post(accessPointApiUrl) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $apiKey")

                setBody(PeppolSendRequest(
                    documentType = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2",
                    processId = "urn:fdc:peppol.eu:2017:poacc:billing:01:1.0",
                    senderParticipant = senderPeppolId,
                    recipientParticipant = recipientPeppolId,
                    documentContent = Base64.encode(ublXml.toByteArray())
                ))
            }

            if (response.status.isSuccess()) {
                val result = response.body<PeppolSendResponse>()
                PeppolTransmissionResult.Success(
                    transmissionId = result.transmissionId,
                    timestamp = result.timestamp
                )
            } else {
                PeppolTransmissionResult.Failure(
                    error = "Access Point returned ${response.status}"
                )
            }
        } catch (e: Exception) {
            PeppolTransmissionResult.Failure(
                error = e.message ?: "Unknown error"
            )
        }
    }
}

@Serializable
data class PeppolSendRequest(
    val documentType: String,
    val processId: String,
    val senderParticipant: String,
    val recipientParticipant: String,
    val documentContent: String  // Base64-encoded UBL XML
)

@Serializable
data class PeppolSendResponse(
    val transmissionId: String,
    val timestamp: Instant,
    val status: String
)

sealed class PeppolTransmissionResult {
    data class Success(
        val transmissionId: String,
        val timestamp: Instant
    ) : PeppolTransmissionResult()

    data class Failure(
        val error: String
    ) : PeppolTransmissionResult()
}
```

### Step 3: Update Invoice Status

```kotlin
suspend fun sendInvoiceViaPeppol(invoiceId: UUID): Result<Invoice> {
    return dbQuery {
        // Get invoice with items and client
        val invoice = invoiceRepository.findById(invoiceId)
            ?: return@dbQuery Result.Failure(InvoiceNotFound())

        val client = clientRepository.findById(invoice.clientId)
            ?: return@dbQuery Result.Failure(ClientNotFound())

        val tenant = tenantRepository.findById(invoice.tenantId)
            ?: return@dbQuery Result.Failure(TenantNotFound())

        // Generate UBL XML
        val ublXml = peppolGenerator.generateUBL(invoice, client, tenant)

        // Send to Access Point
        val result = peppolService.sendInvoice(
            ublXml = ublXml,
            senderPeppolId = tenant.vatNumber!!,
            recipientPeppolId = client.vatNumber!!
        )

        when (result) {
            is PeppolTransmissionResult.Success -> {
                // Update invoice
                Invoices.update({ Invoices.id eq invoiceId }) {
                    it[status] = "sent"
                    it[peppolId] = result.transmissionId
                    it[peppolSentAt] = result.timestamp
                    it[peppolStatus] = "delivered"
                }

                // Audit log
                auditLog(
                    tenantId = invoice.tenantId,
                    action = "invoice.sent_via_peppol",
                    entityType = "invoice",
                    entityId = invoiceId,
                    newValues = """{"peppolId": "${result.transmissionId}"}"""
                )

                Result.Success(invoiceRepository.findById(invoiceId)!!)
            }

            is PeppolTransmissionResult.Failure -> {
                Result.Failure(PeppolTransmissionError(result.error))
            }
        }
    }
}
```

---

## Testing

### Test Environment

Most Access Point providers offer test environments:

**Pagero Test API:**
```
URL: https://test-api.pagero.com/peppol/v1/
Credentials: Provided by Pagero
```

### Test Cases

1. **Valid Invoice Transmission**
   - Create test invoice
   - Generate UBL XML
   - Send via test API
   - Verify delivery confirmation

2. **Invalid VAT Number**
   - Test with invalid recipient VAT
   - Verify error handling

3. **Missing Required Fields**
   - Omit mandatory UBL fields
   - Verify validation errors

4. **Large Invoice**
   - 100+ line items
   - Verify performance

---

## Compliance Checklist

### Pre-Launch

- [ ] Partner agreement with Access Point provider signed
- [ ] UBL 2.1 XML generation implemented
- [ ] Peppol BIS 3.0 validation implemented
- [ ] Belgian VAT rates configured (21%, 12%, 6%)
- [ ] IBAN/BIC validation implemented
- [ ] Delivery confirmation handling
- [ ] Error handling and retry logic
- [ ] Audit logging of all transmissions
- [ ] Test environment validated

### Go-Live

- [ ] Production API credentials configured
- [ ] User documentation updated
- [ ] Client onboarding process defined
- [ ] Support procedures established
- [ ] Backup PDF generation (fallback)

---

## Related Documentation

- [Architecture](./ARCHITECTURE.md) - System architecture
- [Database Schema](./DATABASE.md) - Invoice data model
- [API Reference](./API.md) - Invoice API
- [Security](./SECURITY.md) - Secure transmission

---

**Last Updated:** October 2025
**Compliance Deadline:** January 1, 2026
