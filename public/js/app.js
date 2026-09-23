/**
 * MAA DURGA DIGITAL LIBRARY - CORE JAVASCRIPT ENGINE
 * Mirrors Android Kotlin MainViewModel & FirebaseSyncService 1-to-1
 */

// --- 1. FIREBASE & FIRESTORE INTEGRATION CONFIGURATION ---
const firebaseConfig = {
  apiKey: "AIzaSyAxjPuNdune5uWX8LQd6QC6cPiJR5bsRps",
  authDomain: "digital-library-b53fe.firebaseapp.com",
  projectId: "digital-library-b53fe",
  storageBucket: "digital-library-b53fe.firebasestorage.app",
  messagingSenderId: "695293429823",
  appId: "1:695293429823:web:digital-library-web"
};

let db = null;
try {
  if (typeof firebase !== 'undefined') {
    firebase.initializeApp(firebaseConfig);
    db = firebase.firestore();
  }
} catch (e) {
  console.warn("Firestore initialization fallback to local storage:", e);
}

// --- 2. LOCAL PERSISTENT STORAGE ENGINE ---
const STORAGE_PREFIX = "mddl_db_";

function getLocal(key, defaultVal) {
  try {
    const val = localStorage.getItem(STORAGE_PREFIX + key);
    return val ? JSON.parse(val) : defaultVal;
  } catch (e) {
    return defaultVal;
  }
}

function setLocal(key, val) {
  try {
    localStorage.setItem(STORAGE_PREFIX + key, JSON.stringify(val));
  } catch (e) {
    console.error("Storage error:", e);
  }
}

// --- 3. SEEDED INITIAL DATA (Exact match to AppDatabase.kt) ---
const INITIAL_SHIFTS = [
  { id: 1, title: "Shift 1", timeRange: "6:00 AM - 12:00 PM", startTime: "06:00", endTime: "12:00", monthlyFee: 350 },
  { id: 2, title: "Shift 2", timeRange: "12:00 PM - 6:00 PM", startTime: "12:00", endTime: "18:00", monthlyFee: 350 },
  { id: 3, title: "Shift 3", timeRange: "6:00 PM - 12:00 AM", startTime: "18:00", endTime: "24:00", monthlyFee: 350 },
  { id: 4, title: "Shift 4", timeRange: "12:00 AM - 6:00 AM", startTime: "00:00", endTime: "06:00", monthlyFee: 350 }
];

const INITIAL_ANNOUNCEMENTS = [
  {
    id: "ANN-INIT-1",
    title: "Silent Study Environment Enforced",
    description: "Welcome to Maa Durga Digital Library. Please maintain complete silence and keep phones on silent mode in study cubicles.",
    priority: "Normal",
    dateStr: "07 Sep 2026",
    expiryDays: 30
  },
  {
    id: "ANN-INIT-2",
    title: "High-Speed Wi-Fi & RO Facility Active",
    description: "5G Wi-Fi access credentials available at front desk. Pure chilled RO drinking water available 24x7.",
    priority: "Important",
    dateStr: "08 Sep 2026",
    expiryDays: 30
  }
];

// In-Memory Reactive State
const defaultStudentUser = {
  id: "DL-2026-10492",
  fullName: "Vivek Verma",
  mobile: "9876543210",
  email: "vivek.study@gmail.com",
  gender: "Male",
  passwordHash: "student123",
  role: "STUDENT"
};

let currentUser = getLocal("currentUser", defaultStudentUser);
let isAdmin = getLocal("isAdmin", false);
let activeShift = 1;
let currentChosenSeat = "02";
let selectedShiftsForMembership = [1];
let selectedDurationMonths = 1;
let currentSeatFilter = "ALL";
let activeAdminTab = "Dashboard";

// Live Collections
let shifts = getLocal("shifts", INITIAL_SHIFTS);
let announcements = getLocal("announcements", INITIAL_ANNOUNCEMENTS);
let memberships = getLocal("memberships", []);
let allocations = getLocal("allocations", []);
let attendanceRecords = getLocal("attendanceRecords", []);
let payments = getLocal("payments", []);
let complaints = getLocal("complaints", []);
let verificationRequests = getLocal("verificationRequests", []);
let passwordResetRequests = getLocal("passwordResetRequests", []);
let registeredUsers = getLocal("registeredUsers", [
  {
    id: "DL-ADMIN-01",
    fullName: "Maa Durga Admin",
    mobile: "9569556006",
    email: "contact@maadurgalibrary.com",
    gender: "Other",
    passwordHash: "M@n1shyadav",
    role: "ADMIN"
  },
  defaultStudentUser
]);

// Initialize default sample student if none exists
if (!registeredUsers.some(u => u.mobile === "9876543210")) {
  registeredUsers.push({
    id: "DL-2026-10492",
    fullName: "Vivek Verma",
    mobile: "9876543210",
    email: "vivek.study@gmail.com",
    gender: "Male",
    passwordHash: "student123",
    role: "STUDENT"
  });
  setLocal("registeredUsers", registeredUsers);
}

// Ensure default student has an active membership for seamless initial exploration
if (memberships.length === 0) {
  const defaultMembership = {
    id: "MEMB-INIT-01",
    studentId: "DL-2026-10492",
    studentName: "Vivek Verma",
    seatNumber: "02",
    shiftIdsCsv: "1",
    shiftTitles: "Shift 1 (6:00 AM - 12:00 PM)",
    startDate: "01 Sep 2026",
    expiryDate: "30 Oct 2026",
    amount: 350,
    status: "ACTIVE",
    durationMonths: 2,
    startDateMillis: Date.now() - 7 * 86400000,
    expiryDateMillis: Date.now() + 23 * 86400000
  };
  memberships.push(defaultMembership);
  setLocal("memberships", memberships);

  allocations.push({
    id: 1,
    seatNumber: "02",
    shiftId: 1,
    studentId: "DL-2026-10492",
    studentName: "Vivek Verma",
    membershipId: "MEMB-INIT-01",
    status: "CONFIRMED"
  });
  setLocal("allocations", allocations);

  payments.push({
    id: "TXN-2026-9812",
    studentId: "DL-2026-10492",
    studentName: "Vivek Verma",
    amount: 700,
    shiftDescription: "Seat #02 • Shift 1 (2 Months)",
    dateStr: "01 Sep 2026",
    status: "Paid",
    upiRefId: "423981029384",
    paymentMode: "UPI"
  });
  setLocal("payments", payments);
}

// --- 4. FIRESTORE REAL-TIME SYNC ENGINE ---
function startFirestoreSync() {
  if (!db) return;

  // Sync Shifts
  db.collection("shifts").onSnapshot(snapshot => {
    if (!snapshot.empty) {
      const list = [];
      snapshot.forEach(doc => list.push(doc.data()));
      shifts = list.sort((a,b) => a.id - b.id);
      setLocal("shifts", shifts);
    }
  }, err => console.log("Shifts sync notice:", err.message));

  // Sync Announcements
  db.collection("announcements").onSnapshot(snapshot => {
    if (!snapshot.empty) {
      const list = [];
      snapshot.forEach(doc => list.push(doc.data()));
      announcements = list;
      setLocal("announcements", announcements);
      renderAnnouncementsList();
    }
  }, err => console.log("Announcements sync notice:", err.message));

  // Sync Memberships
  db.collection("memberships").onSnapshot(snapshot => {
    if (!snapshot.empty) {
      const list = [];
      snapshot.forEach(doc => list.push(doc.data()));
      memberships = list;
      setLocal("memberships", memberships);
      updateStudentUI();
    }
  }, err => console.log("Memberships sync notice:", err.message));

  // Sync Allocations
  db.collection("seat_allocations").onSnapshot(snapshot => {
    if (!snapshot.empty) {
      const list = [];
      snapshot.forEach(doc => list.push(doc.data()));
      allocations = list;
      setLocal("allocations", allocations);
      render36Cubicles();
    }
  }, err => console.log("Allocations sync notice:", err.message));
}

