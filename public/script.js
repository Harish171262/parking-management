// ── Auth check ──
const token = localStorage.getItem('parkops_token');
const role  = localStorage.getItem('parkops_role');
const user  = localStorage.getItem('parkops_user');
if (!token) window.location.href = '/login.html';

document.getElementById('sideUser').textContent =
  (role === 'admin' ? '👑 ' : '👤 ') + (user || '');

function logout() {
  localStorage.clear();
  window.location.href = '/login.html';
}

// ── DOM refs ──
const lot          = document.getElementById('lot');
const statusDiv    = document.getElementById('status');
const logDiv       = document.getElementById('log');
const entryBarrier = document.getElementById('entryBarrier');
const exitBarrier  = document.getElementById('exitBarrier');

let slotPositions = {};
let car           = null;
let pendingExit   = null;
let allHistory    = [];

// ── Utilities ──
function wait(ms) { return new Promise(r => setTimeout(r, ms)); }

function log(msg) {
  const time = new Date().toLocaleTimeString();
  logDiv.innerHTML = `<span style="color:#FFC93C">[${time}]</span> ${msg}<br>${logDiv.innerHTML}`;
}

// ── Sidebar toggle ──
function toggleSidebar() {
  document.getElementById('sidebar').classList.toggle('collapsed');
}

// ── View switcher ──
function showView(name) {
  document.querySelectorAll('.view').forEach(v => v.style.display = 'none');
  document.querySelectorAll('.side-btn').forEach(b => b.classList.remove('active'));

  const viewMap = {
    dashboard: 'dashboardView',
    entry:     'entryView',
    exit:      'exitView',
    levels:    'levelsView',
    history:   'historyView',
    activity:  'activityView'
  };
  const el = document.getElementById(viewMap[name]);
  if (el) el.style.display = 'block';

  const btnIndex = ['dashboard','entry','exit','levels','history','activity'].indexOf(name);
  const btns = document.querySelectorAll('.side-btn');
  if (btns[btnIndex]) btns[btnIndex].classList.add('active');

  if (name === 'history') refreshHistory();
  if (name === 'levels')  refreshLevels();
  if (name === 'exit')    refreshParkedList();
  if (name === 'dashboard') refreshStatus();
}

// ── Status & slots ──
function layoutPosition(slot, indexInSize) {
  const startX = 16;
  if (slot.size === 'SMALL')  return { x: startX + indexInSize * 95, y: 14 };
  if (slot.size === 'MEDIUM') return { x: startX + indexInSize * 95, y: 90 };
  return { x: startX + indexInSize * 115, y: 166 };
}

async function safeFetch(url, options = {}) {
  try {
    const res = await fetch(url, options);
    if (!res.ok) throw new Error(`HTTP Error: ${res.status}`);
    return await res.json();
  } catch (err) {
    console.error(`API Fetch Error [${url}]:`, err);
    log(`⚠️ Network/Server Error communication error.`);
    return null;
  }
}

async function refreshStatus() {
  const data = await safeFetch('/api/status');
  if (!data || !data.slots) return;

  const counters = { SMALL: 0, MEDIUM: 0, LARGE: 0 };
  const level1Slots = data.slots.filter(s => s.level === 'LEVEL1');

  level1Slots.forEach(slot => {
    const pos = layoutPosition(slot, counters[slot.size]++);
    slotPositions[slot.id] = pos;

    let el = document.getElementById('slot-' + slot.id);
    if (!el) {
      el = document.createElement('div');
      el.id = 'slot-' + slot.id;
      el.style.left = pos.x + 'px';
      el.style.top  = pos.y + 'px';
      lot.appendChild(el);
    }
    el.className = 'slot ' + (slot.occupied ? 'occupied' : 'available');
    el.innerHTML  = `${slot.id}<span class="plate">${slot.occupied ? slot.plate : slot.size}</span>`;
  });

  const counts = {};
  data.slots.forEach(s => {
    if (!counts[s.size]) counts[s.size] = { total: 0, free: 0 };
    counts[s.size].total++;
    if (!s.occupied) counts[s.size].free++;
  });
  
  let summary = '';
  for (const sz of ['SMALL','MEDIUM','LARGE']) {
    if (counts[sz]) summary += `${sz} ${counts[sz].free}/${counts[sz].total}   `;
  }
  statusDiv.textContent = summary.trim();
  buildLevelSummary(data.slots);
}

