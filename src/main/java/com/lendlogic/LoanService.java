package com.lendlogic;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

@Service
public class LoanService {

    public LoanResult recommend(LoanInput input) {
        validateInput(input);

        List<LoanPlan> plans = getPlansForPurpose(input.purpose());
        List<LoanOption> suitableSchemes = new ArrayList<>();

        for (LoanPlan plan : plans) {
            int finalTenure = Math.min(input.tenureYears(), plan.maximumTenureYears());

            double emi = calculateEmi(
                    input.loanAmount(),
                    plan.annualInterestRate(),
                    finalTenure
            );

            double dti = ((input.existingMonthlyDebt() + emi)
                    / input.monthlyIncome()) * 100;

            if (input.monthlyIncome() >= plan.minimumMonthlyIncome()
                    && dti <= plan.maximumDtiRatio()) {

                suitableSchemes.add(new LoanOption(
                        plan.name(),
                        plan.annualInterestRate(),
                        finalTenure,
                        round(emi),
                        round(dti),
                        plan.description()
                ));
            }
        }

        if (suitableSchemes.isEmpty()) {
            LoanPlan fallbackPlan = plans.get(0);
            int finalTenure = Math.min(
                    input.tenureYears(),
                    fallbackPlan.maximumTenureYears()
            );

            double emi = calculateEmi(
                    input.loanAmount(),
                    fallbackPlan.annualInterestRate(),
                    finalTenure
            );

            double dti = ((input.existingMonthlyDebt() + emi)
                    / input.monthlyIncome()) * 100;

            return new LoanResult(
                    fallbackPlan.name(),
                    fallbackPlan.annualInterestRate(),
                    finalTenure,
                    round(emi),
                    round(dti),
                    "REJECTED",
                    "No suitable scheme was found with the current income, debt, and requested amount. Consider reducing debt, reducing the loan amount, or increasing the repayment tenure.",
                    List.of()
            );
        }

        LoanOption bestScheme = suitableSchemes.get(0);
        String status = bestScheme.debtToIncomeRatio() < 40
                ? "APPROVED"
                : "CONDITIONAL";

        String message = status.equals("APPROVED")
                ? "You qualify for the recommended scheme. Other suitable schemes are shown below for comparison."
                : "You may qualify conditionally. Consider reducing existing debt or choosing a longer tenure where available.";

        return new LoanResult(
                bestScheme.schemeName(),
                bestScheme.annualInterestRate(),
                bestScheme.tenureYears(),
                bestScheme.estimatedEmi(),
                bestScheme.debtToIncomeRatio(),
                status,
                message,
                suitableSchemes
        );
    }

    public List<ScheduleEntry> generateAmortizationSchedule(LoanInput input) {
        LoanResult recommendation = recommend(input);

        if (recommendation.eligibilityStatus().equals("REJECTED")) {
            throw new IllegalArgumentException(
                    "An amortization schedule cannot be generated for a rejected loan request."
            );
        }

        double monthlyRate = recommendation.annualInterestRate() / 12 / 100;
        int totalMonths = recommendation.approvedTenureYears() * 12;
        double emi = recommendation.estimatedEmi();
        double remainingBalance = input.loanAmount();

        List<ScheduleEntry> schedule = new ArrayList<>();

        for (int month = 1; month <= totalMonths; month++) {
            double interest = remainingBalance * monthlyRate;
            double principal = emi - interest;

            if (month == totalMonths || principal > remainingBalance) {
                principal = remainingBalance;
                emi = principal + interest;
            }

            remainingBalance -= principal;

            schedule.add(new ScheduleEntry(
                    month,
                    round(emi),
                    round(principal),
                    round(interest),
                    round(Math.max(remainingBalance, 0))
            ));
        }

        return schedule;
    }

    public double calculateEmi(
            double principal,
            double annualInterestRate,
            int tenureYears
    ) {
        double monthlyRate = annualInterestRate / 12 / 100;
        int months = tenureYears * 12;

        return principal * monthlyRate
                * Math.pow(1 + monthlyRate, months)
                / (Math.pow(1 + monthlyRate, months) - 1);
    }

