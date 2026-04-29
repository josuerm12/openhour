const API_BASE = window.OPENHOUR_API_BASE || "http://localhost:8080/api";

let services = [];
const defaultTimes = ["10:00 AM", "12:00 PM", "2:00 PM", "4:00 PM", "6:00 PM"];

const state = {
    calendarDate: new Date(),
    selectedDate: "",
    selectedTime: "",
    draft: {},
    latestAppointmentId: null,
    adminAuthenticated: false,
    adminToken: "",
    availability: [],
    daySlots: []
};

const panels = document.querySelectorAll("[data-view-panel]");
const navButtons = document.querySelectorAll("[data-view]");
const calendarDays = document.querySelector("#calendarDays");
const calendarTitle = document.querySelector("#calendarTitle");
const timeList = document.querySelector("#timeList");
const selectedDateText = document.querySelector("#selectedDateText");
const continueToInfo = document.querySelector("#continueToInfo");
const bookingForm = document.querySelector("#bookingForm");
const serviceSelect = document.querySelector("#serviceSelect");
const paymentSummary = document.querySelector("#paymentSummary");
const confirmationSummary = document.querySelector("#confirmationSummary");
const toast = document.querySelector("#toast");

async function api(path, options = {}) {
    const response = await fetch(`${API_BASE}${path}`, {
        headers: {
            "Content-Type": "application/json",
            ...(options.headers || {})
        },
        ...options
    });

    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.error || `Request failed: ${response.status}`);
    }

    if (response.status === 204) {
        return null;
    }
    return response.json();
}

function adminHeaders() {
    return state.adminToken ? { "X-Admin-Token": state.adminToken } : {};
}

function centsToDollars(cents) {
    return `$${(Number(cents || 0) / 100).toFixed(2)}`;
}

function toDateKey(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
}

function parseDateKey(dateKey) {
    const [year, month, day] = dateKey.split("-").map(Number);
    return new Date(year, month - 1, day);
}

function formatDate(dateKey) {
    return parseDateKey(dateKey).toLocaleDateString(undefined, {
        weekday: "long",
        month: "long",
        day: "numeric",
        year: "numeric"
    });
}

function showToast(message) {
    toast.textContent = message;
    toast.classList.add("show");
    window.setTimeout(() => toast.classList.remove("show"), 3200);
}

async function showView(viewName) {
    if (viewName === "adminDashboard" && !state.adminAuthenticated) {
        viewName = "adminLogin";
    }

    panels.forEach((panel) => {
        panel.classList.toggle("active", panel.dataset.viewPanel === viewName);
    });

    try {
        if (viewName === "clientCalendar") {
            await renderCalendar();
        }
        if (viewName === "timeSelect") {
            await renderTimes();
        }
        if (viewName === "payment") {
            renderPaymentSummary();
        }
        if (viewName === "adminDashboard") {
            await renderAdmin();
        }
    } catch (error) {
        showToast(error.message);
    }

    window.scrollTo({ top: 0, behavior: "smooth" });
}

function firstDayOfCalendarMonth() {
    return new Date(state.calendarDate.getFullYear(), state.calendarDate.getMonth(), 1);
}

function lastDayOfCalendarMonth() {
    return new Date(state.calendarDate.getFullYear(), state.calendarDate.getMonth() + 1, 0);
}

async function loadCalendarAvailability() {
    const start = toDateKey(firstDayOfCalendarMonth());
    const end = toDateKey(lastDayOfCalendarMonth());
    state.availability = await api(`/availability?start=${start}&end=${end}`);
}

function slotsForDate(dateKey) {
    return state.availability.filter((slot) => slot.date === dateKey);
}

async function renderCalendar() {
    await loadCalendarAvailability();
    const year = state.calendarDate.getFullYear();
    const month = state.calendarDate.getMonth();
    const firstDay = firstDayOfCalendarMonth();
    const startOffset = firstDay.getDay();
    const daysInMonth = lastDayOfCalendarMonth().getDate();

    calendarTitle.textContent = state.calendarDate.toLocaleDateString(undefined, {
        month: "long",
        year: "numeric"
    });
    calendarDays.innerHTML = "";

    for (let blank = 0; blank < startOffset; blank += 1) {
        calendarDays.appendChild(document.createElement("span"));
    }

    for (let day = 1; day <= daysInMonth; day += 1) {
        const date = new Date(year, month, day);
        const dateKey = toDateKey(date);
        const slots = slotsForDate(dateKey);
        const openSlots = slots.filter((slot) => slot.open && !slot.booked);
        const publishedSlots = slots.filter((slot) => slot.open);
        const button = document.createElement("button");
        button.type = "button";
        button.className = "calendar-day";
        button.textContent = day;

        if (openSlots.length > 0) {
            button.classList.add("available");
        } else if (publishedSlots.length > 0) {
            button.classList.add("full");
            button.disabled = true;
        } else {
            button.disabled = true;
        }

        if (state.selectedDate === dateKey) {
            button.classList.add("selected");
        }

        button.addEventListener("click", () => {
            state.selectedDate = dateKey;
            state.selectedTime = "";
            showView("timeSelect");
        });
        calendarDays.appendChild(button);
    }
}

