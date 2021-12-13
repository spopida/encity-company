package uk.co.encity.company;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.LocalDate;

/**
 * De-serialises a response received from Companies House in relation to a company query, into
 * a {@link CompanyResponse} POJO.
 */
public class CompanyResponseDeserializer extends StdDeserializer<CompanyResponse> {

    public CompanyResponseDeserializer() { this(null); }
    public CompanyResponseDeserializer(Class<?> valueClass) { super(valueClass); }

    @Override
    public CompanyResponse deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode rootNode = jp.getCodec().readTree(jp);

        CompanyResponse response = new CompanyResponse();

        // There is some checking for fields that may be missing below, but we could be far more defensive.  At the
        // time of writing we are still prototyping, but this may have to be re-visited with a 'paranoid' hat on

        // Company status and name
        response.setCompanyStatus(rootNode.get("company_status").asText());
        response.setCompanyName(rootNode.get("company_name").asText());

        // Registered office address
        if (rootNode.hasNonNull("registered_office_address")) {
            JsonNode roaNode = rootNode.get("registered_office_address");

            response.registeredOfficeAddress.postalCode = roaNode.hasNonNull("postal_code") ?
                roaNode.get("postal_code").asText() : null;
            response.registeredOfficeAddress.region = roaNode.hasNonNull("region") ?
                roaNode.get("region").asText() : null;
            response.registeredOfficeAddress.country = roaNode.hasNonNull("country") ?
                roaNode.get("country").asText() : null;
            response.registeredOfficeAddress.addressLine1 = roaNode.hasNonNull("address_line_1") ?
                roaNode.get("address_line_1").asText() : null;
            response.registeredOfficeAddress.locality = roaNode.hasNonNull("locality") ?
                roaNode.get("locality").asText() : null;
        } else {
            response.registeredOfficeAddress = null;
        }

        if (rootNode.hasNonNull("confirmation_statement")) {
            // Confirmation statement details
            JsonNode csNode = rootNode.get("confirmation_statement");
            response.confirmationStatement.overdue = csNode.get("overdue").asBoolean();
            response.confirmationStatement.nextDue = LocalDate.parse(csNode.get("next_due").asText());
            response.confirmationStatement.nextMadeUpTo = LocalDate.parse(csNode.get("next_made_up_to").asText());
            response.confirmationStatement.lastMadeUpTo = csNode.hasNonNull("last_made_up_to") ? LocalDate.parse(csNode.get("last_made_up_to").asText()) : null;
        } else {
            response.confirmationStatement = null;
        }

        // Accounts details
        JsonNode acctsNode = rootNode.get("accounts");

        if (acctsNode.hasNonNull("accounting_reference_date")) {
            response.accounts.accountingReferenceDate.day = acctsNode.get("accounting_reference_date").get("day").asText();
            response.accounts.accountingReferenceDate.month = acctsNode.get("accounting_reference_date").get("month").asText();
        } else {
            response.accounts.accountingReferenceDate = null;
        }

        if (acctsNode.hasNonNull("next_accounts")) {
            response.accounts.nextAccounts.periodStartOn = LocalDate.parse(acctsNode.get("next_accounts").get("period_start_on").asText());
            response.accounts.nextAccounts.periodEndOn = LocalDate.parse(acctsNode.get("next_accounts").get("period_end_on").asText());
            response.accounts.nextAccounts.overdue = acctsNode.get("next_accounts").get("overdue").asBoolean();
            response.accounts.nextAccounts.dueOn = LocalDate.parse(acctsNode.get("next_accounts").get("due_on").asText());
        } else {
            response.accounts.nextAccounts = null;
        }

        if (acctsNode.hasNonNull("last_accounts")) {
            JsonNode lastAcctsNode = acctsNode.get("last_accounts");

            response.accounts.lastAccounts.madeUpTo = (lastAcctsNode.hasNonNull("made_up_to")) ?
                LocalDate.parse(lastAcctsNode.get("made_up_to").asText()) : null;
            response.accounts.lastAccounts.type = (lastAcctsNode.hasNonNull("type")) ?
                lastAcctsNode.get("type").asText() : null;
            response.accounts.lastAccounts.periodStartOn = (lastAcctsNode.hasNonNull("period_start_on")) ?
                LocalDate.parse(lastAcctsNode.get("period_start_on").asText()) : null;
            response.accounts.lastAccounts.periodEndOn = (lastAcctsNode.hasNonNull("period_end_on")) ?
                LocalDate.parse(lastAcctsNode.get("period_end_on").asText()) : null;
        } else {
            response.accounts.lastAccounts = null;
        }

        response.accounts.nextMadeUpTo = (acctsNode.hasNonNull("next_made_up_to")) ?
            LocalDate.parse(acctsNode.get("next_made_up_to").asText()) : null;

        response.accounts.overdue = (acctsNode.hasNonNull("overdue")) ?
            acctsNode.get("overdue").asBoolean() : false;

        return response;
    }
}
