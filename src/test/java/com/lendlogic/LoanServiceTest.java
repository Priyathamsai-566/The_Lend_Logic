package com.lendlogic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoanServiceTest {

    private final LoanService loanService = new LoanService();

    @Test
    void shouldRecommendPrimeHomeLoanForStrongProfile() {
        LoanService.LoanInput input = new LoanService.LoanInput(
                "HOME", 100000, 5000, 1000000, 10
        );

        LoanService.LoanResult result = loanService.recommend(input);

        assertEquals("APPROVED", result.eligibilityStatus());
        assertEquals("Prime Home Loan", result.recommendedProduct());
        assertEquals(8.60, result.annualInterestRate());
        assertFalse(result.availableSchemes().isEmpty());
    }

    @Test
    void shouldProvideConditionalPersonalLoanRecommendation() {
        LoanService.LoanInput input = new LoanService.LoanInput(
                "PERSONAL", 50000, 8000, 600000, 5
        );

        LoanService.LoanResult result = loanService.recommend(input);

        assertEquals("CONDITIONAL", result.eligibilityStatus());
        assertTrue(result.debtToIncomeRatio() >= 40);
        assertTrue(result.debtToIncomeRatio() <= 50);
    }

    @Test
    void shouldRejectHighDebtProfile() {
        LoanService.LoanInput input = new LoanService.LoanInput(
                "DEBT", 30000, 20000, 1000000, 5
        );

        LoanService.LoanResult result = loanService.recommend(input);

        assertEquals("REJECTED", result.eligibilityStatus());
        assertTrue(result.availableSchemes().isEmpty());
    }

    @Test
    void shouldRecommendCarFinance() {
        LoanService.LoanInput input = new LoanService.LoanInput(
                "CAR", 75000, 5000, 700000, 6
        );

        LoanService.LoanResult result = loanService.recommend(input);

        assertEquals("APPROVED", result.eligibilityStatus());
        assertEquals("New Car Finance", result.recommendedProduct());
        assertEquals(8.40, result.annualInterestRate());
    }

    @Test
    void shouldRejectNegativeLoanAmount() {
        LoanService.LoanInput input = new LoanService.LoanInput(
                "HOME", 60000, 0, -100000, 10
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> loanService.recommend(input)
        );
    }

    @Test
    void shouldCreateCorrectMonthlySchedule() {
        LoanService.LoanInput input = new LoanService.LoanInput(
                "EDUCATION", 80000, 5000, 500000, 2
        );

        List<LoanService.ScheduleEntry> schedule =
                loanService.generateAmortizationSchedule(input);

        assertEquals(24, schedule.size());
        assertEquals(
                0.0,
                schedule.get(schedule.size() - 1).remainingBalance()
        );
    }
}