function buildLevelSummary(slots) {
  const div = document.getElementById('levelSummary');
  if (!div) return;
  const levels = ['LEVEL1','LEVEL2','BASEMENT'];
  div.innerHTML = levels.map(lv => {
    const lvSlots = slots.filter(s => s.level === lv);
    const free = lvSlots.filter(s => !s.occupied).length;
    const total = lvSlots.length || 1;
    const pct = Math.round((1 - free/total) * 100);
    return `<div class="level-card">
      <div class="lv-name">${lv.replace('LEVEL','Level ')}</div>
      <div class="lv-stat">${free}/${total} free</div>
      <div class="lv-bar"><div class="lv-fill" style="width:${pct}%"></div></div>
    </div>`;
  }).join('');
}

async function refreshLevels() {
  const data = await safeFetch('/api/status');
  if (!data || !data.slots) return;

  const container = document.getElementById('levelsContainer');
  container.innerHTML = '';
  const levels = ['LEVEL1','LEVEL2','BASEMENT'];
  
  levels.forEach(lv => {
    const lvSlots = data.slots.filter(s => s.level === lv);
    const div = document.createElement('div');
    div.className = 'level-block';
    div.innerHTML = '<h3 class="level-heading">' + lv.replace('LEVEL','Level ') + '</h3><div class="level-slots">'
      + lvSlots.map(s =>
          `<div class="slot-card ${s.occupied ? 'occ' : 'avl'}">
            <b>${s.id}</b><br>
            <span>${s.size}</span><br>
            <span class="sc-plate">${s.occupied ? s.plate : '—'}</span>
          </div>`).join('')
      + '</div>';
    container.appendChild(div);
  });
}

async function refreshParkedList() {
  const data = await safeFetch('/api/parked');
  if (!data || !data.vehicles) return;

  const sel  = document.getElementById('exitPlate');
  const cur  = sel.value;
  sel.innerHTML = '<option value="">Select vehicle…</option>';
  
  data.vehicles.forEach(v => {
    const opt = document.createElement('option');
    opt.value = v.plate;
    opt.textContent = `${v.plate} — ${v.slotId} (${v.level})`;
    sel.appendChild(opt);
  });
  if ([...sel.options].some(o => o.value === cur)) sel.value = cur;
}

async function refreshHistory() {
  const data = await safeFetch('/api/history');
  if (!data || !data.history) return;
  allHistory = data.history;
  renderHistory(allHistory);
}

function renderHistory(records) {
  const body = document.getElementById('historyBody');
  body.innerHTML = '';
  records.forEach(r => {
    const tr = document.createElement('tr');
    const fee     = r.exitTime > 0 ? '₹' + r.fee.toFixed(2)     : '—';
    const penalty = r.penalty  > 0 ? '₹' + r.penalty.toFixed(2)  : '—';
    const status  = r.exitTime > 0 ? '<span class="status-exited">EXITED</span>' : '<span class="status-parked">PARKED</span>';
    
    tr.innerHTML = `
      <td>${r.plate}</td><td>${r.ownerName}</td><td>${r.ownerPhone}</td>
      <td>${r.vehicleType}</td><td>${r.size}</td><td>${r.level}</td>
      <td>${r.slotId}</td><td>${r.entryFormatted}</td><td>${r.exitFormatted || '—'}</td>
      <td>${fee}</td><td>${penalty}</td><td>${r.paymentType}</td><td>${status}</td>
    `;
    body.appendChild(tr);
  });
}

function filterHistory() {
  const search = document.getElementById('searchPlate').value.toUpperCase();
  const status = document.getElementById('filterStatus').value;
  const filtered = allHistory.filter(r => {
    const matchPlate  = r.plate.includes(search);
    const matchStatus = !status || (status === 'PARKED' ? r.exitTime === 0 : r.exitTime > 0);
    return matchPlate && matchStatus;
  });
  renderHistory(filtered);
}

// ── Car animation helpers ──
function ensureCar() {
  if (!car) { 
    car = document.createElement('div'); 
    car.className = 'car'; 
    lot.appendChild(car); 
  }
  return car;
}

function moveCar(fx, fy, tx, ty) {
  return new Promise(resolve => {
    const c = ensureCar();
    c.style.display = 'block';
    c.style.transition = 'none';
    c.style.left = fx + 'px'; c.style.top = fy + 'px';
    void c.offsetWidth; // Force reflow
    c.style.transition = 'left 1s linear, top 1s linear';
    requestAnimationFrame(() => { c.style.left = tx + 'px'; c.style.top = ty + 'px'; });
    setTimeout(() => {
      c.style.display = 'none'; // Hide car smoothly upon arrival
      resolve();
    }, 1050);
  });
}

