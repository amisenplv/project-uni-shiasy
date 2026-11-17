"use strict";

const app = document.querySelector("#app");
const overlay = document.querySelector("#overlay");
const scenarioNav = document.querySelector("#scenarios");
const params = new URLSearchParams(location.search);
if (params.has("capture")) document.documentElement.dataset.capture = "";
if (params.get("text") === "150") {
  document.documentElement.dataset.large = "";
  document.documentElement.style.fontSize = "150%";
}

const appearanceChoices = [["system","تنظیم گوشی"],["light","روشن"],["dark","تیره"]];
const systemDark = window.matchMedia("(prefers-color-scheme: dark)");
const appearanceStorageKey = "vehicle-ledger-appearance";
const validAppearance = value => appearanceChoices.some(([key]) => key === value);
let savedAppearance = null;
try { savedAppearance = localStorage.getItem(appearanceStorageKey); } catch { /* File storage may be unavailable. */ }
let appearance = [params.get("theme"), savedAppearance].find(validAppearance) || "system";

function applyAppearance() {
  const dark = appearance === "dark" || (appearance === "system" && systemDark.matches);
  document.documentElement.dataset.theme = dark ? "dark" : "light";
  document.querySelectorAll("[data-appearance]").forEach(select => { select.value = appearance; });
}

systemDark.addEventListener("change", applyAppearance);
document.addEventListener("change", event => {
  const control = event.target;
  if (!control.matches("[data-appearance]") || !validAppearance(control.value)) return;
  appearance = control.value;
  try { localStorage.setItem(appearanceStorageKey, appearance); } catch { /* The current session still applies it. */ }
  if (validAppearance(params.get("theme"))) {
    const url = new URL(location.href);
    url.searchParams.set("theme", appearance);
    history.replaceState(null, "", url);
  }
  applyAppearance();
});
applyAppearance();

// Local sample values for visual review. No device data is read or written.
const vehicles = {
  peugeot: { name: "پژو ۲۰۶", year: "۱۴۰۰ شمسی", mileage: 84620 },
  samand: { name: "سمند LX", year: "۱۳۹۷ شمسی", mileage: 121305 },
};
const categories = {
  fuel: ["سوخت", "fuel"], oil: ["تعویض روغن", "tool"], repair: ["تعمیر", "tool"],
  insurance: ["بیمه", "shield"], wash: ["کارواش", "drop"],
  parts: ["قطعات", "box"], other: ["سایر", "book"],
};
const entries = [
  { id: "fuel", day: 25, type: "fuel", amount: 240000, liters: 40, mileage: 84620 },
  { id: "repair", day: 23, type: "repair", amount: 4850000, note: "تعویض لنت جلو" },
  { id: "oil", day: 20, type: "oil", amount: 1180000, note: "روغن موتور و فیلتر" },
  { id: "wash", day: 18, type: "wash", amount: 120000 },
  { id: "fuel-early", day: 12, type: "fuel", amount: 180000, liters: 30 },
  { id: "parts", day: 9, type: "parts", amount: null, note: "فیلتر هوا" },
  { id: "wash-past", day: 15, month: "mehr", type: "wash", amount: 110000 },
];
const dueReminder = { title: "بازدید ترمز", day: 23 };
const nextMileage = 87000;

const scenarios = {
  home: "خانه", "home-multi": "خانه با چند خودرو", switch: "انتخاب خودرو",
  fuel: "ثبت سوخت", expense: "ثبت هزینه یا سرویس", validation: "خطا با صفحه‌کلید",
  history: "سوابق هزینه‌ها", edit: "ویرایش ثبت", delete: "تأیید حذف",
  report: "گزارش ماهانه", reminders: "یادآورها", "reminder-add": "ثبت یادآور",
  settings: "تنظیمات", backup: "پشتیبان‌گیری دستی", export: "ذخیره فایل",
  restore: "بازیابی از فایل", "restore-blocked": "بازیابی در دفتر پر",
  "restore-error": "خطای بازیابی", "restore-success": "بازیابی موفق",
  empty: "شروع بدون خودرو", "vehicle-add": "افزودن خودرو", "vehicle-edit": "ویرایش خودرو",
};