async function renderTimes() {
    selectedDateText.textContent = state.selectedDate
        ? `Open times for ${formatDate(state.selectedDate)}`
        : "Choose an open time slot for your appointment.";

    continueToInfo.disabled = !state.selectedTime;
    timeList.innerHTML = "";
    state.daySlots = await api(`/availability/day?date=${state.selectedDate}`);

    if (state.daySlots.length === 0) {
        timeList.innerHTML = "<p>No availability is published for this date.</p>";
        return;
    }

    state.daySlots.forEach((slot) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "time-slot";
        button.textContent = slot.time;
        button.disabled = !slot.open || slot.booked;
        button.classList.toggle("active", state.selectedTime === slot.time);
        button.addEventListener("click", () => {
            state.selectedTime = slot.time;
            renderTimes();
        });
        timeList.appendChild(button);
    });
}

async function populateServices() {
    services = await api("/services");
    serviceSelect.innerHTML = '<option value="">Select a service</option>';
    services.forEach((service) => {
        const option = document.createElement("option");
        option.value = service.name;
        option.textContent = `${service.name} - ${centsToDollars(service.priceCents)} (${centsToDollars(service.depositCents)} deposit)`;
        serviceSelect.appendChild(option);
    });
}

function setFieldError(fieldId, message) {
    const error = document.querySelector(`[data-error-for="${fieldId}"]`);
    if (error) {
        error.textContent = message;
    }
}

function validateBookingForm() {
    const name = document.querySelector("#clientName").value.trim();
    const email = document.querySelector("#clientEmail").value.trim();
    const phone = document.querySelector("#clientPhone").value.trim();
    const service = document.querySelector("#serviceSelect").value;
    let valid = true;

    setFieldError("clientName", "");
    setFieldError("clientEmail", "");
    setFieldError("clientPhone", "");
    setFieldError("serviceSelect", "");

    if (name.length < 2) {
        setFieldError("clientName", "Enter your full name.");
        valid = false;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        setFieldError("clientEmail", "Enter a valid email address.");
        valid = false;
    }
    if (!/^[0-9+\-().\s]{7,}$/.test(phone)) {
        setFieldError("clientPhone", "Enter a valid phone number.");
        valid = false;
    }
    if (!service) {
        setFieldError("serviceSelect", "Select a service.");
        valid = false;
    }

    if (!valid) {
        return false;
    }

    state.draft = {
        name,
        email,
        phone,
        service,
        notes: document.querySelector("#clientNotes").value.trim()
    };
    return true;
}

function selectedService() {
    return services.find((service) => service.name === state.draft.service) || services[0];
}

function renderSummary(container, appointment) {
    const rows = [
        ["Name", appointment.name],
        ["Service", appointment.service],
        ["Date", formatDate(appointment.date)],
        ["Time", appointment.time],
        ["Email", appointment.email],
        ["Phone", appointment.phone],
        ["Deposit", centsToDollars(appointment.depositCents)],
        ["Donation", centsToDollars(appointment.donationCents)],
        ["Payment", appointment.paymentStatus || "Pending"]
    ];

    container.innerHTML = rows
        .map(([label, value]) => `<div class="summary-row"><span>${label}</span><strong>${value}</strong></div>`)
        .join("");
}

function renderPaymentSummary() {
    const service = selectedService();
    renderSummary(paymentSummary, {
        ...state.draft,
        date: state.selectedDate,
        time: state.selectedTime,
        depositCents: service.depositCents,
        donationCents: Number(document.querySelector("#donationAmount").value || 0) * 100,
        paymentStatus: "Pending"
    });
}