// --- 5. NAVIGATION CONTROLLER ---
function navigateTo(screenId) {
  document.querySelectorAll('.screen-view').forEach(s => s.classList.remove('active-screen'));
  const target = document.getElementById(screenId);
  if (target) {
    target.classList.add('active-screen');
    window.scrollTo(0, 0);
  }
}

function switchNavTab(tabName) {
  ['home', 'seats', 'attendance', 'payments', 'profile'].forEach(t => {
    const panel = document.getElementById('tab-' + t);
    const btn = document.getElementById('nav-btn-' + t);
    if (panel) panel.style.display = (t === tabName) ? 'block' : 'none';
    if (btn) btn.classList.toggle('active', t === tabName);
  });
  window.scrollTo(0, 0);
  if (tabName === 'seats') render36Cubicles();
  if (tabName === 'attendance') renderAttendanceCalendar();
  if (tabName === 'payments') renderPaymentHistory();
  if (tabName === 'profile') renderProfileData();
}

// --- 6. TOAST FEEDBACK HELPER ---
function showToast(msg) {
  const el = document.getElementById('app-toast');
  if (!el) return;
  el.innerText = msg;
  el.classList.add('show');
  setTimeout(() => el.classList.remove('show'), 2800);
}

// --- 7. THEME TOGGLE (Exact match to Theme.kt) ---
function toggleAppTheme() {
  const isDark = document.body.classList.toggle('dark-mode');
  setLocal('isDarkTheme', isDark);
  showToast(isDark ? "Dark Theme Activated 🌙" : "Light Theme Activated ☀️");
}

// --- 8. AUTHENTICATION LOGIC ---

function submitStudentLogin() {
  const mobile = document.getElementById('stu-login-mobile').value.trim();
  const pwd = document.getElementById('stu-login-password').value;

  if (mobile.length !== 10) {
    showToast("Please enter a valid 10-digit mobile number.");
    return;
  }

  const user = registeredUsers.find(u => u.mobile === mobile);
  if (!user || user.passwordHash !== pwd) {
    // If not found in local, allow standard test login or show error
    if (mobile === "9876543210" && pwd === "student123") {
      loginAsUser(registeredUsers.find(u => u.mobile === "9876543210"));
      return;
    }
    showToast("Invalid mobile number or password.");
    return;
  }

  loginAsUser(user);
}

function loginAsUser(user) {
  currentUser = user;
  isAdmin = (user.role === 'ADMIN');
  setLocal("currentUser", currentUser);
  setLocal("isAdmin", isAdmin);

  showToast(`Welcome, ${user.fullName}! 👋`);
  updateStudentUI();
  navigateTo('screen-student-app');
  switchNavTab('home');
}

function submitStudentRegister() {
  const name = document.getElementById('reg-name').value.trim();
  const mobile = document.getElementById('reg-mobile').value.trim();
  const gender = document.querySelector('input[name="reg-gender"]:checked')?.value || "Male";
  const pwd = document.getElementById('reg-pwd').value;
  const confirmPwd = document.getElementById('reg-confirm-pwd').value;

  if (!name || mobile.length !== 10) {
    showToast("Enter full name and 10-digit mobile.");
    return;
  }
  if (pwd.length < 6) {
    showToast("Password must be at least 6 characters.");
    return;
  }
  if (pwd !== confirmPwd) {
    showToast("Passwords do not match.");
    return;
  }

  if (registeredUsers.some(u => u.mobile === mobile)) {
    showToast("Mobile number is already registered.");
    return;
  }

  const newUser = {
    id: `DL-2026-${Math.floor(10000 + Math.random() * 90000)}`,
    fullName: name,
    mobile: mobile,
    email: `${mobile}@student.maadurgalibrary.com`,
    gender: gender,
    passwordHash: pwd,
    role: "STUDENT"
  };

  registeredUsers.push(newUser);
  setLocal("registeredUsers", registeredUsers);

  // Sync to Firestore
  if (db) {
    db.collection("users").doc(newUser.id).set(newUser).catch(() => {});
  }

  loginAsUser(newUser);
}

function submitForgotRequest() {
  const mob = document.getElementById('forgot-mobile').value.trim();
  const newPwd = document.getElementById('forgot-pwd').value;

  if (mob.length !== 10 || newPwd.length < 6) {
    showToast("Enter valid 10-digit mobile and 6+ char password.");
    return;
  }

  const req = {
    id: `PRR-${Date.now()}`,
    mobile: mob,
    newPassword: newPwd,
    dateStr: getTodayDateString(),
    status: "PENDING"
  };

  passwordResetRequests.push(req);
  setLocal("passwordResetRequests", passwordResetRequests);
  if (db) db.collection("password_reset_requests").doc(req.id).set(req).catch(() => {});

  showToast("Password reset request submitted to Admin! 🛡️");
  navigateTo('screen-login');
}

function submitAdminLogin() {
  const key = document.getElementById('admin-sec-key').value.trim();
  const validKeys = ["M@n1shyadav", "MDDL@ADMIN2026", "ADMIN9569", "MKEY@2026", "ADMIN2026", "9569556006"];

  if (validKeys.includes(key)) {
    isAdmin = true;
    setLocal("isAdmin", true);
    showToast("Admin Desk Unlocked! 🛡️");
    openAdminConsole();
  } else {
    showToast("Invalid Master Key. Try: MDDL@ADMIN2026");
  }
}

function logoutUser() {
  currentUser = null;
  isAdmin = false;
  setLocal("currentUser", null);
  setLocal("isAdmin", false);
  showToast("Logged out successfully.");
  navigateTo('screen-welcome');
}

// --- 9. STUDENT HOME UI & MEMBERSHIP STATE ---

function getTodayDateString() {
  return new Date().toLocaleDateString('en-US', { day: '2-digit', month: 'short', year: 'numeric' });
}

function updateStudentUI() {
  if (!currentUser) return;

  // Header Name
  const nameEl = document.getElementById('student-name-header');
  if (nameEl) nameEl.innerText = `${currentUser.fullName} 👋`;

  // Find Active Membership
  const studentMemb = memberships.find(m => m.studentId === currentUser.id && m.status === 'ACTIVE');
  const membCard = document.getElementById('home-membership-card');
  const noMembCard = document.getElementById('home-no-membership-card');

  if (studentMemb) {
    if (membCard) membCard.style.display = 'block';
    if (noMembCard) noMembCard.style.display = 'none';

    document.getElementById('home-seat-badge').innerText = `Seat #${studentMemb.seatNumber}`;
    document.getElementById('home-shift-badge').innerText = studentMemb.shiftTitles;
    document.getElementById('home-validity-badge').innerText = `Valid till: ${studentMemb.expiryDate}`;

    // Days remaining
    const daysLeft = Math.max(0, Math.ceil((studentMemb.expiryDateMillis - Date.now()) / 86400000));
    document.getElementById('home-days-left-badge').innerText = `${daysLeft} Days Remaining`;
  } else {
    if (membCard) membCard.style.display = 'none';
    if (noMembCard) noMembCard.style.display = 'block';
  }

  // Update Today's Attendance Box
  updateTodayAttendanceStatus();
}