const icons = {
  back: '<path d="M5 12h14m-7-7 7 7-7 7"/>',
  next: '<path d="m14 6-6 6 6 6"/>',
  down: '<path d="m6 9 6 6 6-6"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  close: '<path d="m6 6 12 12M18 6 6 18"/>',
  check: '<path d="m5 12 4 4L19 6"/>',
  edit: '<path d="m14 6 4 4M4 20l4-1 12-12-4-4L4 15z"/>',
  car: '<path d="M5 17H3v-5l2-2 2-5h10l2 5 2 2v5h-2M5 17h14M5 17v2m14-2v2M5 10h14M7 14h1m8 0h1"/>',
  fuel: '<path d="M3 21h11M5 21V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v16M5 10h8m0 3h2a2 2 0 0 1 2 2v2a2 2 0 0 0 4 0v-7l-3-3m3 6h-2l-1-2"/>',
  book: '<path d="M5 4h13v16H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Zm2 0v16m3-12h5m-5 4h5m-5 4h3"/>',
  tool: '<path d="M14 6a4 4 0 0 0-5 5L3 17a2.1 2.1 0 0 0 3 3l6-6a4 4 0 0 0 5-5l-3 2-2-2z"/>',
  settings: '<path d="M4 7h16M4 17h16"/><circle cx="9" cy="7" r="2.5" fill="var(--paper)"/><circle cx="15" cy="17" r="2.5" fill="var(--paper)"/>',
  bell: '<path d="M18 8a6 6 0 0 0-12 0c0 6-3 7-3 9h18c0-2-3-3-3-9m-8 12a2 2 0 0 0 4 0"/>',
  chart: '<path d="M4 19h16M6 15v-3m6 3V5m6 10V9"/>',
  calendar: '<rect x="4" y="5" width="16" height="15" rx="2"/><path d="M8 3v4m8-4v4M4 10h16"/>',
  file: '<path d="M6 3h8l4 4v14H6zM14 3v5h4M9 13h6m-6 4h6"/>',
  export: '<path d="M4 15v5h16v-5M12 3v12m-4-4 4 4 4-4"/>',
  restore: '<path d="M4 15v5h16v-5M12 15V3M8 7l4-4 4 4"/>',
  shield: '<path d="m12 3 7 3v6c0 4-3 7-7 9-4-2-7-5-7-9V6z"/>',
  drop: '<path d="M12 3c-2 4-6 7-6 11a6 6 0 0 0 12 0c0-4-4-7-6-11z"/>',
  box: '<path d="m4 7 8-4 8 4v10l-8 4-8-4V7zm0 0 8 4 8-4m-8 4v10"/>',
  warning: '<path d="M12 4 3 20h18L12 4zm0 6v4m0 3h.01"/>',
};
const icon = name => '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' + icons[name] + '</svg>';
const tile = name => '<span class="tile-icon">' + icon(name) + '</span>';
const number = value => '<bdi dir="ltr">' + new Intl.NumberFormat("fa-IR").format(value) + '</bdi>';
const monthLabels = { aban:"آبان ۱۴۰۵", mehr:"مهر ۱۴۰۵" };
const belongsTo = (entry, period) => (entry.month || "aban") === period;
const dayLabel = entry => number(entry.day) + " " + monthLabels[entry.month || "aban"].split(" ")[0];
const date = (day, period = "aban") => (period === "aban" ? "۱۴۰۵/۰۸/" : "۱۴۰۵/۰۷/") + new Intl.NumberFormat("fa-IR", {minimumIntegerDigits: 2}).format(day);
const href = (route, label, className = "", glyph = "") =>
  `<a class="${className}" href="#${route}">${glyph ? icon(glyph) : ""}${label}</a>`;
const act = (action, label, className = "quiet", glyph = "") =>
  `<button class="${className}" type="button" data-action="${action}">${glyph ? icon(glyph) : ""}${label}</button>`;

let screen = "home";
let previous = "home";
let origin = "home";
let settingsOrigin = "home";
let selectedVehicle = "peugeot";
let vehicleEditId = "peugeot";
let multi = false;
let entryId = "fuel";
let month = "aban";
let draft;
let flash;
let modalTrigger;
let notificationState = "off";

function active() { return vehicles[selectedVehicle]; }
function activeEntries() { return selectedVehicle === "peugeot" ? entries : []; }

function header(title, { back, subject, tail, brand } = {}) {
  return `<header class="bar">
    ${back ? `<a href="#${back}" class="icon-control" aria-label="بازگشت">${icon("back")}</a>` : ""}
    ${brand ? `<span class="brand">${icon("book")}</span>` : ""}
    <div class="label"><h1 id="view-title" tabindex="-1">${title}</h1>${subject ? `<span class="meta"><bdi>${subject}</bdi></span>` : ""}</div>
    ${tail || ""}
  </header>`;
}

const view = (body, extra = "") => `<main class="view" aria-labelledby="view-title"><div class="content ${extra}">${body}</div></main>`;
const sticky = control => `<div class="sticky">${control}</div>`;

function linkRow(route, name, title, sub) {
  return `<a href="#${route}" class="row link">${tile(name)}<span class="grow"><strong>${title}</strong>${sub ? `<span class="meta">${sub}</span>` : ""}</span>${icon("next")}</a>`;
}

function notice(label, detail, tone = "", glyph = "") {
  return `<div class="notice ${tone}" role="status">${glyph ? icon(glyph) + " " : ""}<strong>${label}</strong>${detail ? `<p>${detail}</p>` : ""}</div>`;
}

function flashMessage() {
  if (!flash) return "";
  const message = flash;
  flash = null;
  return notice(message, "", "good", "check");
}

