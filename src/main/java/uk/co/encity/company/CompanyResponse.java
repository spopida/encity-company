package uk.co.encity.company;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.tomcat.jni.Local;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;

/**
 * Represents data received from Companies House in response to a company request
 */
@Getter @Setter
public class CompanyResponse {

    @NoArgsConstructor
    @Getter
    protected class RegisteredOfficeAddress {
        protected String postalCode;
        protected String region;
        protected String addressLine1;
        protected String locality;
    }

    @NoArgsConstructor
    @Getter
    protected class ConfirmationStatement {
        protected boolean overdue;
        protected LocalDate nextDue;
        protected LocalDate nextMadeUpTo;
        protected LocalDate lastMadeUpTo;
    }

    @Getter
    protected class Accounts {

        @NoArgsConstructor
        @Getter
        protected class AccountingReferenceDate {
            protected String day;
            protected String month;
        }

        @NoArgsConstructor
        @Getter
        protected class LastAccounts {
            protected LocalDate madeUpTo;
            protected String type;
            protected LocalDate periodStartOn;
            protected LocalDate periodEndOn;
        }

        @NoArgsConstructor
        @Getter
        protected class NextAccounts {
            protected LocalDate periodStartOn;
            protected LocalDate periodEndOn;
            protected boolean overdue;
            protected LocalDate dueOn;
        }

        protected AccountingReferenceDate accountingReferenceDate;
        protected LocalDate nextDue;
        protected NextAccounts nextAccounts;
        protected LastAccounts lastAccounts;
        protected LocalDate nextMadeUpTo;
        protected boolean overdue;

        protected Accounts()  {
            this.accountingReferenceDate = new AccountingReferenceDate();
            this.lastAccounts = new LastAccounts();
            this.nextAccounts = new NextAccounts();
        }

    }
    protected RegisteredOfficeAddress registeredOfficeAddress;
    protected ConfirmationStatement confirmationStatement;
    protected Accounts accounts;

    private String companyStatus;
    private String companyName;

    public CompanyResponse() {
        this.registeredOfficeAddress = new RegisteredOfficeAddress();
        this.confirmationStatement = new ConfirmationStatement();
        this.accounts = new Accounts();
    }
}
