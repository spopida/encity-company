package uk.co.encity.company;

public class Company {
    private final String companyNumber;
    private final String details;

    public Company(final String nbr, final String details) {
        this.companyNumber = nbr;
        this.details = details;
    }

    public String getCompanyNumber() { return this.companyNumber; }
    public String getDetails() { return this.details; }
}