    private List<LoanPlan> getPlansForPurpose(String purpose) {
        String value = purpose == null ? "PERSONAL"
                : purpose.toUpperCase(Locale.ROOT);

        return switch (value) {
            case "HOME" -> List.of(
                    new LoanPlan(
                            "Prime Home Loan", 8.60, 30, 75000, 40,
                            "Designed for applicants with strong income and a low debt burden."
                    ),
                    new LoanPlan(
                            "Standard Home Loan", 8.90, 30, 30000, 45,
                            "A long-tenure home-finance plan for eligible salaried or self-employed applicants."
                    ),
                    new LoanPlan(
                            "Home Improvement Loan", 10.25, 15, 25000, 50,
                            "Suitable for renovation, repairs, furnishing, or home improvements."
                    )
            );

            case "CAR" -> List.of(
                    new LoanPlan(
                            "New Car Finance", 8.40, 7, 30000, 45,
                            "A standard finance option for purchasing a new vehicle."
                    ),
                    new LoanPlan(
                            "Electric Vehicle Finance", 8.20, 7, 35000, 45,
                            "A benchmark scheme for eligible electric-vehicle buyers."
                    ),
                    new LoanPlan(
                            "Used Car Finance", 10.75, 5, 25000, 45,
                            "A shorter-tenure finance option for pre-owned vehicles."
                    )
            );

            case "EDUCATION" -> List.of(
                    new LoanPlan(
                            "Scholar Education Loan", 9.25, 15, 60000, 50,
                            "A lower-rate benchmark plan for strong family-income profiles."
                    ),
                    new LoanPlan(
                            "Standard Education Loan", 10.50, 15, 25000, 50,
                            "A general education-finance plan; lender terms may include a moratorium period."
                    ),
                    new LoanPlan(
                            "Overseas Education Loan", 11.25, 15, 50000, 50,
                            "A benchmark plan for higher-cost overseas education requirements."
                    )
            );

            case "DEBT" -> List.of(
                    new LoanPlan(
                            "Balance Transfer Plan", 10.75, 5, 50000, 45,
                            "For applicants seeking to move eligible debt to a lower-cost repayment plan."
                    ),
                    new LoanPlan(
                            "Debt Consolidation Plan", 11.50, 5, 35000, 50,
                            "Combines eligible debts into one monthly repayment."
                    )
            );

            case "BUSINESS" -> List.of(
                    new LoanPlan(
                            "SME Term Loan", 12.50, 7, 75000, 45,
                            "A longer-term benchmark option for business equipment or expansion."
                    ),
                    new LoanPlan(
                            "Working Capital Loan", 13.00, 5, 50000, 45,
                            "A shorter-term benchmark option for operational business needs."
                    )
            );

            default -> List.of(
                    new LoanPlan(
                            "Salary Personal Loan", 11.00, 5, 25000, 45,
                            "A standard unsecured loan option for eligible income profiles."
                    ),
                    new LoanPlan(
                            "Flexi Personal Loan", 12.00, 5, 18000, 50,
                            "A flexible benchmark plan for moderate borrowing requirements."
                    )
            );
        };
    }

    private void validateInput(LoanInput input) {
        if (input == null) {
            throw new IllegalArgumentException("Loan details are required.");
        }

        if (input.monthlyIncome() <= 0) {
            throw new IllegalArgumentException(
                    "Monthly income must be greater than zero."
            );
        }

        if (input.existingMonthlyDebt() < 0) {
            throw new IllegalArgumentException(
                    "Existing monthly debt cannot be negative."
            );
        }

        if (input.loanAmount() <= 0) {
            throw new IllegalArgumentException(
                    "Loan amount must be greater than zero."
            );
        }

        if (input.tenureYears() <= 0) {
            throw new IllegalArgumentException(
                    "Loan tenure must be greater than zero."
            );
        }
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public record LoanInput(
            String purpose,
            double monthlyIncome,
            double existingMonthlyDebt,
            double loanAmount,
            int tenureYears
    ) {
    }

    public record LoanResult(
            String recommendedProduct,
            double annualInterestRate,
            int approvedTenureYears,
            double estimatedEmi,
            double debtToIncomeRatio,
            String eligibilityStatus,
            String message,
            List<LoanOption> availableSchemes
    ) {
    }

    public record LoanOption(
            String schemeName,
            double annualInterestRate,
            int tenureYears,
            double estimatedEmi,
            double debtToIncomeRatio,
            String description
    ) {
    }

    public record ScheduleEntry(
            int month,
            double emi,
            double principalPaid,
            double interestPaid,
            double remainingBalance
    ) {
    }

    private record LoanPlan(
            String name,
            double annualInterestRate,
            int maximumTenureYears,
            double minimumMonthlyIncome,
            double maximumDtiRatio,
            String description
    ) {
    }
}