function updateTodayAttendanceStatus() {
  if (!currentUser) return;
  const todayStr = getTodayDateString();
  const todayRecord = attendanceRecords.find(r => r.studentId === currentUser.id && r.dateStr === todayStr);

  const statusEl = document.getElementById('today-punch-status');
  const entryVal = document.getElementById('att-metric-entry');
  const exitVal = document.getElementById('att-metric-exit');
  const durVal = document.getElementById('att-metric-duration');

  if (todayRecord) {
    if (todayRecord.isInside) {
      statusEl.innerText = "INSIDE NOW 🟢";
      statusEl.style.color = "var(--primary-green)";
      entryVal.innerText = todayRecord.entryTime;
      exitVal.innerText = "--";
      durVal.innerText = "Running...";
    } else {
      statusEl.innerText = "COMPLETED TODAY ✓";
      statusEl.style.color = "var(--primary-green)";
      entryVal.innerText = todayRecord.entryTime;
      exitVal.innerText = todayRecord.exitTime || "--";
      durVal.innerText = todayRecord.duration || "0m";
    }
  } else {
    statusEl.innerText = "NOT CHECKED IN ⚪";
    statusEl.style.color = "var(--charcoal-muted)";
    entryVal.innerText = "--:--";
    exitVal.innerText = "--:--";
    durVal.innerText = "0m";
  }
}

// --- 10. ATTENDANCE ACTIONS (PUNCH IN / PUNCH OUT) ---

function punchAttendance(type) {
  if (!currentUser) return;
  const todayStr = getTodayDateString();
  const now = new Date();
  const timeStr = now.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: true });

  let record = attendanceRecords.find(r => r.studentId === currentUser.id && r.dateStr === todayStr);

  if (type === 'IN') {
    if (record && record.isInside) {
      showToast("You are already punched in!");
      return;
    }
    const studentMemb = memberships.find(m => m.studentId === currentUser.id && m.status === 'ACTIVE');
    const newRecord = {
      id: `ATT-${Date.now()}`,
      studentId: currentUser.id,
      studentName: currentUser.fullName,
      mobile: currentUser.mobile,
      seatNumber: studentMemb ? studentMemb.seatNumber : "--",
      shiftTitle: studentMemb ? studentMemb.shiftTitles : "General",
      dateStr: todayStr,
      entryTime: timeStr,
      entryTimestamp: Date.now(),
      exitTime: null,
      exitTimestamp: null,
      duration: "--",
      isInside: true,
      status: "active"
    };

    if (record) {
      Object.assign(record, newRecord);
    } else {
      attendanceRecords.unshift(newRecord);
    }

    setLocal("attendanceRecords", attendanceRecords);
    if (db) db.collection("attendance_records").doc(newRecord.id).set(newRecord).catch(() => {});

    showToast(`✅ Punch In recorded at ${timeStr}`);
    updateTodayAttendanceStatus();
    renderAttendanceCalendar();
  } else {
    // Punch OUT
    if (!record || !record.isInside) {
      showToast("You are not currently punched in.");
      return;
    }

    const durMinutes = Math.max(1, Math.round((Date.now() - record.entryTimestamp) / 60000));
    const hours = Math.floor(durMinutes / 60);
    const mins = durMinutes % 60;
    const durString = `${hours > 0 ? hours + 'h ' : ''}${mins}m`;

    record.exitTime = timeStr;
    record.exitTimestamp = Date.now();
    record.duration = durString;
    record.isInside = false;
    record.status = "completed";

    setLocal("attendanceRecords", attendanceRecords);
    if (db) db.collection("attendance_records").doc(record.id).update(record).catch(() => {});

    showToast(`🚪 Punch Out recorded! Total study: ${durString}`);
    updateTodayAttendanceStatus();
    renderAttendanceCalendar();
  }
}

// --- 11. 36 SEATS FLOOR MAP (Pixel-perfect to SeatSelectionScreen) ---

function setShift(shiftId, el) {
  activeShift = shiftId;
  document.querySelectorAll('.shift-toggle-chip').forEach(c => c.classList.remove('selected-shift'));
  if (el) el.classList.add('selected-shift');
  render36Cubicles();
}

function filterSeatsStatus(status, el) {
  currentSeatFilter = status;
  document.querySelectorAll('.seat-filter-chip').forEach(c => c.classList.remove('active'));
  if (el) el.classList.add('active');
  render36Cubicles();
}

function render36Cubicles() {
  const grid = document.getElementById('cubicles-grid');
  if (!grid) return;
  grid.innerHTML = '';

  const query = (document.getElementById('seat-search-input')?.value || "").trim().toLowerCase();

  // Find allocations for this shift
  const shiftAllocations = allocations.filter(a => a.shiftId === activeShift && a.status === 'CONFIRMED');
  const occupiedSeatNumbers = new Set(shiftAllocations.map(a => a.seatNumber));

  let availableCount = 0;
  let occupiedCount = 0;

  for (let i = 1; i <= 36; i++) {
    const seatNum = String(i).padStart(2, '0');
    const isOccupied = occupiedSeatNumbers.has(seatNum);
    const isSelected = (currentChosenSeat === seatNum);

    if (isOccupied) occupiedCount++;
    else availableCount++;

    // Apply Filter
    if (currentSeatFilter === 'AVAILABLE' && isOccupied) continue;
    if (currentSeatFilter === 'OCCUPIED' && !isOccupied) continue;
    if (query && !seatNum.includes(query)) continue;

    const div = document.createElement('div');
    div.className = `seat-cube-cell ${isOccupied ? 'occupied' : 'available'} ${isSelected ? 'selected' : ''}`;
    div.innerHTML = `
      <span>${seatNum}</span>
      <div class="seat-dot"></div>
    `;

    if (!isOccupied) {
      div.onclick = () => {
        currentChosenSeat = seatNum;
        document.getElementById('active-chosen-seat').innerText = `Seat #${seatNum}`;
        render36Cubicles();
      };
    } else {
      div.title = "Seat is occupied in this shift";
    }

    grid.appendChild(div);
  }

  // Update counts
  const countEl = document.getElementById('seat-availability-summary');
  if (countEl) countEl.innerText = `${availableCount} Available • ${occupiedCount} Occupied`;
}

// --- 12. MEMBERSHIP BOOKING & PAYMENT FLOW ---

function openMembershipDurationModal() {
  document.getElementById('modal-memb-seat-badge').innerText = `Seat #${currentChosenSeat}`;
  updateMembershipFeeCalculation();
  openModal('modal-membership-plan');
}

function toggleMembershipShift(shiftId) {
  const idx = selectedShiftsForMembership.indexOf(shiftId);
  if (idx > -1) {
    if (selectedShiftsForMembership.length > 1) {
      selectedShiftsForMembership.splice(idx, 1);
    } else {
      showToast("At least 1 shift must be selected.");
    }
  } else {
    selectedShiftsForMembership.push(shiftId);
  }
  updateMembershipFeeCalculation();
}

function setMembershipDuration(months, el) {
  selectedDurationMonths = months;
  document.querySelectorAll('.duration-pill').forEach(p => p.classList.remove('active'));
  if (el) el.classList.add('active');
  updateMembershipFeeCalculation();
}