function home(empty = false) {
  const settings = '<a href="#settings" class="icon-control" aria-label="تنظیمات">' + icon("settings") + "</a>";
  const top = header("دفتر خودرو", { brand:true, tail:settings });
  if (empty) return top + view(`<section class="stack opening">
      ${tile("car")}<h2>اولین خودرو را اضافه کنید</h2>
      <p>هزینه‌ها، سرویس‌ها و کارکرد خودرو را در همین گوشی ثبت کنید.</p>
      ${href("vehicle-add","افزودن خودرو","action","plus")}
    </section>
    <section class="stack tight"><h3>قبلاً پشتیبان گرفته‌اید؟</h3>
    <div class="row-group">${linkRow("restore","restore","بازیابی از فایل","بازگرداندن اطلاعات از فایل پشتیبان")}</div></section>`);
  const car = active();
  const carTitle = `<span class="vehicle-identity"><span class="vehicle-icon">${icon("car")}</span><span class="vehicle-name"><bdi>${car.name}</bdi><span class="meta">مدل ${car.year}</span></span></span>`;
  const chooser = multi
    ? `<button type="button" class="vehicle-trigger" data-action="switch" aria-haspopup="dialog">${carTitle}${icon("down")}<span class="sr-only">، انتخاب خودرو</span></button>`
    : `<div class="between">${carTitle}<button type="button" class="icon-control" data-action="edit-car:${selectedVehicle}" aria-label="ویرایش خودرو">${icon("edit")}</button></div>`;
  const due = selectedVehicle === "peugeot"
    ? `<section class="stack tight"><h2>نوبت سرویس</h2>
       <a class="notice link service-due" href="#reminders">${icon("bell")}<span class="grow"><strong>${dueReminder.title}</strong>
       <span class="service-detail">موعد ${dayLabel({day:dueReminder.day})}</span></span>
       <span class="due-status">سررسیدشده</span></a></section>` : "";
  const recent = activeEntries().slice(0,2);
  return top + view(`
    ${flashMessage()}
    <section class="panel vehicle-panel" aria-label="خودرو و کارکرد">
      ${chooser}
      <div class="odometer">
        <p class="meta">آخرین کارکرد ثبت‌شده</p>
        <div class="reading"><strong class="big">${number(car.mileage)}</strong><span class="meta">کیلومتر</span></div>
      </div>
    </section>
    <div class="actions pair quick-entry" aria-label="ثبت سریع">
      ${href("fuel","ثبت سوخت","action","fuel")}${href("expense","ثبت هزینه","secondary","plus")}
    </div>
    ${due}
    <section class="stack tight">
      <div class="between wrap"><h2>${recent.length ? "ثبت‌های اخیر" : "سوابق هزینه‌ها"}</h2>
        ${href("history",recent.length ? "همه سوابق هزینه‌ها" : "نمایش سوابق هزینه‌ها","quiet recent-link")}</div>
      ${recent.length
        ? `<ul class="recent-list row-group">${recent.map(entry => `<li class="row">
          <span class="grow"><strong>${categories[entry.type][0]}</strong><span class="meta">${dayLabel(entry)}</span></span>
          ${entryAmount(entry)}</li>`).join("")}</ul>`
        : '<p class="meta">هنوز هزینه یا سرویسی ثبت نشده است.</p>'}
    </section>
    <div class="row-group home-destinations">
      ${linkRow("report","chart","گزارش ماهانه","")}
      ${linkRow("reminders","bell","یادآورها","")}
    </div>
    ${multi ? "" : href("vehicle-add","افزودن خودرو","quiet","plus")}
  `, "home-content");
}

function field(id, label, { value = "", placeholder = "", mode = "", error = "", help = "", large = false, area = false, required = false } = {}) {
  const described = error || help ? `aria-describedby="${id}-help"` : "";
  const attrs = `id="${id}" name="${id}" ${described} ${error ? 'aria-invalid="true"' : ""} ${required ? 'aria-required="true"' : ""}`;
  return `<div class="field ${large ? "leading-field" : ""}">
    <label for="${id}">${label}</label>
    ${area
      ? `<textarea ${attrs} dir="auto" placeholder="${placeholder}">${value}</textarea>`
      : `<input ${attrs} ${mode ? `inputmode="${mode}" dir="ltr"` : 'dir="auto"'} value="${value}" placeholder="${placeholder}" autocomplete="off">`}
    ${error || help ? `<small id="${id}-help" class="${error ? "field-error" : ""}">${error || help}</small>` : ""}
  </div>`;
}

function selectField(id, label, choices, selected) {
  return `<div class="field"><label for="${id}">${label}</label><select id="${id}" name="${id}">
    ${choices.map(([key, text]) => `<option value="${key}"${key === selected ? " selected" : ""}>${text}</option>`).join("")}
  </select></div>`;
}

function entryAmount(entry) {
  return `<span class="amount-block">${entry.amount === null ? '<small>بدون مبلغ</small>' : `<span class="amount">${number(entry.amount)}</span><small>تومان</small>`}</span>`;
}