async function createAppointment() {
    if (!state.selectedDate || !state.selectedTime || !state.draft.name) {
        showToast("Please complete the booking details first.");
        await showView("clientCalendar");
        return;
    }

    const response = await api("/appointments/checkout", {
        method: "POST",
        body: JSON.stringify({
            ...state.draft,
            date: state.selectedDate,
            time: state.selectedTime,
            donationCents: Number(document.querySelector("#donationAmount").value || 0) * 100
        })
    });
    state.latestAppointmentId = response.appointmentId;
    window.location.href = response.checkoutUrl;
}

async function cancelAppointment(id, { refreshAdmin = false } = {}) {
    const confirmed = window.confirm("Cancel this appointment?");
    if (!confirmed) {
        return;
    }
    await api(`/appointments/${id}/cancel`, { method: "PATCH", headers: adminHeaders() });
    showToast("Appointment cancelled.");
    await renderCalendar();
    if (refreshAdmin) {
        await renderAdmin();
    }
}

async function renderAdmin() {
    await Promise.all([renderAppointments(), renderAvailabilityEditor(), renderLog()]);
}

async function renderAppointments() {
    const appointments = await api("/appointments", { headers: adminHeaders() });
    const list = document.querySelector("#appointmentList");
    document.querySelector("#appointmentCount").textContent = `${appointments.length} booked`;

    if (appointments.length === 0) {
        list.innerHTML = '<p class="hint">No upcoming appointments yet.</p>';
        return;
    }

    list.innerHTML = "";
    appointments.forEach((appointment) => {
        const item = document.createElement("article");
        item.className = "appointment-item";
        item.innerHTML = `
            <div class="appointment-meta">
                <strong>${appointment.name} - ${appointment.service}</strong>
                <small>${formatDate(appointment.date)} at ${appointment.time}</small>
                <small>${appointment.email} | ${appointment.phone}</small>
                <small>${centsToDollars(appointment.depositCents + appointment.donationCents)} paid via Stripe</small>
            </div>
            <div class="appointment-actions">
                <button class="mini-button" type="button" data-edit="${appointment.id}">Change Time</button>
                <button class="mini-button danger" type="button" data-cancel="${appointment.id}">Cancel</button>
            </div>
        `;
        list.appendChild(item);
    });

    list.querySelectorAll("[data-cancel]").forEach((button) => {
        button.addEventListener("click", () => cancelAppointment(button.dataset.cancel, { refreshAdmin: true }));
    });

    list.querySelectorAll("[data-edit]").forEach((button) => {
        button.addEventListener("click", () => moveAppointment(button.dataset.edit));
    });
}

async function moveAppointment(id) {
    const appointment = await api(`/appointments/${id}`, { headers: adminHeaders() });
    const slots = await api(`/availability/day?date=${appointment.date}`);
    const openSlot = slots.find((slot) => slot.open && !slot.booked && slot.time !== appointment.time);

    if (!openSlot) {
        showToast("No other open times are available on that date.");
        return;
    }

    await api(`/appointments/${id}/move`, {
        method: "PATCH",
        headers: adminHeaders(),
        body: JSON.stringify({ date: appointment.date, time: openSlot.time })
    });
    showToast(`Appointment moved to ${openSlot.time}.`);
    await renderAdmin();
}

async function renderAvailabilityEditor() {
    const editor = document.querySelector("#availabilityEditor");
    const start = toDateKey(new Date());
    const endDate = new Date();
    endDate.setDate(endDate.getDate() + 14);
    const slots = await api(`/availability?start=${start}&end=${toDateKey(endDate)}`);
    const grouped = slots.reduce((dates, slot) => {
        dates[slot.date] = dates[slot.date] || [];
        dates[slot.date].push(slot);
        return dates;
    }, {});

    editor.innerHTML = "";
    Object.entries(grouped).forEach(([dateKey, daySlots]) => {
        const day = document.createElement("section");
        day.className = "availability-day";
        day.innerHTML = `<h4>${formatDate(dateKey)}</h4><div class="availability-slots"></div>`;
        const slotContainer = day.querySelector(".availability-slots");

        daySlots.forEach((slot) => {
            const button = document.createElement("button");
            button.type = "button";
            button.className = `availability-slot ${slot.open ? "" : "blocked"}`;
            button.textContent = `${slot.time}${slot.booked ? " booked" : ""}`;
            button.disabled = slot.booked;
            button.addEventListener("click", () => toggleSlot(dateKey, slot.time, !slot.open));
            slotContainer.appendChild(button);
        });
        editor.appendChild(day);
    });
}

async function toggleSlot(dateKey, time, open) {
    await api("/availability", {
        method: "PUT",
        headers: adminHeaders(),
        body: JSON.stringify({ date: dateKey, time, open })
    });
    await renderAdmin();
    await renderCalendar();
}

