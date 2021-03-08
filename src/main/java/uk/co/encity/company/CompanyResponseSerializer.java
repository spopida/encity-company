package uk.co.encity.company;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class CompanyResponseSerializer extends StdSerializer<CompanyResponse> {
    public CompanyResponseSerializer() {
        this(null);
    }

    public CompanyResponseSerializer(Class<CompanyResponse> m) {
        super(m);
    }

    @Override
    public void serialize(CompanyResponse value, JsonGenerator jGen, SerializerProvider provider)
            throws IOException, JsonProcessingException {
        // Top level details
        jGen.writeStartObject();
        jGen.writeStringField("companyStatus", value.getCompanyStatus());
        jGen.writeStringField("companyName", value.getCompanyName());

        // Confirmation Statement Details

        CompanyResponse.ConfirmationStatement cs = value.getConfirmationStatement();
        jGen.writeFieldName("confirmationStatement");
        jGen.writeStartObject();
        jGen.writeStringField("lastMadeUpTo", cs.lastMadeUpTo.toString());
        jGen.writeStringField("nextMadeUpTo", cs.nextMadeUpTo.toString());
        jGen.writeStringField("nextDue", cs.nextDue.toString());
        jGen.writeEndObject();

        // Accounts Details
        CompanyResponse.Accounts accts = value.getAccounts();
        jGen.writeFieldName("accounts");
        jGen.writeStartObject();

        // Next Accounts
        CompanyResponse.Accounts.NextAccounts next = accts.nextAccounts;
        jGen.writeFieldName("nextAccounts");
        jGen.writeStartObject();

        jGen.writeStringField("periodStartOn", next.periodStartOn.toString());
        jGen.writeStringField("periodEndOn", next.periodEndOn.toString());
        jGen.writeStringField("dueOn", next.dueOn.toString());

        jGen.writeEndObject(); // end of Next Accounts
        jGen.writeEndObject(); // end of Accounts

        jGen.writeEndObject(); // end of top level object
    }

}