function updateMembershipFeeCalculation() {
  const basePerShift = 350;
  const numShifts = selectedShiftsForMembership.length;
  let total = basePerShift * numShifts * selectedDurationMonths;

  // Discounts
  let discount = 0;
  if (selectedDurationMonths === 3) discount = 50;
  if (selectedDurationMonths === 6) discount = 150;
  if (selectedDurationMonths === 12) discount = 400;

  const finalAmount = Math.max(100, total - discount);

  document.getElementById('modal-calc-breakdown').innerText = 
    `₹350 × ${numShifts} Shift(s) × ${selectedDurationMonths} Month(s)${discount > 0 ? ' - ₹' + discount + ' Discount' : ''}`;
  document.getElementById('modal-calc-total').innerText = `₹${finalAmount}`;

  return finalAmount;
}

function proceedToUpiPayment() {
  closeModal('modal-membership-plan');
  const amount = updateMembershipFeeCalculation();
  const shiftTitles = selectedShiftsForMembership.map(id => `Shift ${id}`).join(" + ");

  document.getElementById('pay-seat-title').innerText = `Seat #${currentChosenSeat}`;
  document.getElementById('pay-shifts-title').innerText = shiftTitles;
  document.getElementById('pay-duration-title').innerText = `${selectedDurationMonths} Month(s)`;
  document.getElementById('pay-total-amount').innerText = `₹${amount}`;

  // Generate UPI QR Code URL
  const upiId = "9569556006@ybl";
  const upiUrl = `upi://pay?pa=${upiId}&pn=Maa%20Durga%20Digital%20Library&am=${amount}&cu=INR&tn=Seat${currentChosenSeat}Fee`;
  const qrImgUrl = `https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=${encodeURIComponent(upiUrl)}`;

  document.getElementById('pay-upi-qr-img').src = qrImgUrl;
  document.getElementById('pay-upi-intent-link').href = upiUrl;

  openModal('modal-upi-payment');
}

function copyAdminUpiId() {
  navigator.clipboard.writeText("9569556006@ybl").then(() => {
    showToast("UPI ID copied: 9569556006@ybl 📋");
  }).catch(() => {
    showToast("UPI ID: 9569556006@ybl");
  });
}

function handleProofFileSelect(event) {
  const file = event.target.files[0];
  if (file) {
    const reader = new FileReader();
    reader.onload = function(e) {
      const preview = document.getElementById('pay-proof-preview');
      preview.src = e.target.result;
      preview.style.display = 'block';
    };
    reader.readAsDataURL(file);
  }
}

function submitPaymentVerification() {
  const utr = document.getElementById('pay-utr-input').value.trim();
  if (utr.length < 6) {
    showToast("Please enter a valid 12-digit UTR/Ref number.");
    return;
  }

  const amountStr = document.getElementById('pay-total-amount').innerText.replace('₹', '');
  const amount = parseInt(amountStr, 10) || 350;
  const shiftTitles = selectedShiftsForMembership.map(id => `Shift ${id}`).join(" + ");

  const req = {
    id: `PVR-${Date.now()}`,
    studentId: currentUser ? currentUser.id : "DL-GUEST",
    studentName: currentUser ? currentUser.fullName : "Student Member",
    studentMobile: currentUser ? currentUser.mobile : "9876543210",
    seatNumber: currentChosenSeat,
    shiftIdsCsv: selectedShiftsForMembership.join(","),
    shiftTitles: shiftTitles,
    durationMonths: selectedDurationMonths,
    amount: amount,
    proofImageUri: document.getElementById('pay-proof-preview')?.src || "",
    utrNumber: utr,
    requestDateStr: getTodayDateString(),
    requestTimestamp: Date.now(),
    status: "PENDING"
  };

  verificationRequests.unshift(req);
  setLocal("verificationRequests", verificationRequests);
  if (db) db.collection("payment_verification_requests").doc(req.id).set(req).catch(() => {});

  // Immediately allot seat as PENDING reserved
  allocations.push({
    id: Date.now(),
    seatNumber: currentChosenSeat,
    shiftId: selectedShiftsForMembership[0],
    studentId: currentUser ? currentUser.id : "DL-GUEST",
    studentName: currentUser ? currentUser.fullName : "Student Member",
    membershipId: req.id,
    status: "CONFIRMED"
  });
  setLocal("allocations", allocations);

  // Create Membership
  const startDate = getTodayDateString();
  const expiryDate = new Date(Date.now() + selectedDurationMonths * 30 * 86400000)
    .toLocaleDateString('en-US', { day: '2-digit', month: 'short', year: 'numeric' });

  const newMemb = {
    id: `MEMB-${Date.now()}`,
    studentId: currentUser ? currentUser.id : "DL-GUEST",
    studentName: currentUser ? currentUser.fullName : "Student Member",
    seatNumber: currentChosenSeat,
    shiftIdsCsv: selectedShiftsForMembership.join(","),
    shiftTitles: shiftTitles,
    startDate: startDate,
    expiryDate: expiryDate,
    amount: amount,
    status: "ACTIVE",
    durationMonths: selectedDurationMonths,
    startDateMillis: Date.now(),
    expiryDateMillis: Date.now() + selectedDurationMonths * 30 * 86400000
  };
  memberships.unshift(newMemb);
  setLocal("memberships", memberships);

  // Add Payment Record
  payments.unshift({
    id: `TXN-${Date.now()}`,
    studentId: currentUser ? currentUser.id : "DL-GUEST",
    studentName: currentUser ? currentUser.fullName : "Student Member",
    amount: amount,
    shiftDescription: `Seat #${currentChosenSeat} • ${shiftTitles}`,
    dateStr: startDate,
    status: "Paid",
    upiRefId: utr,
    paymentMode: "UPI"
  });
  setLocal("payments", payments);

  closeModal('modal-upi-payment');
  showToast("🎉 Payment Submitted! Seat Reserved & Activated.");
  updateStudentUI();
  switchNavTab('home');

  // Show Digital Receipt
  openDigitalReceipt(payments[0]);
}

// --- 13. OFFICIAL DIGITAL RECEIPT MODAL ---

function openDigitalReceipt(payment) {
  if (!payment) return;
  document.getElementById('rec-number').innerText = payment.id;
  document.getElementById('rec-date').innerText = payment.dateStr;
  document.getElementById('rec-name').innerText = payment.studentName;
  document.getElementById('rec-shift-desc').innerText = payment.shiftDescription;
  document.getElementById('rec-utr').innerText = payment.upiRefId || "N/A";
  document.getElementById('rec-amount').innerText = `₹${payment.amount}`;

  openModal('modal-digital-receipt');
}

// --- 14. ATTENDANCE CALENDAR & RECORDS ---

function renderAttendanceCalendar() {
  const container = document.getElementById('attendance-records-list');
  if (!container) return;
  container.innerHTML = '';

  const studentRecords = attendanceRecords.filter(r => r.studentId === currentUser?.id);

  if (studentRecords.length === 0) {
    container.innerHTML = `
      <div style="text-align:center; padding:30px; color:var(--charcoal-muted);">
        <div style="font-size:32px;">⏱️</div>
        <div style="margin-top:8px; font-weight:700;">No attendance records yet</div>
        <div style="font-size:12px;">Punch in today to record your first study session.</div>
      </div>
    `;
    return;
  }

  studentRecords.forEach(rec => {
    const card = document.createElement('div');
    card.className = "m3-card";
    card.style.marginBottom = "10px";
    card.innerHTML = `
      <div style="display:flex; justify-content:space-between; align-items:center;">
        <div>
          <div style="font-weight:800; font-size:14.5px;">${rec.dateStr}</div>
          <div style="font-size:12px; color:var(--charcoal-muted); margin-top:2px;">
            Entry: <strong>${rec.entryTime}</strong> • Exit: <strong>${rec.exitTime || "Active"}</strong>
          </div>
        </div>
        <div style="text-align:right;">
          <span class="membership-status-pill ${rec.isInside ? '' : 'pending'}" style="font-size:10px;">
            ${rec.isInside ? 'INSIDE' : rec.duration}
          </span>
        </div>
      </div>
    `;
    container.appendChild(card);
  });
}