async function renderLog() {
    const log = await api("/activity", { headers: adminHeaders() });
    const activityLog = document.querySelector("#activityLog");

    if (log.length === 0) {
        activityLog.innerHTML = '<p class="hint">No activity has been recorded yet.</p>';
        return;
    }

    activityLog.innerHTML = log
        .map((entry) => `<article class="log-item"><strong>${entry.message}</strong><small>${new Date(entry.createdAt).toLocaleString()}</small></article>`)
        .join("");
}

function switchAdminPanel(panelName) {
    document.querySelectorAll("[data-admin-tab]").forEach((tab) => {
        tab.classList.toggle("active", tab.dataset.adminTab === panelName);
    });
    document.querySelectorAll("[data-admin-panel]").forEach((panel) => {
        panel.classList.toggle("active", panel.dataset.adminPanel === panelName);
    });
}

async function handleStripeReturn() {
    const params = new URLSearchParams(window.location.search);
    const payment = params.get("payment");
    if (!payment) {
        return false;
    }

    if (payment === "success") {
        const appointmentId = params.get("appointmentId");
        const sessionId = params.get("session_id");
        const appointment = await api(`/appointments/${appointmentId}/confirm?session_id=${encodeURIComponent(sessionId)}`, {
            method: "POST"
        });
        state.latestAppointmentId = appointment.id;
        renderSummary(confirmationSummary, appointment);
        await showView("confirmation");
        return true;
    }

    if (payment === "donation_success") {
        document.querySelector("#donationStatus").textContent = "Donation payment completed through Stripe.";
        await showView("donation");
        return true;
    }

    showToast("Stripe checkout was cancelled.");
    await showView(payment === "donation_cancelled" ? "donation" : "clientCalendar");
    return true;
}

navButtons.forEach((button) => {
    button.addEventListener("click", () => showView(button.dataset.view));
});

document.querySelector("#prevMonth").addEventListener("click", () => {
    state.calendarDate.setMonth(state.calendarDate.getMonth() - 1);
    renderCalendar().catch((error) => showToast(error.message));
});

document.querySelector("#nextMonth").addEventListener("click", () => {
    state.calendarDate.setMonth(state.calendarDate.getMonth() + 1);
    renderCalendar().catch((error) => showToast(error.message));
});

continueToInfo.addEventListener("click", () => showView("clientInfo"));

bookingForm.addEventListener("submit", (event) => {
    event.preventDefault();
    if (validateBookingForm()) {
        showView("payment");
    }
});

document.querySelector("#donationAmount").addEventListener("input", renderPaymentSummary);
document.querySelector("#confirmPayment").addEventListener("click", () => {
    createAppointment().catch((error) => showToast(error.message));
});

document.querySelector("#sendDonation").addEventListener("click", async () => {
    const amount = Number(document.querySelector("#standaloneDonation").value || 0);
    const status = document.querySelector("#donationStatus");
    if (amount < 1) {
        status.textContent = "Enter a donation amount of at least $1.";
        return;
    }
    try {
        const response = await api("/donations/checkout", {
            method: "POST",
            body: JSON.stringify({ amountCents: amount * 100 })
        });
        window.location.href = response.checkoutUrl;
    } catch (error) {
        status.textContent = error.message;
    }
});

document.querySelector("#adminForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const username = document.querySelector("#adminUsername").value.trim();
    const password = document.querySelector("#adminPassword").value;
    setFieldError("adminUsername", "");
    setFieldError("adminPassword", "");

    try {
        const result = await api("/auth/login", {
            method: "POST",
            body: JSON.stringify({ username, password })
        });
        if (result.authenticated) {
            state.adminAuthenticated = true;
            state.adminToken = result.token;
            await showView("adminDashboard");
        } else {
            setFieldError("adminPassword", "Invalid owner credentials.");
        }
    } catch (error) {
        setFieldError("adminPassword", error.message);
    }
});

document.querySelector("#logoutAdmin").addEventListener("click", () => {
    state.adminAuthenticated = false;
    state.adminToken = "";
    showView("home");
});

document.querySelectorAll("[data-admin-tab]").forEach((tab) => {
    tab.addEventListener("click", () => switchAdminPanel(tab.dataset.adminTab));
});

(async function init() {
    try {
        await populateServices();
        const handledReturn = await handleStripeReturn();
        if (!handledReturn) {
            await renderCalendar();
        }
    } catch (error) {
        showToast(error.message);
    }
})();