function entryForm(kind) {
  const editing = kind === "edit" || kind === "delete";
  const invalid = kind === "validation";
  const example = entries.find(item => item.id === entryId) || entries[0];
  const type = draft?.type ?? (editing ? example.type : kind === "expense" ? "" : "fuel");
  const fuel = type === "fuel";
  const optionalAmount = ["oil","repair","parts"].includes(type);
  const amount = editing && example.amount != null ? numberPlain(example.amount) : invalid ? "۰" : "";
  const volume = editing && example.liters ? numberPlain(example.liters) : invalid ? "۴۰" : "";
  const when = editing ? date(example.day, example.month) : date(25);
  const title = editing ? "ویرایش ثبت" : fuel ? "ثبت سوخت" : "ثبت هزینه یا سرویس";
  const amountError = invalid ? "مبلغ باید بیشتر از صفر باشد و حداکثر یک رقم اعشار داشته باشد." : "";
  const pickCategory = (editing || !fuel) ? selectField("type", "دسته‌بندی",
    [["","انتخاب دسته‌بندی"], ...Object.entries(categories).filter(([key]) => editing || key !== "fuel").map(([key,[label]]) => [key,label])], type) : "";
  const mileageHelp = editing && example.mileage
    ? "اگر خالی بماند، کارکرد فعلی حفظ می‌شود."
    : `آخرین کارکرد ثبت‌شده ${number(active().mileage)} کیلومتر`;
  const amountField = field("amount", "مبلغ (تومان)" + (optionalAmount ? ' <span class="hint">اختیاری</span>' : ""), {
    value:amount, placeholder:"۲۴۰٬۰۰۰", mode:"decimal", large:true, error:amountError, required:!optionalAmount,
  });
  const volumeField = fuel ? field("volume","حجم سوخت (لیتر)",{value:volume,placeholder:"۴۰",mode:"decimal",required:true}) : "";
  const body = `<form id="entry-form" class="form-fields" novalidate>
    ${pickCategory}
    <div class="entry-values ${fuel && !invalid ? "form-grid" : ""}">${amountField}${volumeField}</div>
    ${field("date","تاریخ ثبت (شمسی)",{value:when,mode:"text",required:true})}
    <details ${editing && (example.mileage || example.note) ? "open" : ""}><summary>کارکرد و یادداشت <span class="meta">(اختیاری)</span></summary>
      <div class="stack">
      ${field("mileage","کارکرد خودرو (کیلومتر)",{value:editing && example.mileage ? numberPlain(example.mileage) : "",mode:"numeric",help:mileageHelp})}
      ${editing && example.mileage ? '<label class="inline"><input type="checkbox" name="removeMileage"> حذف کارکرد همراه ثبت</label>' : ""}
      ${field("notes","یادداشت",{area:true,value: editing ? example.note || "" : "",placeholder:"مثلاً تعویض فیلتر هوا"})}
      </div></details>
    ${editing ? act("delete", "حذف این ثبت", "quiet danger-link") : ""}
  </form>`;
  const keys = invalid ? '<div class="keypad" aria-hidden="true">' + "۱۲۳۴۵۶۷۸۹٫۰⌫".split("").map(key => "<span>" + key + "</span>").join("") + "</div>" : "";
  return header(title, { back:editing ? "history" : origin, subject:active().name }) +
    view(body) + sticky(act("save-entry",editing ? "ذخیره تغییرات" : fuel ? "ثبت سوخت" : "ثبت هزینه یا سرویس","action")) + keys;
}

function numberPlain(value) { return new Intl.NumberFormat("fa-IR",{useGrouping:false}).format(value); }

function notebookNav(on) {
  return `<nav class="segment" aria-label="دفتر این خودرو">
    <a href="#history" ${on === "history" ? 'aria-current="page"' : ""}>سوابق هزینه‌ها</a>
    <a href="#report" ${on === "report" ? 'aria-current="page"' : ""}>گزارش ماهانه</a>
  </nav>`;
}

function historyView() {
  const rows = activeEntries();
  const groups = Object.entries(monthLabels).map(([period,label]) => {
    const inMonth = rows.filter(entry => belongsTo(entry,period));
    if (!inMonth.length) return "";
    const days = [...new Set(inMonth.map(entry => entry.day))];
    const list = days.map(day => `
      <div class="ledger-day">
        <h3 class="ledger-date"><span aria-hidden="true">${number(day)}</span><span class="sr-only">${dayLabel({day,month:period})}</span></h3>
        <div class="ledger-records">${inMonth.filter(entry => entry.day === day).map(entry => {
          const [category] = categories[entry.type];
          const detail = entry.liters ? number(entry.liters) + " لیتر" : entry.note;
          return `<button type="button" class="row link" data-action="edit:${entry.id}">
            <span class="grow"><strong>${category}</strong>${detail ? `<span class="meta">${detail}</span>` : ""}</span>
            ${entryAmount(entry)}
          </button>`;
        }).join("")}</div>
      </div>`).join("");
    return `<section><h2 class="group-label">${label}</h2><div class="row-group">${list}</div></section>`;
  }).join("");
  return header("دفتر هزینه‌ها", { back:"home",subject:active().name }) + notebookNav("history") +
    view(flashMessage() + (rows.length ? groups :
      `<section class="stack"><h2>هنوز هزینه یا سرویسی ثبت نشده است.</h2><div class="actions pair">${href("fuel","ثبت سوخت","action")}${href("expense","ثبت هزینه","secondary")}</div></section>`), "history");
}

function reportView() {
  const rows = activeEntries().filter(entry => belongsTo(entry,month));
  const totals = rows.reduce((result, entry) => {
    if (entry.amount !== null) result[entry.type] = (result[entry.type] || 0) + entry.amount;
    return result;
  }, {});
  const total = Object.values(totals).reduce((sum, value) => sum + value, 0);
  const unpricedOnly = [...new Set(rows.filter(entry => !(entry.type in totals)).map(entry => entry.type))];
  const breakdown = Object.entries(totals).sort((a,b) => b[1] - a[1]).map(([type,value]) => `
    <div class="category-row"><div class="between"><strong>${categories[type][0]}</strong><span>${number(value)} <small>تومان</small></span></div>
    <div class="track" aria-hidden="true"><span style="--share:${(100*value/total).toFixed(2)}%"></span></div></div>`).join("");
  return header("دفتر هزینه‌ها", { back:"home",subject:active().name }) + notebookNav("report") + view(`
    <section class="report-slip">
    ${act("month",`<span class="inline">${icon("calendar")} ${monthLabels[month]}</span>${icon("down")}`,"secondary report-control")}
    ${rows.length ? `<div class="panel report-panel"><div><p class="meta">جمع هزینه‌های ثبت‌شده</p><div class="reading"><strong class="big">${number(total)}</strong><span class="meta">تومان</span></div></div>
    <hr class="panel-rule"><p class="meta"><span>${number(rows.length)} ثبت</span><span>${number(rows.filter(row => row.amount === null).length)} ثبت بدون مبلغ</span></p></div></section>
    <section><h2>هزینه‌ها به تفکیک دسته</h2><div class="category-group">${breakdown}
    ${unpricedOnly.map(type => `<div class="category-row"><div class="between"><strong>${categories[type][0]}</strong><small>بدون مبلغ</small></div></div>`).join("")}</div></section>` :
    '</section><section class="stack"><h2>در این ماه هزینه یا سرویسی ثبت نشده است.</h2><p class="meta">می‌توانید ماه دیگری را از فهرست انتخاب کنید.</p></section>'}
  `);
}