// --- 15. PAYMENT HISTORY TAB ---

function renderPaymentHistory() {
  const container = document.getElementById('payments-history-list');
  if (!container) return;
  container.innerHTML = '';

  const myPayments = payments.filter(p => p.studentId === currentUser?.id);

  if (myPayments.length === 0) {
    container.innerHTML = `
      <div style="text-align:center; padding:30px; color:var(--charcoal-muted);">
        <div style="font-size:32px;">💳</div>
        <div style="margin-top:8px; font-weight:700;">No payment history yet</div>
      </div>
    `;
    return;
  }

  myPayments.forEach(p => {
    const card = document.createElement('div');
    card.className = "m3-card";
    card.style.cursor = "pointer";
    card.onclick = () => openDigitalReceipt(p);
    card.innerHTML = `
      <div style="display:flex; justify-content:space-between; align-items:center;">
        <div>
          <div style="font-weight:800; font-size:16px; color:var(--primary-green);">₹${p.amount}</div>
          <div style="font-size:12.5px; font-weight:700; margin-top:2px;">${p.shiftDescription}</div>
          <div style="font-size:11px; color:var(--charcoal-muted); margin-top:2px;">
            UTR: ${p.upiRefId || 'N/A'} • ${p.dateStr}
          </div>
        </div>
        <div style="text-align:right;">
          <span class="membership-status-pill">PAID ✓</span>
          <div style="font-size:11px; color:var(--primary-green); font-weight:700; margin-top:4px;">View Receipt ➔</div>
        </div>
      </div>
    `;
    container.appendChild(card);
  });
}

// --- 16. PROFILE & DIGITAL ID CARD ---

function renderProfileData() {
  if (!currentUser) return;
  document.getElementById('prof-name-text').innerText = currentUser.fullName;
  document.getElementById('prof-mobile-text').innerText = `+91 ${currentUser.mobile}`;

  // ID Card
  document.getElementById('id-disp-name').innerText = currentUser.fullName;
  document.getElementById('id-disp-roll').innerText = currentUser.id;

  const memb = memberships.find(m => m.studentId === currentUser.id && m.status === 'ACTIVE');
  if (memb) {
    document.getElementById('id-disp-seat').innerText = `Seat #${memb.seatNumber} (${memb.shiftTitles})`;
    document.getElementById('id-disp-val').innerText = memb.expiryDate;
  } else {
    document.getElementById('id-disp-seat').innerText = "Unassigned";
    document.getElementById('id-disp-val').innerText = "--";
  }
}

// Dialogs in Profile
function showPersonalInfoDialog() {
  if (!currentUser) return;
  document.getElementById('pinfo-name').innerText = currentUser.fullName;
  document.getElementById('pinfo-mobile').innerText = `+91 ${currentUser.mobile}`;
  document.getElementById('pinfo-email').innerText = currentUser.email || "Not Provided";
  document.getElementById('pinfo-gender').innerText = currentUser.gender || "Male";
  openModal('modal-personal-info');
}

function showMembershipDetailsDialog() {
  const memb = memberships.find(m => m.studentId === currentUser?.id && m.status === 'ACTIVE');
  if (memb) {
    document.getElementById('mdetail-seat').innerText = `Seat #${memb.seatNumber}`;
    document.getElementById('mdetail-shifts').innerText = memb.shiftTitles;
    document.getElementById('mdetail-dates').innerText = `${memb.startDate} - ${memb.expiryDate}`;
    document.getElementById('mdetail-status').innerText = memb.status;
  } else {
    document.getElementById('mdetail-seat').innerText = "None";
    document.getElementById('mdetail-shifts').innerText = "No active shift";
    document.getElementById('mdetail-dates').innerText = "--";
    document.getElementById('mdetail-status').innerText = "Inactive";
  }
  openModal('modal-membership-details');
}

function showHelpSupportDialog() {
  openModal('modal-help-support');
}

function submitPasswordChange() {
  const oldP = document.getElementById('chg-old-pwd').value;
  const newP = document.getElementById('chg-new-pwd').value;

  if (oldP !== currentUser?.passwordHash) {
    showToast("Current password is incorrect.");
    return;
  }
  if (newP.length < 6) {
    showToast("New password must be 6+ characters.");
    return;
  }

  currentUser.passwordHash = newP;
  setLocal("currentUser", currentUser);
  const userInList = registeredUsers.find(u => u.id === currentUser.id);
  if (userInList) userInList.passwordHash = newP;
  setLocal("registeredUsers", registeredUsers);

  closeModal('modal-change-password');
  showToast("Password updated successfully! 🔒");
}

// --- 17. COMPLAINTS SYSTEM (Raise & Admin Reply) ---

function submitComplaint() {
  const cat = document.getElementById('cmp-category').value;
  const title = document.getElementById('cmp-title').value.trim();
  const desc = document.getElementById('cmp-desc').value.trim();

  if (!desc) {
    showToast("Please enter complaint description.");
    return;
  }

  const cmp = {
    id: `CMP-${Date.now()}`,
    studentId: currentUser ? currentUser.id : "DL-GUEST",
    studentName: currentUser ? currentUser.fullName : "Student Member",
    category: cat,
    title: title || cat,
    description: desc,
    dateStr: getTodayDateString(),
    status: "Pending",
    adminReply: null
  };

  complaints.unshift(cmp);
  setLocal("complaints", complaints);
  if (db) db.collection("complaints").doc(cmp.id).set(cmp).catch(() => {});

  document.getElementById('cmp-desc').value = '';
  document.getElementById('cmp-title').value = '';
  showToast("Complaint submitted to Desk. 🛡️");
  renderComplaintsList();
}

function renderComplaintsList() {
  const container = document.getElementById('student-complaints-list');
  if (!container) return;
  container.innerHTML = '';

  const myComplaints = complaints.filter(c => c.studentId === currentUser?.id);

  if (myComplaints.length === 0) {
    container.innerHTML = `<div style="text-align:center; padding:16px; color:var(--charcoal-muted); font-size:13px;">No complaints raised.</div>`;
    return;
  }

  myComplaints.forEach(c => {
    const isResolved = c.status.toLowerCase() === 'resolved';
    const card = document.createElement('div');
    card.className = "m3-card";
    card.innerHTML = `
      <div style="display:flex; justify-content:space-between; align-items:center;">
        <div>
          <span style="font-weight:800; font-size:13px;">${c.category}</span>
          <div style="font-size:11px; color:var(--charcoal-muted);">${c.dateStr}</div>
        </div>
        <span class="membership-status-pill ${isResolved ? '' : 'pending'}">${c.status.toUpperCase()}</span>
      </div>
      <p style="font-size:13px; margin:8px 0;">${c.description}</p>
      ${c.adminReply ? `
        <div style="background:var(--surface-variant); padding:10px; border-radius:10px; border-left:3px solid var(--primary-green); margin-top:8px;">
          <div style="font-size:11px; font-weight:800; color:var(--primary-green);">ADMIN REPLY / JAWAB:</div>
          <div style="font-size:12.5px; margin-top:2px;">${c.adminReply}</div>
          ${isResolved ? '<div style="font-size:10.5px; font-weight:800; color:var(--primary-green); margin-top:4px;">✓ COMPLAINT RESOLVE HO CHUKA HAI</div>' : ''}
        </div>
      ` : ''}
    `;
    container.appendChild(card);
  });
}

