const services = [
    { name: "Silk Press", price: 75, deposit: 25 },
    { name: "Knotless Braids", price: 180, deposit: 45 },
    { name: "Loc Retwist", price: 95, deposit: 30 },
    { name: "Wash and Style", price: 65, deposit: 20 },
    { name: "Color Consultation", price: 40, deposit: 15 }
];

const defaultTimes = ["10:00 AM", "12:00 PM", "2:00 PM", "4:00 PM", "6:00 PM"];
const storageKeys = {
    appointments: "openhour.appointments",
    availability: "openhour.availability",
    log: "openhour.log"
};

const state = {
    calendarDate: new Date(2026, 3, 1),
    selectedDate: "",
    selectedTime: "",
    draft: {},
    latestAppointmentId: null,
    adminAuthenticated: false
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

function readJson(key, fallback) {
    try {
        const value = localStorage.getItem(key);
        return value ? JSON.parse(value) : fallback;
    } catch (error) {
        return fallback;
    }
}

function writeJson(key, value) {
    localStorage.setItem(key, JSON.stringify(value));
}

function seedAvailability() {
    const existing = readJson(storageKeys.availability, null);
    if (existing) {
        return existing;
    }

    const availability = {};
    const start = new Date(2026, 3, 20);
    for (let index = 0; index < 42; index += 1) {
        const date = new Date(start);
        date.setDate(start.getDate() + index);
        const day = date.getDay();
        if (day !== 0 && day !== 1) {
            availability[toDateKey(date)] = [...defaultTimes];
        }
    }

    writeJson(storageKeys.availability, availability);
    return availability;
}

function getAppointments() {
    return readJson(storageKeys.appointments, []);
}

function setAppointments(appointments) {
    writeJson(storageKeys.appointments, appointments);
}

function getAvailability() {
    return seedAvailability();
}

function setAvailability(availability) {
    writeJson(storageKeys.availability, availability);
}

function getLog() {
    return readJson(storageKeys.log, []);
}

function addLog(message) {
    const log = getLog();
    log.unshift({
        id: crypto.randomUUID(),
        message,
        createdAt: new Date().toLocaleString()
    });
    writeJson(storageKeys.log, log.slice(0, 30));
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

function showView(viewName) {
    if (viewName === "adminDashboard" && !state.adminAuthenticated) {
        viewName = "adminLogin";
    }

    panels.forEach((panel) => {
        panel.classList.toggle("active", panel.dataset.viewPanel === viewName);
    });

    if (viewName === "clientCalendar") {
        renderCalendar();
    }
    if (viewName === "timeSelect") {
        renderTimes();
    }
    if (viewName === "payment") {
        renderPaymentSummary();
    }
    if (viewName === "adminDashboard") {
        renderAdmin();
    }

    window.scrollTo({ top: 0, behavior: "smooth" });
}

function showToast(message) {
    toast.textContent = message;
    toast.classList.add("show");
    window.setTimeout(() => toast.classList.remove("show"), 2800);
}

function getBookedTimes(dateKey) {
    return getAppointments()
        .filter((appointment) => appointment.date === dateKey && appointment.status === "confirmed")
        .map((appointment) => appointment.time);
}

function getOpenTimes(dateKey) {
    const availability = getAvailability();
    const slots = availability[dateKey] || [];
    const bookedTimes = getBookedTimes(dateKey);
    return slots.filter((slot) => !bookedTimes.includes(slot));
}

function renderCalendar() {
    const year = state.calendarDate.getFullYear();
    const month = state.calendarDate.getMonth();
    const firstDay = new Date(year, month, 1);
    const startOffset = firstDay.getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();

    calendarTitle.textContent = state.calendarDate.toLocaleDateString(undefined, {
        month: "long",
        year: "numeric"
    });
    calendarDays.innerHTML = "";

    for (let blank = 0; blank < startOffset; blank += 1) {
        const spacer = document.createElement("span");
        calendarDays.appendChild(spacer);
    }

    for (let day = 1; day <= daysInMonth; day += 1) {
        const date = new Date(year, month, day);
        const dateKey = toDateKey(date);
        const availability = getAvailability()[dateKey] || [];
        const openTimes = getOpenTimes(dateKey);
        const button = document.createElement("button");
        button.type = "button";
        button.className = "calendar-day";
        button.textContent = day;

        if (availability.length > 0 && openTimes.length > 0) {
            button.classList.add("available");
        } else if (availability.length > 0) {
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

function renderTimes() {
    selectedDateText.textContent = state.selectedDate
        ? `Open times for ${formatDate(state.selectedDate)}`
        : "Choose an open time slot for your appointment.";

    timeList.innerHTML = "";
    continueToInfo.disabled = !state.selectedTime;

    const availability = getAvailability()[state.selectedDate] || [];
    const booked = getBookedTimes(state.selectedDate);

    if (availability.length === 0) {
        timeList.innerHTML = "<p>No availability is published for this date.</p>";
        return;
    }

    availability.forEach((time) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "time-slot";
        button.textContent = time;
        button.disabled = booked.includes(time);
        button.classList.toggle("active", state.selectedTime === time);
        button.addEventListener("click", () => {
            state.selectedTime = time;
            renderTimes();
        });
        timeList.appendChild(button);
    });
}

function populateServices() {
    serviceSelect.innerHTML = '<option value="">Select a service</option>';
    services.forEach((service) => {
        const option = document.createElement("option");
        option.value = service.name;
        option.textContent = `${service.name} - $${service.price} ($${service.deposit} deposit)`;
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
    const service = services.find((item) => item.name === appointment.service) || selectedService();
    const donation = Number(appointment.donation || 0);
    const rows = [
        ["Name", appointment.name],
        ["Service", appointment.service],
        ["Date", formatDate(appointment.date)],
        ["Time", appointment.time],
        ["Email", appointment.email],
        ["Phone", appointment.phone],
        ["Deposit", `$${service.deposit}`],
        ["Donation", `$${donation}`],
        ["Confirmation", appointment.paymentId || "Pending"]
    ];

    container.innerHTML = rows
        .map(([label, value]) => `<div class="summary-row"><span>${label}</span><strong>${value}</strong></div>`)
        .join("");
}

function renderPaymentSummary() {
    renderSummary(paymentSummary, {
        ...state.draft,
        date: state.selectedDate,
        time: state.selectedTime,
        donation: Number(document.querySelector("#donationAmount").value || 0),
        paymentId: "Pending"
    });
}

function createAppointment() {
    if (!state.selectedDate || !state.selectedTime || !state.draft.name) {
        showToast("Please complete the booking details first.");
        showView("clientCalendar");
        return;
    }

    if (!getOpenTimes(state.selectedDate).includes(state.selectedTime)) {
        showToast("That time was just booked or blocked. Please choose another slot.");
        showView("timeSelect");
        return;
    }

    const service = selectedService();
    const donation = Number(document.querySelector("#donationAmount").value || 0);
    const appointment = {
        id: crypto.randomUUID(),
        ...state.draft,
        date: state.selectedDate,
        time: state.selectedTime,
        donation,
        deposit: service.deposit,
        paymentId: `PAY-${Date.now().toString().slice(-6)}`,
        status: "confirmed",
        createdAt: new Date().toISOString()
    };

    const appointments = getAppointments();
    appointments.push(appointment);
    setAppointments(appointments);
    state.latestAppointmentId = appointment.id;
    addLog(`Booked ${appointment.service} for ${appointment.name} on ${formatDate(appointment.date)} at ${appointment.time}.`);
    renderSummary(confirmationSummary, appointment);
    showView("confirmation");
}

function cancelAppointment(id) {
    const appointments = getAppointments();
    const appointment = appointments.find((item) => item.id === id);
    if (!appointment) {
        return;
    }

    const confirmed = window.confirm(`Cancel ${appointment.name}'s appointment on ${formatDate(appointment.date)} at ${appointment.time}?`);
    if (!confirmed) {
        return;
    }

    appointment.status = "cancelled";
    appointment.cancelledAt = new Date().toISOString();
    setAppointments(appointments);
    addLog(`Cancelled ${appointment.service} for ${appointment.name} on ${formatDate(appointment.date)}.`);
    showToast("Appointment cancelled.");
    renderCalendar();
    renderAdmin();
}

function renderAdmin() {
    renderAppointments();
    renderAvailabilityEditor();
    renderLog();
}

function renderAppointments() {
    const appointments = getAppointments()
        .filter((appointment) => appointment.status === "confirmed")
        .sort((a, b) => `${a.date} ${a.time}`.localeCompare(`${b.date} ${b.time}`));
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
            </div>
            <div class="appointment-actions">
                <button class="mini-button" type="button" data-edit="${appointment.id}">Change Time</button>
                <button class="mini-button danger" type="button" data-cancel="${appointment.id}">Cancel</button>
            </div>
        `;
        list.appendChild(item);
    });

    list.querySelectorAll("[data-cancel]").forEach((button) => {
        button.addEventListener("click", () => cancelAppointment(button.dataset.cancel));
    });

    list.querySelectorAll("[data-edit]").forEach((button) => {
        button.addEventListener("click", () => moveAppointment(button.dataset.edit));
    });
}

function moveAppointment(id) {
    const appointments = getAppointments();
    const appointment = appointments.find((item) => item.id === id);
    if (!appointment) {
        return;
    }

    const openTimes = getOpenTimes(appointment.date);
    if (openTimes.length === 0) {
        showToast("No other open times are available on that date.");
        return;
    }

    const nextTime = openTimes[0];
    appointment.time = nextTime;
    setAppointments(appointments);
    addLog(`Changed ${appointment.name}'s appointment to ${nextTime} on ${formatDate(appointment.date)}.`);
    showToast(`Appointment moved to ${nextTime}.`);
    renderAdmin();
}

function renderAvailabilityEditor() {
    const editor = document.querySelector("#availabilityEditor");
    const availability = getAvailability();
    const start = new Date(2026, 3, 20);
    editor.innerHTML = "";

    for (let index = 0; index < 14; index += 1) {
        const date = new Date(start);
        date.setDate(start.getDate() + index);
        const dateKey = toDateKey(date);
        const day = document.createElement("section");
        day.className = "availability-day";
        day.innerHTML = `<h4>${formatDate(dateKey)}</h4><div class="availability-slots"></div>`;
        const slots = day.querySelector(".availability-slots");

        defaultTimes.forEach((time) => {
            const isOpen = (availability[dateKey] || []).includes(time);
            const button = document.createElement("button");
            button.type = "button";
            button.className = `availability-slot ${isOpen ? "" : "blocked"}`;
            button.textContent = time;
            button.addEventListener("click", () => toggleSlot(dateKey, time));
            slots.appendChild(button);
        });

        editor.appendChild(day);
    }
}

function toggleSlot(dateKey, time) {
    const availability = getAvailability();
    const slots = new Set(availability[dateKey] || []);
    const isBooked = getBookedTimes(dateKey).includes(time);

    if (isBooked) {
        showToast("Booked slots cannot be blocked until the appointment is cancelled.");
        return;
    }

    if (slots.has(time)) {
        slots.delete(time);
        addLog(`Blocked ${time} on ${formatDate(dateKey)}.`);
    } else {
        slots.add(time);
        addLog(`Opened ${time} on ${formatDate(dateKey)}.`);
    }

    availability[dateKey] = defaultTimes.filter((slot) => slots.has(slot));
    setAvailability(availability);
    renderAdmin();
    renderCalendar();
}

function renderLog() {
    const log = getLog();
    const activityLog = document.querySelector("#activityLog");

    if (log.length === 0) {
        activityLog.innerHTML = '<p class="hint">No activity has been recorded yet.</p>';
        return;
    }

    activityLog.innerHTML = log
        .map((entry) => `<article class="log-item"><strong>${entry.message}</strong><small>${entry.createdAt}</small></article>`)
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

navButtons.forEach((button) => {
    button.addEventListener("click", () => showView(button.dataset.view));
});

document.querySelector("#prevMonth").addEventListener("click", () => {
    state.calendarDate.setMonth(state.calendarDate.getMonth() - 1);
    renderCalendar();
});

document.querySelector("#nextMonth").addEventListener("click", () => {
    state.calendarDate.setMonth(state.calendarDate.getMonth() + 1);
    renderCalendar();
});

continueToInfo.addEventListener("click", () => showView("clientInfo"));

bookingForm.addEventListener("submit", (event) => {
    event.preventDefault();
    if (validateBookingForm()) {
        showView("payment");
    }
});

document.querySelector("#donationAmount").addEventListener("input", renderPaymentSummary);
document.querySelector("#confirmPayment").addEventListener("click", createAppointment);

document.querySelector("#cancelLatest").addEventListener("click", () => {
    if (state.latestAppointmentId) {
        cancelAppointment(state.latestAppointmentId);
        showView("clientCalendar");
    }
});

document.querySelector("#sendDonation").addEventListener("click", () => {
    const amount = Number(document.querySelector("#standaloneDonation").value || 0);
    const status = document.querySelector("#donationStatus");
    if (amount < 1) {
        status.textContent = "Enter a donation amount of at least $1.";
        return;
    }
    addLog(`Received a $${amount} donation.`);
    status.textContent = `Donation confirmation DON-${Date.now().toString().slice(-6)} recorded.`;
});

document.querySelector("#adminForm").addEventListener("submit", (event) => {
    event.preventDefault();
    const username = document.querySelector("#adminUsername").value.trim();
    const password = document.querySelector("#adminPassword").value;
    setFieldError("adminUsername", "");
    setFieldError("adminPassword", "");

    if (username === "owner" && password === "openhour") {
        state.adminAuthenticated = true;
        showView("adminDashboard");
    } else {
        setFieldError("adminPassword", "Use owner / openhour for this prototype.");
    }
});

document.querySelector("#logoutAdmin").addEventListener("click", () => {
    state.adminAuthenticated = false;
    showView("home");
});

document.querySelectorAll("[data-admin-tab]").forEach((tab) => {
    tab.addEventListener("click", () => switchAdminPanel(tab.dataset.adminTab));
});

populateServices();
seedAvailability();
renderCalendar();
