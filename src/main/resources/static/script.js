const form = document.getElementById("loan-form");
const emptyState = document.getElementById("empty-state");
const recommendationCard = document.getElementById("recommendation-card");
const scheduleEmpty = document.getElementById("schedule-empty");
const tableWrapper = document.getElementById("table-wrapper");
const scheduleBody = document.getElementById("schedule-body");
const scheduleSummary = document.getElementById("schedule-summary");
const schemeList = document.getElementById("scheme-list");

let latestLoanInput = null;
let latestRecommendation = null;

function formatCurrency(value) {
    return new Intl.NumberFormat("en-IN", {
        style: "currency",
        currency: "INR",
        maximumFractionDigits: 2
    }).format(value);
}

function showError(message) {
    let errorMessage = document.getElementById("error-message");

    if (!errorMessage) {
        errorMessage = document.createElement("p");
        errorMessage.id = "error-message";
        errorMessage.className = "error-message";
        form.appendChild(errorMessage);
    }

    errorMessage.textContent = message;
}

function clearError() {
    const errorMessage = document.getElementById("error-message");

    if (errorMessage) {
        errorMessage.textContent = "";
    }
}

function getLoanInput() {
    return {
        purpose: document.getElementById("purpose").value,
        monthlyIncome: Number(document.getElementById("income").value),
        existingMonthlyDebt: Number(document.getElementById("debt").value),
        loanAmount: Number(document.getElementById("loanAmount").value),
        tenureYears: Number(document.getElementById("tenure").value)
    };
}

function showTab(tabId) {
    document.querySelectorAll(".tab-button").forEach((button) => {
        button.classList.toggle("active", button.dataset.tab === tabId);
    });

    document.querySelectorAll(".tab-content").forEach((content) => {
        content.classList.toggle("active", content.id === tabId);
    });
}

function renderSchemes(schemes) {
    schemeList.innerHTML = "";

    if (!schemes || schemes.length === 0) {
        schemeList.innerHTML =
            "<p>No alternative schemes match this financial profile.</p>";
        return;
    }

    schemes.forEach((scheme) => {
        const card = document.createElement("div");
        card.className = "scheme-card";

        card.innerHTML = `
            <h4>${scheme.schemeName}</h4>
            <p>${scheme.description}</p>
            <div class="scheme-details">
                <span>${scheme.annualInterestRate}% p.a.</span>
                <span>${scheme.tenureYears} years</span>
                <span>${formatCurrency(scheme.estimatedEmi)} EMI</span>
            </div>
        `;

        schemeList.appendChild(card);
    });
}

function updateRecommendation(result) {
    emptyState.classList.add("hidden");
    recommendationCard.classList.remove("hidden");

    document.getElementById("product-name").textContent =
        result.recommendedProduct;

    document.getElementById("emi").textContent =
        formatCurrency(result.estimatedEmi);

    document.getElementById("interest-rate").textContent =
        `${result.annualInterestRate}% p.a.`;

    document.getElementById("dti").textContent =
        `${result.debtToIncomeRatio}%`;

    document.getElementById("approved-tenure").textContent =
        `${result.approvedTenureYears} years`;

    document.getElementById("recommendation-message").textContent =
        result.message;

    const statusBadge = document.getElementById("status-badge");

    statusBadge.textContent = result.eligibilityStatus;
    statusBadge.className = "status-badge";

    if (result.eligibilityStatus === "APPROVED") {
        statusBadge.classList.add("status-approved");
    } else if (result.eligibilityStatus === "CONDITIONAL") {
        statusBadge.classList.add("status-conditional");
    } else {
        statusBadge.classList.add("status-rejected");
    }

    renderSchemes(result.availableSchemes);
}

async function getRecommendation(event) {
    event.preventDefault();
    clearError();

    const input = getLoanInput();

    try {
        const response = await fetch("/api/recommend", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(input)
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || "Unable to analyse the loan request.");
        }

        latestLoanInput = {
            ...input,
            tenureYears: data.approvedTenureYears
        };

        latestRecommendation = data;
        updateRecommendation(data);

    } catch (error) {
        showError(error.message);
    }
}

async function generateSchedule() {
    clearError();

    if (!latestRecommendation
            || latestRecommendation.eligibilityStatus === "REJECTED") {
        showError(
            "A repayment schedule is available only for approved or conditional schemes."
        );
        showTab("finder");
        return;
    }

    try {
        const response = await fetch("/api/amortization", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(latestLoanInput)
        });

        const schedule = await response.json();

        if (!response.ok) {
            throw new Error(
                schedule.error || "Unable to generate the repayment schedule."
            );
        }

        scheduleBody.innerHTML = "";

        schedule.forEach((entry) => {
            const row = document.createElement("tr");

            row.innerHTML = `
                <td>${entry.month}</td>
                <td>${formatCurrency(entry.emi)}</td>
                <td>${formatCurrency(entry.principalPaid)}</td>
                <td>${formatCurrency(entry.interestPaid)}</td>
                <td>${formatCurrency(entry.remainingBalance)}</td>
            `;

            scheduleBody.appendChild(row);
        });

        scheduleEmpty.classList.add("hidden");
        tableWrapper.classList.remove("hidden");

        scheduleSummary.textContent =
            `${latestRecommendation.recommendedProduct} · ` +
            `${formatCurrency(latestRecommendation.estimatedEmi)} monthly EMI`;

        scheduleSummary.classList.remove("hidden");
        showTab("schedule");

    } catch (error) {
        showError(error.message);
        showTab("finder");
    }
}

form.addEventListener("submit", getRecommendation);

document.getElementById("schedule-button")
    .addEventListener("click", generateSchedule);

document.querySelectorAll(".tab-button").forEach((button) => {
    button.addEventListener("click", () => showTab(button.dataset.tab));
});