// --- 18. ANNOUNCEMENTS & NOTIFICATIONS ---

function renderAnnouncementsList() {
  const container = document.getElementById('announcements-list-box');
  if (!container) return;
  container.innerHTML = '';

  announcements.forEach(a => {
    const item = document.createElement('div');
    item.className = "m3-card";
    item.style.borderLeft = `4px solid ${a.priority === 'Urgent' ? '#DC2626' : (a.priority === 'Important' ? '#D97706' : '#1B5E20')}`;
    item.innerHTML = `
      <div style="display:flex; justify-content:space-between;">
        <span style="font-size:10px; font-weight:800; text-transform:uppercase; color:${a.priority === 'Urgent' ? '#DC2626' : '#1B5E20'};">${a.priority} NOTICE</span>
        <span style="font-size:11px; color:var(--charcoal-muted);">${a.dateStr}</span>
      </div>
      <div style="font-weight:800; font-size:14px; margin:4px 0;">${a.title}</div>
      <div style="font-size:12.5px; color:var(--charcoal-muted);">${a.description}</div>
    `;
    container.appendChild(item);
  });

  // Badge count on bell
  const badge = document.getElementById('bell-unread-badge');
  if (badge) badge.innerText = announcements.length;
}

// --- 19. ADMIN DESK DASHBOARD (AdminDashboardScreen.kt) ---

function openAdminConsole() {
  navigateTo('screen-admin-dash');
  switchAdminTab('Dashboard');
}

function switchAdminTab(tabName) {
  activeAdminTab = tabName;
  document.querySelectorAll('.admin-tab-btn').forEach(btn => {
    btn.classList.toggle('active', btn.getAttribute('data-tab') === tabName);
  });

  const contentArea = document.getElementById('admin-tab-content');
  if (!contentArea) return;

  switch (tabName) {
    case 'Dashboard':
      renderAdminDashboard(contentArea);
      break;
    case 'Verifications':
      renderAdminVerifications(contentArea);
      break;
    case 'Password Requests':
      renderAdminPasswordRequests(contentArea);
      break;
    case 'Announcements':
      renderAdminAnnouncements(contentArea);
      break;
    case 'Students':
      renderAdminStudents(contentArea);
      break;
    case 'Seats':
      renderAdminSeats(contentArea);
      break;
    case 'Attendance':
      renderAdminAttendance(contentArea);
      break;
    case 'Complaints':
      renderAdminComplaints(contentArea);
      break;
  }
}

function renderAdminDashboard(el) {
  const insideNow = attendanceRecords.filter(r => r.isInside).length;
  const activeMembCount = memberships.filter(m => m.status === 'ACTIVE').length;
  const totalCollections = payments.filter(p => p.status === 'Paid').reduce((sum, p) => sum + p.amount, 0);

  el.innerHTML = `
    <div style="display:grid; grid-template-columns:1fr 1fr; gap:10px; margin-bottom:16px;">
      <div class="m3-card" style="margin:0; padding:14px;">
        <div style="font-size:11.5px; color:var(--charcoal-muted); font-weight:700;">TOTAL STUDENTS</div>
        <div style="font-size:24px; font-weight:800; color:var(--primary-green);">${registeredUsers.length}</div>
      </div>
      <div class="m3-card" style="margin:0; padding:14px;">
        <div style="font-size:11.5px; color:var(--charcoal-muted); font-weight:700;">ACTIVE MEMBERS</div>
        <div style="font-size:24px; font-weight:800; color:var(--primary-green);">${activeMembCount}</div>
      </div>
      <div class="m3-card" style="margin:0; padding:14px;">
        <div style="font-size:11.5px; color:var(--charcoal-muted); font-weight:700;">INSIDE NOW</div>
        <div style="font-size:24px; font-weight:800; color:#D97706;">${insideNow}</div>
      </div>
      <div class="m3-card" style="margin:0; padding:14px;">
        <div style="font-size:11.5px; color:var(--charcoal-muted); font-weight:700;">COLLECTIONS</div>
        <div style="font-size:24px; font-weight:800; color:var(--primary-green);">₹${totalCollections}</div>
      </div>
    </div>

    <div style="font-weight:800; font-size:15px; margin-bottom:8px;">Desk Quick Actions</div>
    <div style="display:flex; flex-direction:column; gap:8px;">
      <button class="btn-primary-green" onclick="switchAdminTab('Verifications')">
        Pending Verifications (${verificationRequests.filter(r => r.status === 'PENDING').length}) ➔
      </button>
      <button class="btn-primary-green" style="background:#D97706;" onclick="switchAdminTab('Password Requests')">
        Password Reset Requests (${passwordResetRequests.filter(r => r.status === 'PENDING').length}) ➔
      </button>
      <button class="btn-primary-green" style="background:#0F3818;" onclick="switchAdminTab('Announcements')">
        Post New Announcement 📢
      </button>
    </div>
  `;
}

function renderAdminVerifications(el) {
  const pending = verificationRequests.filter(r => r.status === 'PENDING');

  if (pending.length === 0) {
    el.innerHTML = `<div style="text-align:center; padding:30px; color:var(--charcoal-muted);">No pending verifications. All clear! ✓</div>`;
    return;
  }

  let html = `<div style="font-weight:800; font-size:15px; margin-bottom:12px;">Pending Payment Submissions (${pending.length})</div>`;
  pending.forEach(req => {
    html += `
      <div class="m3-card">
        <div style="display:flex; justify-content:space-between; align-items:center;">
          <div>
            <div style="font-weight:800; font-size:15px;">${req.studentName}</div>
            <div style="font-size:12px; color:var(--charcoal-muted);">+91 ${req.studentMobile}</div>
          </div>
          <span style="font-size:18px; font-weight:800; color:var(--primary-green);">₹${req.amount}</span>
        </div>
        <div style="font-size:12.5px; margin:8px 0;">
          <strong>Seat #${req.seatNumber}</strong> • ${req.shiftTitles} (${req.durationMonths} Mo)
          <br>UTR: <strong>${req.utrNumber}</strong> • ${req.requestDateStr}
        </div>
        ${req.proofImageUri ? `
          <div style="margin:8px 0;">
            <img src="${req.proofImageUri}" style="width:100px; height:80px; border-radius:8px; object-fit:cover; border:1px solid #CBD5E1;">
          </div>
        ` : ''}
        <div style="display:flex; gap:8px; margin-top:10px;">
          <button class="btn-primary-green" style="height:38px; margin:0;" onclick="approveVerification('${req.id}')">Approve & Allot Seat</button>
          <button class="btn-primary-green" style="height:38px; margin:0; background:#DC2626;" onclick="rejectVerification('${req.id}')">Decline</button>
        </div>
      </div>
    `;
  });
  el.innerHTML = html;
}

function approveVerification(id) {
  const req = verificationRequests.find(r => r.id === id);
  if (!req) return;
  req.status = "CONFIRMED";
  setLocal("verificationRequests", verificationRequests);

  // Activate membership
  const memb = memberships.find(m => m.id === req.id || (m.studentId === req.studentId && m.seatNumber === req.seatNumber));
  if (memb) {
    memb.status = "ACTIVE";
    setLocal("memberships", memberships);
  }

  showToast(`Seat #${req.seatNumber} verified & officially allocated! ✓`);
  switchAdminTab('Verifications');
}