function reminderRow(title, detail, { state, action, remaining } = {}) {
  const copy = `<span class="grow"><strong>${title}</strong><span class="meta">${detail}</span>
    ${remaining ? `<span class="remaining">${remaining}</span>` : ""}</span>`;
  return action
    ? `<button type="button" class="row link reminder-row" data-action="${action}">${copy}${icon("next")}</button>`
    : `<div class="row reminder-row">${copy}<span class="badge">${state}</span></div>`;
}

function reminderView() {
  const content = selectedVehicle !== "peugeot" ? '<h2>هنوز یادآوری برای این خودرو ثبت نکرده‌اید.</h2>' : `
    <section class="reminder-group reminder-due"><h2>سررسیدشده</h2><div class="row-group">${reminderRow(dueReminder.title, "موعد " + dayLabel({day:dueReminder.day}) + " ۱۴۰۵", {action:"reminder-detail"})}</div></section>
    <section class="reminder-group"><h2>در انتظار موعد</h2><div class="row-group">${reminderRow("تعویض فیلتر هوا",
      "موعد در کارکرد " + number(nextMileage) + " کیلومتر",
      {action:"reminder-upcoming",remaining:number(nextMileage - active().mileage) + " کیلومتر تا موعد"})}</div></section>
    <section class="reminder-group"><h2>سوابق یادآورها</h2><div class="row-group">
    ${reminderRow("تعویض روغن", "موعد ۲۰ آبان ۱۴۰۵", {state:"انجام‌شده"})}
    ${reminderRow("بازدید لاستیک", "موعد ۱۵ آبان ۱۴۰۵", {state:"لغوشده"})}</div></section>`;
  return header("یادآورها",{back:"home",subject:active().name}) + view(flashMessage() + content) +
    sticky(href("reminder-add","ثبت یادآور","action","plus"));
}

function reminderForm() {
  const mileage = draft?.threshold === "mileage";
  return header("ثبت یادآور",{back:"reminders",subject:active().name}) + view(`<form id="reminder-form" class="form-fields">
    ${field("reminder-name","عنوان یادآور",{placeholder:"مثلاً تعویض روغن",required:true})}
    ${selectField("threshold","نوع یادآور",[["date","بر اساس تاریخ"],["mileage","بر اساس کارکرد"]],mileage?"mileage":"date")}
    ${mileage
      ? field("threshold-value","کارکرد سررسید (کیلومتر)",{mode:"numeric",placeholder:"مثلاً ۸۷۰۰۰",required:true})
      : field("threshold-value","تاریخ سررسید (شمسی)",{mode:"text",placeholder:"مثلاً ۱۴۰۵/۰۹/۱۵",required:true})}
    </form>`) + sticky(act("save-reminder","ثبت یادآور","action"));
}

function appearanceControl(id) {
  return `<div class="field"><label for="${id}">حالت نمایش</label>
    <select id="${id}" data-appearance>
      ${appearanceChoices.map(([key,label]) => `<option value="${key}"${key === appearance ? " selected" : ""}>${label}</option>`).join("")}
    </select></div>`;
}

function settings() {
  return header("تنظیمات",{back:settingsOrigin}) + view(`
    <section class="stack tight"><h2>ظاهر</h2>${appearanceControl("settings-appearance")}</section>
    <section class="stack tight"><h2>داده‌ها</h2><div class="row-group">
      ${linkRow("backup","file","پشتیبان‌گیری دستی","ذخیره یا بازیابی از فایل")}
    </div></section>
    <section class="stack tight"><h2>اعلان‌ها</h2><div class="row-group">
      ${linkRow("notifications","bell","اعلان یادآورهای تاریخ‌دار",notificationState === "on" ? "روشن" : "خاموش")}
    </div></section>`);
}

function notificationSettings() {
  return header("اعلان یادآورها",{back:"settings"}) + view(`
    <section class="stack"><h2>یادآورهای تاریخ‌دار</h2><p>اعلان‌ها ممکن است با تأخیر برسند. موعدهای رسیده را می‌توانید در خود دفتر خودرو ببینید.</p>
    ${act("toggle-notifications",notificationState === "off" ? "روشن کردن اعلان‌ها" : "خاموش کردن اعلان‌ها","action")}
    </section>`);
}

function backup() {
  return header("پشتیبان‌گیری دستی",{back:"settings"}) + view(`
    <section class="stack tight"><h2>فایل پشتیبان</h2><div class="row-group">
      ${linkRow("export","export","ذخیره فایل پشتیبان","از اطلاعات فعلی همه خودروها")}
      ${linkRow("restore","restore","بازیابی از فایل","فقط در دفتر کاملاً خالی")}
    </div></section>
    ${notice("پشتیبان‌گیری دستی است","برای نگه داشتن تغییرات بعدی، دوباره فایل بگیرید. فایل را جایی نگه دارید که بعد از تعویض گوشی در دسترس باشد.","","file")}
  `);
}