// ── Park Vehicle ──
async function parkVehicle() {
  const plate       = document.getElementById('entryPlate').value.trim().toUpperCase();
  const size        = document.getElementById('entrySize').value;
  const ownerName   = document.getElementById('entryOwnerName').value.trim();
  const ownerPhone  = document.getElementById('entryPhone').value.trim();
  const vehicleType = document.getElementById('entryVehicleType').value;
  const level       = document.getElementById('entryLevel').value;
  const msg         = document.getElementById('entryMsg');

  if (!plate || !ownerName || !ownerPhone) {
    msg.style.color = 'red';
    msg.textContent = '❌ Please fill Vehicle No, Owner Name & Phone!';
    return;
  }

  const body = 'plate='        + encodeURIComponent(plate)
             + '&size='        + size
             + '&ownerName='   + encodeURIComponent(ownerName)
             + '&ownerPhone='  + encodeURIComponent(ownerPhone)
             + '&vehicleType='+ encodeURIComponent(vehicleType)
             + '&level='      + encodeURIComponent(level);

  const result = await safeFetch('/api/entry', { 
    method: 'POST', 
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, 
    body 
  });

  if (!result || !result.success) {
    msg.style.color = 'red';
    msg.textContent = '❌ ' + (result ? result.message : 'Entry generation failed.');
    log(`ENTRY DENIED — ${plate}`);
    return;
  }

  msg.style.color = 'green';
  msg.textContent = '✅ ' + result.message;

  if (slotPositions[result.slotId]) {
    const pos = slotPositions[result.slotId];
    entryBarrier.classList.add('open');
    await wait(500);
    await moveCar(0, pos.y, pos.x, pos.y);
    entryBarrier.classList.remove('open');
  }

  log(`ENTRY — ${plate} | ${ownerName} | Slot ${result.slotId} | ${result.level}`);
  document.getElementById('entryPlate').value = '';
  document.getElementById('entryOwnerName').value = '';
  document.getElementById('entryPhone').value = '';
  
  await refreshStatus();
  await refreshParkedList();
}

// ── Generate Bill ──
async function generateBill() {
  const plate = document.getElementById('exitPlate').value;
  if (!plate) { alert('Please select a vehicle.'); return; }

  const result = await safeFetch(`/api/bill?plate=${encodeURIComponent(plate)}`);
  if (!result || !result.success) { alert(result ? result.message : 'Billing query error.'); return; }

  pendingExit = { plate, slotId: result.slotId, level: result.level };

  document.getElementById('billSlot').textContent    = result.slotId;
  document.getElementById('billLevel').textContent   = result.level;
  document.getElementById('billHours').textContent   = result.hours;
  document.getElementById('billFee').textContent     = result.fee.toFixed(2);
  document.getElementById('billTotal').textContent   = result.total.toFixed(2);

  const penaltyRow = document.getElementById('penaltyRow');
  if (result.penalty > 0) {
    document.getElementById('billPenalty').textContent = result.penalty.toFixed(2);
    penaltyRow.style.display = 'table-row';
  } else {
    penaltyRow.style.display = 'none';
  }

  const payType = document.getElementById('paymentType').value;
  const qrImg   = document.getElementById('billQr');
  if (payType === 'UPI') {
    const qrData = `upi://pay?pa=parkops@upi&pn=ParkOps&am=${result.total}&cu=INR&tn=${plate}`;
    qrImg.src = 'https://api.qrserver.com/v1/create-qr-code/?size=140x140&data=' + encodeURIComponent(qrData);
    qrImg.style.display = 'block';
  } else {
    qrImg.style.display = 'none';
  }

  document.getElementById('billPanel').style.display = 'block';
}

// ── Confirm Payment ──
async function confirmPayment() {
  if (!pendingExit) return;
  const { plate, slotId } = pendingExit;
  const paymentType = document.getElementById('paymentType').value;

  const result = await safeFetch('/api/exit', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: 'plate=' + encodeURIComponent(plate) + '&paymentType=' + paymentType
  });
  if (!result || !result.success) { alert(result ? result.message : 'Exit transaction failed.'); return; }

  if (slotPositions[slotId]) {
    const pos  = slotPositions[slotId];
    const exitX = lot.clientWidth ? (lot.clientWidth - 30) : 600;
    exitBarrier.classList.add('open');
    await wait(200);
    await moveCar(pos.x, pos.y, exitX, pos.y);
    exitBarrier.classList.remove('open');
  }

  log(`EXIT — ${plate} | ${result.hours}hr | ₹${result.fee}${result.penalty > 0 ? ' + Penalty ₹' + result.penalty : ''} | ${paymentType}`);
  document.getElementById('billPanel').style.display = 'none';
  pendingExit = null;
  
  await refreshStatus();
  await refreshParkedList();
}

// ── Boot ──
refreshStatus();
refreshParkedList();