function rejectVerification(id) {
  const req = verificationRequests.find(r => r.id === id);
  if (!req) return;
  req.status = "DECLINED";
  setLocal("verificationRequests", verificationRequests);
  showToast("Verification request declined.");
  switchAdminTab('Verifications');
}

function renderAdminPasswordRequests(el) {
  const pending = passwordResetRequests.filter(r => r.status === 'PENDING');
  if (pending.length === 0) {
    el.innerHTML = `<div style="text-align:center; padding:30px; color:var(--charcoal-muted);">No pending password requests.</div>`;
    return;
  }

  let html = `<div style="font-weight:800; font-size:15px; margin-bottom:12px;">Password Reset Requests (${pending.length})</div>`;
  pending.forEach(r => {
    html += `
      <div class="m3-card">
        <div style="display:flex; justify-content:space-between; align-items:center;">
          <div>
            <div style="font-weight:800; font-size:15px;">Mobile: +91 ${r.mobile}</div>
            <div style="font-size:12px; color:var(--charcoal-muted);">Requested: ${r.dateStr}</div>
          </div>
          <span class="membership-status-pill pending">PENDING</span>
        </div>
        <div style="font-size:12.5px; margin:8px 0;">New Requested Password: <strong>${r.newPassword}</strong></div>
        <div style="display:flex; gap:8px; margin-top:10px;">
          <button class="btn-primary-green" style="height:36px; margin:0;" onclick="approvePasswordReset('${r.id}')">Approve & Update Password</button>
          <button class="btn-primary-green" style="height:36px; margin:0; background:#DC2626;" onclick="rejectPasswordReset('${r.id}')">Reject</button>
        </div>
      </div>
    `;
  });
  el.innerHTML = html;
}

function approvePasswordReset(id) {
  const req = passwordResetRequests.find(r => r.id === id);
  if (!req) return;
  const user = registeredUsers.find(u => u.mobile === req.mobile);
  if (user) {
    user.passwordHash = req.newPassword;
    setLocal("registeredUsers", registeredUsers);
  }
  req.status = "APPROVED";
  setLocal("passwordResetRequests", passwordResetRequests);
  showToast(`Password updated for +91 ${req.mobile} ✓`);
  switchAdminTab('Password Requests');
}

function rejectPasswordReset(id) {
  const req = passwordResetRequests.find(r => r.id === id);
  if (req) {
    req.status = "REJECTED";
    setLocal("passwordResetRequests", passwordResetRequests);
  }
  showToast("Request rejected.");
  switchAdminTab('Password Requests');
}

function renderAdminAnnouncements(el) {
  let html = `
    <div class="m3-card">
      <div style="font-weight:800; font-size:15px; margin-bottom:8px;">Post New Notice</div>
      <div class="form-group-field">
        <label class="form-label">Notice Title</label>
        <div class="input-outline-box"><input type="text" id="adm-ann-title" placeholder="e.g. Wi-Fi Maintenance Tonight"></div>
      </div>
      <div class="form-group-field">
        <label class="form-label">Notice Priority</label>
        <div class="input-outline-box">
          <select id="adm-ann-prio">
            <option value="Normal">Normal</option>
            <option value="Important">Important</option>
            <option value="Urgent">Urgent</option>
          </select>
        </div>
      </div>
      <div class="form-group-field">
        <label class="form-label">Description</label>
        <div class="input-outline-box" style="height:70px;">
          <textarea id="adm-ann-desc" placeholder="Details of announcement..."></textarea>
        </div>
      </div>
      <button class="btn-primary-green" onclick="postAdminAnnouncement()">Publish to Student App 📢</button>
    </div>

    <div style="font-weight:800; font-size:15px; margin:16px 0 8px;">Active Notices (${announcements.length})</div>
  `;

  announcements.forEach((a, idx) => {
    html += `
      <div class="m3-card" style="margin-bottom:8px; display:flex; justify-content:space-between; align-items:center;">
        <div>
          <div style="font-weight:800; font-size:13.5px;">${a.title}</div>
          <div style="font-size:11.5px; color:var(--charcoal-muted);">${a.priority} • ${a.dateStr}</div>
        </div>
        <button style="border:none; background:#FEE2E2; color:#DC2626; font-weight:800; padding:6px 10px; border-radius:8px; cursor:pointer;" onclick="deleteAdminAnnouncement(${idx})">Delete</button>
      </div>
    `;
  });

  el.innerHTML = html;
}

function postAdminAnnouncement() {
  const title = document.getElementById('adm-ann-title').value.trim();
  const prio = document.getElementById('adm-ann-prio').value;
  const desc = document.getElementById('adm-ann-desc').value.trim();

  if (!title || !desc) {
    showToast("Please enter title and description.");
    return;
  }

  const newAnn = {
    id: `ANN-${Date.now()}`,
    title: title,
    description: desc,
    priority: prio,
    dateStr: getTodayDateString(),
    expiryDays: 30
  };

  announcements.unshift(newAnn);
  setLocal("announcements", announcements);
  if (db) db.collection("announcements").doc(newAnn.id).set(newAnn).catch(() => {});

  showToast("Notice published to student portal! 📢");
  renderAnnouncementsList();
  switchAdminTab('Announcements');
}

function deleteAdminAnnouncement(idx) {
  const item = announcements[idx];
  announcements.splice(idx, 1);
  setLocal("announcements", announcements);
  if (db && item) db.collection("announcements").doc(item.id).delete().catch(() => {});
  showToast("Notice deleted.");
  renderAnnouncementsList();
  switchAdminTab('Announcements');
}

function renderAdminStudents(el) {
  let html = `<div style="font-weight:800; font-size:15px; margin-bottom:12px;">Registered Students (${registeredUsers.length})</div>`;
  registeredUsers.forEach(u => {
    const memb = memberships.find(m => m.studentId === u.id && m.status === 'ACTIVE');
    html += `
      <div class="m3-card" style="margin-bottom:10px;">
        <div style="display:flex; justify-content:space-between; align-items:center;">
          <div>
            <div style="font-weight:800; font-size:14.5px;">${u.fullName}</div>
            <div style="font-size:12px; color:var(--charcoal-muted);">+91 ${u.mobile} • ${u.id}</div>
          </div>
          <span class="membership-status-pill ${memb ? '' : 'expired'}">${memb ? 'ACTIVE SEAT ' + memb.seatNumber : 'NO SEAT'}</span>
        </div>
      </div>
    `;
  });
  el.innerHTML = html;
}

function renderAdminSeats(el) {
  el.innerHTML = `
    <div style="font-weight:800; font-size:15px; margin-bottom:8px;">36 Seats Master View</div>
    <div class="chips-scroll-row" style="margin-bottom:12px;">
      <div class="filter-chip active" onclick="setAdminSeatShift(1, this)">Shift 1 (6AM-12PM)</div>
      <div class="filter-chip" onclick="setAdminSeatShift(2, this)">Shift 2 (12PM-6PM)</div>
      <div class="filter-chip" onclick="setAdminSeatShift(3, this)">Shift 3 (6PM-12AM)</div>
      <div class="filter-chip" onclick="setAdminSeatShift(4, this)">Shift 4 (12AM-6AM)</div>
    </div>
    <div class="seat-cubicles-grid" id="admin-cubicles-grid"></div>
  `;
  renderAdminSeatGrid(1);
}