function backupAction(route) {
  const exporting = route.startsWith("export");
  const blocked = route === "restore-blocked";
  const restored = route === "restore-success";
  const invalid = route === "restore-error";
  const exported = route === "export-success";
  const title = exporting ? "ذخیره فایل پشتیبان" : "بازیابی از فایل";
  const fromEmpty = origin === "empty";
  const back = fromEmpty && !exporting ? "empty" : "backup";
  const feedback = blocked
    ? notice("بازیابی انجام نشد","دفتر خودرو باید کاملاً خالی باشد. اطلاعات فعلی شما حفظ شده است.","error","warning")
    : invalid ? notice("فایل معتبر نیست","اطلاعاتی بازیابی نشد. یک فایل پشتیبان دیگر انتخاب کنید.","error","warning")
    : restored ? notice("اطلاعات بازیابی شد","خودروها و ثبت‌های بازیابی‌شده را در دفتر خودرو ببینید.","good","check")
    : exported ? notice("فایل پشتیبان ذخیره شد","فایل را در جای امن نگه دارید.","good","check") : "";
  const description = exporting
    ? `<h2>نسخه‌ای از اطلاعات فعلی</h2><p>فایل شامل اطلاعات ثبت‌شدهٔ همه خودروها تا زمان ذخیره است. برای نگه داشتن تغییرات بعدی، دوباره فایل بگیرید.</p>
       ${notice("فایل رمزگذاری نمی‌شود","محتوای فایل قابل خواندن است. آن را در محل امنی ذخیره کنید.","","shield")}`
    : `<h2>برگرداندن دفتر خودرو</h2><p>فایل پشتیبانی را انتخاب کنید که قبلاً از دفتر خودرو ذخیره کرده‌اید.</p>
       ${notice("دفتر خودرو باید خالی باشد","بازیابی فقط در دفتری انجام می‌شود که هیچ خودرو یا سابقه‌ای ندارد.","","warning")}`;
  const footer = restored ? href("home","رفتن به دفتر خودرو","action") :
    exported || blocked ? href(back,"بازگشت","secondary") :
    act(exporting?"pick-export":invalid?"pick-retry":"pick-restore",exporting?"انتخاب محل ذخیره":invalid?"انتخاب فایل دیگر":"انتخاب فایل پشتیبان","action");
  return header(title,{back}) + view(`<section class="stack">${tile(exporting?"export":"restore")}${feedback || description}</section>`) + sticky(footer);
}

function vehicleForm(editing) {
  const car = vehicles[vehicleEditId];
  const year = editing ? car.year.split(" ")[0] : "";
  const back = editing ? multi ? "home-multi" : "home" : origin;
  return header(editing?"ویرایش خودرو":"افزودن خودرو",{back}) + view(`<form id="vehicle-form" class="form-fields">
    ${field("vehicle-name","نام خودرو",{value:editing?car.name:"",placeholder:"مثلاً پژو ۲۰۶",required:true})}
    ${field("year",'سال ساخت <span class="hint">اختیاری</span>',{value:year,placeholder:"مثلاً ۱۴۰۰",mode:"numeric"})}
    <div id="calendar-field" ${editing?"":"hidden"}>${selectField("calendar","تقویم سال ساخت",[["","انتخاب تقویم"],["solar","شمسی"],["gregorian","میلادی"]],editing?"solar":"")}</div>
    </form>`) + sticky(act("save-vehicle",editing?"ذخیره تغییرات":"ذخیره خودرو","action"));
}

function sheet(title, body, cancelLabel = "بستن") {
  if (overlay.open) closeSheet(false);
  modalTrigger = document.activeElement;
  overlay.innerHTML = `<div class="between sheet-title"><h2 id="overlay-title">${title}</h2>
    <button type="button" class="icon-control" data-action="dismiss" aria-label="${cancelLabel}">${icon("close")}</button>
    </div><div class="sheet-body">${body}</div>`;
  const bounds = document.querySelector("#phone").getBoundingClientRect();
  const center = bounds.left + bounds.width / 2;
  overlay.style.setProperty("--sheet-center", center + "px");
  overlay.style.setProperty("--sheet-width", bounds.width + "px");
  overlay.style.setProperty("--sheet-bottom", Math.max(0, window.innerHeight - bounds.bottom) + "px");
  overlay.style.setProperty("--sheet-height", Math.min(bounds.height, window.innerHeight) - 32 + "px");
  overlay.showModal();
  overlay.querySelector('[data-action="dismiss"]').focus();
}

function closeSheet(restoreFocus = true) {
  if (overlay.open) overlay.close();
  if (restoreFocus && modalTrigger instanceof HTMLElement && modalTrigger.isConnected) modalTrigger.focus();
}

function vehicleSheet() {
  sheet("انتخاب خودرو", Object.entries(vehicles).map(([id,car]) =>
    `<div class="between">
      <button type="button" class="row link" aria-pressed="${id === selectedVehicle}" data-action="vehicle:${id}">
        ${tile("car")}<span class="grow"><bdi>${car.name}</bdi><span class="meta">سال ساخت ${car.year}</span></span>${id === selectedVehicle ? icon("check") : ""}
      </button>
      <button type="button" class="icon-control" aria-label="ویرایش ${car.name}" data-action="edit-car:${id}">${icon("edit")}</button>
    </div>`).join("") + href("vehicle-add","افزودن خودرو","secondary","plus"));
}