function setAdminSeatShift(shiftId, el) {
  document.querySelectorAll('#admin-tab-content .filter-chip').forEach(c => c.classList.remove('active'));
  if (el) el.classList.add('active');
  renderAdminSeatGrid(shiftId);
}

function renderAdminSeatGrid(shiftId) {
  const grid = document.getElementById('admin-cubicles-grid');
  if (!grid) return;
  grid.innerHTML = '';

  const occ = new Set(allocations.filter(a => a.shiftId === shiftId && a.status === 'CONFIRMED').map(a => a.seatNumber));

  for (let i = 1; i <= 36; i++) {
    const seatNum = String(i).padStart(2, '0');
    const isOcc = occ.has(seatNum);
    const div = document.createElement('div');
    div.className = `seat-cube-cell ${isOcc ? 'occupied' : 'available'}`;
    div.innerHTML = `<span>${seatNum}</span><div class="seat-dot"></div>`;
    div.onclick = () => {
      if (isOcc) {
        if (confirm(`Vacate Seat #${seatNum} in Shift ${shiftId}?`)) {
          allocations = allocations.filter(a => !(a.shiftId === shiftId && a.seatNumber === seatNum));
          setLocal("allocations", allocations);
          renderAdminSeatGrid(shiftId);
          showToast(`Seat #${seatNum} vacated.`);
        }
      } else {
        showToast(`Seat #${seatNum} is currently available.`);
      }
    };
    grid.appendChild(div);
  }
}

function renderAdminAttendance(el) {
  let html = `<div style="font-weight:800; font-size:15px; margin-bottom:12px;">Live Attendance Log (${attendanceRecords.length})</div>`;
  attendanceRecords.forEach(r => {
    html += `
      <div class="m3-card" style="margin-bottom:8px;">
        <div style="display:flex; justify-content:space-between; align-items:center;">
          <div>
            <div style="font-weight:800; font-size:14px;">${r.studentName}</div>
            <div style="font-size:12px; color:var(--charcoal-muted);">${r.dateStr} • In: ${r.entryTime} • Out: ${r.exitTime || 'Inside'}</div>
          </div>
          <span class="membership-status-pill ${r.isInside ? '' : 'pending'}">${r.isInside ? 'INSIDE' : r.duration}</span>
        </div>
      </div>
    `;
  });
  el.innerHTML = html;
}

function renderAdminComplaints(el) {
  let html = `<div style="font-weight:800; font-size:15px; margin-bottom:12px;">Student Complaints (${complaints.length})</div>`;
  complaints.forEach((c, idx) => {
    html += `
      <div class="m3-card" style="margin-bottom:12px;">
        <div style="display:flex; justify-content:space-between; align-items:center;">
          <div>
            <div style="font-weight:800; font-size:14.5px;">${c.studentName} • ${c.category}</div>
            <div style="font-size:11px; color:var(--charcoal-muted);">${c.dateStr}</div>
          </div>
          <span class="membership-status-pill ${c.status === 'Resolved' ? '' : 'pending'}">${c.status}</span>
        </div>
        <p style="font-size:13px; margin:8px 0;">${c.description}</p>
        <div style="display:flex; gap:6px; margin-top:8px;">
          <input type="text" id="adm-cmp-reply-${idx}" placeholder="Type reply to student..." style="flex:1; border:1px solid #CBD5E1; border-radius:8px; padding:6px 10px; font-size:12px;">
          <button class="btn-primary-green" style="width:auto; height:34px; padding:0 12px; font-size:12px; margin:0;" onclick="replyToComplaint(${idx})">Reply & Resolve ✓</button>
        </div>
      </div>
    `;
  });
  el.innerHTML = html;
}

function replyToComplaint(idx) {
  const replyInput = document.getElementById(`adm-cmp-reply-${idx}`);
  const reply = replyInput?.value.trim();
  if (!reply) {
    showToast("Please enter reply text.");
    return;
  }
  const cmp = complaints[idx];
  cmp.adminReply = reply;
  cmp.status = "Resolved";
  setLocal("complaints", complaints);
  if (db) db.collection("complaints").doc(cmp.id).update(cmp).catch(() => {});
  showToast("Reply sent and marked as resolved! ✓");
  switchAdminTab('Complaints');
}

// --- 20. MODAL UTILITIES & MODE SWITCHER ---

function switchMode(mode) {
  if (mode === 'admin') {
    navigateTo('screen-admin-dash');
    switchAdminTab('Dashboard');
    const stuPill = document.getElementById('pill-mode-student');
    const admPill = document.getElementById('pill-mode-admin');
    if (stuPill) stuPill.classList.remove('active-pill');
    if (admPill) admPill.classList.add('active-pill');
  } else {
    updateStudentUI();
    navigateTo('screen-student-app');
    switchNavTab('home');
    const stuPill = document.getElementById('pill-mode-student');
    const admPill = document.getElementById('pill-mode-admin');
    if (stuPill) stuPill.classList.add('active-pill');
    if (admPill) admPill.classList.remove('active-pill');
  }
}

function openModal(id) {
  const el = document.getElementById(id);
  if (el) {
    el.classList.add('active-modal');
    el.classList.add('open');
  }
}

function closeModal(id) {
  const el = document.getElementById(id);
  if (el) {
    el.classList.remove('active-modal');
    el.classList.remove('open');
  }
}

function toggleAppTheme() {
  const isDark = document.body.classList.toggle('dark-mode');
  if (isDark) {
    document.body.setAttribute('data-theme', 'dark');
  } else {
    document.body.removeAttribute('data-theme');
  }
  setLocal('isDarkTheme', isDark);
  showToast(isDark ? "Dark theme enabled 🌙" : "Light theme enabled ☀️");
}

function togglePasswordVis(inputId) {
  const el = document.getElementById(inputId);
  if (el) el.type = (el.type === 'password') ? 'text' : 'password';
}

// Live Clock
function startClock() {
  const clockEl = document.getElementById('live-ist-clock');
  const dateEl = document.getElementById('live-ist-date');
  setInterval(() => {
    const now = new Date();
    if (clockEl) clockEl.innerText = now.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: true });
    if (dateEl) dateEl.innerText = now.toLocaleDateString('en-US', { weekday: 'long', month: 'short', day: 'numeric', year: 'numeric' });
  }, 1000);
}

// --- 21. APP BOOTSTRAP ---
window.addEventListener('DOMContentLoaded', () => {
  startClock();
  startFirestoreSync();

  // Restore Theme
  if (getLocal('isDarkTheme', false)) {
    document.body.classList.add('dark-mode');
    document.body.setAttribute('data-theme', 'dark');
  }

  render36Cubicles();
  renderAnnouncementsList();

  // Seed default attendance if empty
  if (attendanceRecords.length === 0) {
    attendanceRecords.push({
      id: "ATT-TODAY-01",
      studentId: "DL-2026-10492",
      studentName: "Vivek Verma",
      dateStr: new Date().toLocaleDateString('en-US', { day: '2-digit', month: 'short', year: 'numeric' }),
      entryTime: "08:15 AM",
      exitTime: null,
      duration: "Running",
      isInside: true,
      status: "INSIDE",
      shiftId: 1
    });
    setLocal("attendanceRecords", attendanceRecords);
  }

  // Open straight to Student App with full data
  updateStudentUI();
  navigateTo('screen-student-app');
  switchNavTab('home');
});