function editStateWithValues() {
  const form = app.querySelector("form");
  return form ? [...new FormData(form).entries()] : [];
}

function restoreDraft(values) {
  for (const [key,value] of values) {
    const field = app.querySelector("form")?.elements.namedItem(key);
    if (field instanceof HTMLInputElement && field.type === "checkbox") field.checked = value === "on";
    else if (field) field.value = value;
  }
}

function getRoute() {
  const hash = location.hash.slice(1);
  return Object.hasOwn(scenarios,hash) || ["notifications","export-success","loading"].includes(hash) ? hash : "home";
}

function navigate(route) {
  if (location.hash === "#" + route) render(true);
  else location.hash = route;
}

function render(focus = false, updateNavigation = true) {
  const route = getRoute();
  if (updateNavigation) {
    previous = screen;
    if (route !== previous) draft = undefined;
    if (["fuel","expense","vehicle-add","restore"].includes(route) && route !== previous) {
      origin = ["empty","history","home-multi","home"].includes(previous) ? previous : route === "vehicle-add" && overlay.open && multi ? "home-multi" : "home";
    }
    if (route === "settings" && ["home","empty","home-multi"].includes(previous)) settingsOrigin = previous;
    screen = route;
  }
  closeSheet(false);
  if (route === "home-multi" || route === "switch") multi = true;
  if (route === "home" && previous === "empty") multi = false;
  const screens = {
    home: () => home(), "home-multi": () => home(), switch: () => home(), empty: () => home(true),
    fuel: () => entryForm(route), expense: () => entryForm(route), validation: () => entryForm(route),
    edit: () => entryForm(route), delete: () => entryForm(route),
    history: historyView, report: reportView, reminders: reminderView, "reminder-add": reminderForm,
    settings, notifications: notificationSettings, backup,
    export: () => backupAction(route), "export-success": () => backupAction(route),
    restore: () => backupAction(route), "restore-blocked": () => backupAction(route),
    "restore-error": () => backupAction(route), "restore-success": () => backupAction(route),
    "vehicle-add": () => vehicleForm(false), "vehicle-edit": () => vehicleForm(true),
    loading: () => header("دفتر هزینه‌ها",{back:"home",subject:active().name}) + notebookNav("history") +
      view('<section class="skeleton" aria-busy="true" aria-label="در حال بارگذاری ثبت‌ها"><span></span><span></span><span></span></section>')
  };
  app.innerHTML = screens[route]();
  scenarioNav.innerHTML = Object.entries(scenarios).map(([key,name]) =>
    `<a href="#${key}" data-scenario="${key}" ${key === route ? 'aria-current="page"' : ""}>${name}</a>`).join("");
  if (focus) app.querySelector("#view-title")?.focus({preventScroll:true});
  if (route === "switch" && updateNavigation) vehicleSheet();
  if (route === "delete" && updateNavigation) deleteSheet();
}

function deleteSheet() {
  const entry = entries.find(item => item.id === entryId);
  sheet("این ثبت حذف شود؟", `
    <p>${entry?.mileage ? "با حذف این ثبت، کارکرد همراه آن هم حذف می‌شود." : "با حذف این ثبت، دیگر آن را در سوابق این خودرو نمی‌بینید."}</p>
    <div class="actions pair">${act("dismiss","انصراف","secondary")}${act("confirm-delete","حذف ثبت","destructive")}</div>`);
}

function fieldError(id, message) {
  const input = document.getElementById(id);
  if (!input) return;
  input.setAttribute("aria-invalid","true");
  input.setAttribute("aria-describedby", id + "-help");
  let help = document.getElementById(id + "-help");
  if (!help) {
    help = document.createElement("small");
    help.id = id + "-help";
    input.after(help);
  }
  help.className = "field-error";
  help.textContent = message;
  input.focus();
}

function normalize(value) {
  return value.trim().replace(/[۰-۹٠-٩]/g, char => String((char.charCodeAt(0) & 15) % 10))
    .replaceAll("٫",".").replaceAll("٬","");
}

function previewSaved(message, route) {
  flash = message;
  navigate(route);
}

function previewEntrySave() {
  const values = Object.fromEntries(editStateWithValues());
  if (values.type === "") return fieldError("type","یک دسته‌بندی انتخاب کنید.");
  const type = values.type || "fuel";
  const amount = normalize(values.amount);
  if (!amount && !["oil","repair","parts"].includes(type)) return fieldError("amount", "مبلغ را به تومان وارد کنید.");
  if (amount && (!/^\d+(\.\d)?$/.test(amount) || Number(amount) <= 0)) {
    return fieldError("amount", "مبلغ باید بیشتر از صفر باشد و حداکثر یک رقم اعشار داشته باشد.");
  }
  if ("volume" in values && (!/^\d+(\.\d{1,3})?$/.test(normalize(values.volume)) || Number(normalize(values.volume)) <= 0))
    return fieldError("volume","حجم سوخت را بیشتر از صفر و با حداکثر سه رقم اعشار وارد کنید.");
  if (!/^\d{4}\/\d{1,2}\/\d{1,2}$/.test(normalize(values.date)))
    return fieldError("date","تاریخ شمسی را در قالب سال، ماه و روز وارد کنید.");
  if (values.mileage && !/^\d+$/.test(normalize(values.mileage))) {
    const details = app.querySelector("details");
    if (details) details.open = true;
    return fieldError("mileage","کارکرد را به‌صورت عدد صحیح از صفر به بالا وارد کنید.");
  }
  previewSaved("ثبت ذخیره شد.", screen === "edit" ? "history" : origin === "history" ? "history" : multi ? "home-multi" : "home");
}

document.addEventListener("click", event => {
  const scenario = event.target.closest("[data-scenario]");
  if (scenario) {
    if (scenario.dataset.scenario === "home") { multi = false; selectedVehicle = "peugeot"; }
    if (scenario.dataset.scenario === "report") month = "aban";
  }
  const control = event.target.closest("[data-action]");
  if (!control) return;
  const [name,key] = control.dataset.action.split(":");
  switch (name) {
    case "dismiss": closeSheet(); break;
    case "switch": vehicleSheet(); break;
    case "vehicle":
      selectedVehicle = key;
      month = "aban";
      closeSheet(false);
      if (screen === "switch") navigate("home-multi");
      else { render(false,false); app.querySelector(".vehicle-trigger")?.focus(); }
      break;
    case "edit-car": vehicleEditId = key; navigate("vehicle-edit"); break;
    case "edit": entryId = key; navigate("edit"); break;
    case "delete": deleteSheet(); break;
    case "confirm-delete": previewSaved("ثبت حذف شد.", "history"); break;
    case "month":
      sheet("ماه گزارش", (selectedVehicle === "peugeot" ? ["aban","mehr"] : ["aban"]).map(id => `<button type="button" class="row link" data-action="select-month:${id}" aria-pressed="${month === id}">${monthLabels[id]}${month === id ? icon("check") : ""}</button>`).join(""));
      break;
    case "select-month": month = key; render(false,false); app.querySelector('[data-action="month"]')?.focus(); break;
    case "reminder-detail":
    case "reminder-upcoming":
      sheet(name === "reminder-detail" ? "بازدید ترمز" : "تعویض فیلتر هوا",
        '<p>وضعیت این یادآور را مشخص کنید.</p>' + act("complete-reminder","ثبت به‌عنوان انجام‌شده","action") + act("cancel-reminder","لغو یادآور","quiet danger-link"));
      break;
    case "complete-reminder":
    case "cancel-reminder":
      closeSheet(false);
      flash = name === "complete-reminder" ? "یادآور انجام شد." : "یادآور لغو شد.";
      render(true,false); break;
    case "save-entry": previewEntrySave(); break;
    case "save-reminder": {
      const values = Object.fromEntries(editStateWithValues());
      if (!values["reminder-name"].trim()) return fieldError("reminder-name","عنوان یادآور را وارد کنید.");
      if (!values["threshold-value"].trim()) return fieldError("threshold-value",values.threshold === "date" ? "تاریخ سررسید را وارد کنید." : "کارکرد سررسید را وارد کنید.");
      previewSaved("یادآور ثبت شد.","reminders"); break;
    }
    case "save-vehicle": {
      const values = Object.fromEntries(editStateWithValues());
      if (!values["vehicle-name"].trim()) return fieldError("vehicle-name","نام خودرو را وارد کنید.");
      if (values.year && !/^[0-9]{4}$/.test(normalize(values.year))) return fieldError("year","سال ساخت را با چهار رقم وارد کنید.");
      if (values.year && !values.calendar) return fieldError("calendar","تقویم سال ساخت را انتخاب کنید.");
      previewSaved("خودرو ذخیره شد.",multi?"home-multi":"home"); break;
    }
    case "toggle-notifications": notificationState = notificationState === "off" ? "on" : "off"; render(true,false); break;
    case "pick-export":
      sheet("نمونهٔ انتخاب محل ذخیره", '<p>در برنامهٔ اندروید، انتخاب محل در فایل‌گزین گوشی انجام می‌شود.</p>' +
        href("export-success","دیدن پیام ذخیره","action") + act("dismiss","انصراف","secondary")); break;
    case "pick-restore":
    case "pick-retry":
      sheet("نمونهٔ انتخاب فایل", '<p>در برنامهٔ اندروید، فایل‌گزین گوشی باز می‌شود.</p>' +
        href(origin === "empty" || name === "pick-retry" ? "restore-success" : "restore-blocked","دیدن نتیجهٔ بازیابی","action") + href("restore-error","دیدن خطای فایل","secondary")); break;
    default: break;
  }
});

app.addEventListener("change", event => {
  if (!["type","threshold"].includes(event.target.id)) return;
  const id = event.target.id;
  const values = editStateWithValues();
  draft = { [id]: event.target.value };
  render(false,false);
  restoreDraft(values);
  document.getElementById(id)?.focus();
});

app.addEventListener("input", event => {
  if (event.target.id === "year") document.getElementById("calendar-field").hidden = !event.target.value.trim();
});

app.addEventListener("submit", event => {
  event.preventDefault();
  app.querySelector(".sticky button")?.click();
});

window.addEventListener("hashchange", () => render(true));
overlay.addEventListener("cancel", event => { event.preventDefault(); closeSheet(); });
overlay.addEventListener("click", event => { if (event.target === overlay) {
  const bounds = overlay.getBoundingClientRect();
  if (event.clientY < bounds.top || event.clientY > bounds.bottom || event.clientX < bounds.left || event.clientX > bounds.right) closeSheet();
} });
document.querySelector("#preview-appearance").innerHTML = appearanceControl("preview-theme");
render();
