"use strict";

const root = document.querySelector("#root");
const sheet = document.querySelector("#sheet");
const query = new URLSearchParams(location.search);
const scene = query.get("scene") || "home";
if (query.get("theme") === "dark") document.documentElement.dataset.theme = "dark";
if (query.get("text") === "150") {
  document.documentElement.dataset.large = "";
  document.documentElement.style.fontSize = "150%";
}

const vehicles = {
  samand: {name:"سمند LX", year:"۱۴۰۰", mileage:120500},
  peugeot: {name:"پژو ۲۰۶", year:"۱۳۹۸", mileage:94610},
};
const records = [
  {day:"۲۴", key:"repair", title:"تعویض لنت جلو", detail:"تعمیر", amount:3200000},
  {day:"۲۱", key:"oil_change", title:"تعویض روغن", detail:"روغن موتور و فیلتر", amount:850000},
  {day:"۱۹", key:"fuel", title:"سوخت", detail:"۳۸ لیتر", amount:228000},
];
const modes = {
  fuel:{label:"سوخت", form:"ثبت سوخت", icon:"fuel", detail:"مبلغ پرداختی و حجم سوخت"},
  expense:{label:"هزینه", form:"ثبت هزینه", icon:"wallet", detail:"بیمه، کارواش و سایر هزینه‌ها", categories:[["insurance","بیمه"],["wash","کارواش"],["other","سایر"]]},
  service:{label:"سرویس", form:"ثبت سرویس", icon:"tool", detail:"تعویض روغن، تعمیر و قطعات", categories:[["oil_change","تعویض روغن"],["repair","تعمیر"],["parts","قطعات"]]},
};
const paths = {
  back:"M5 12h14m-7-7 7 7-7 7", next:"m14 6-6 6 6 6", down:"m6 9 6 6 6-6",
  fuel:"M3 21h10M5 21V4h7v17M5 9h7m0 4h2a2 2 0 0 1 2 2v2a2 2 0 0 0 4 0v-7l-3-3",
  plus:"M12 5v14M5 12h14", bell:"M18 8a6 6 0 0 0-12 0c0 6-3 7-3 9h18c0-2-3-3-3-9m-8 12a2 2 0 0 0 4 0",
  book:"M5 4h14v16H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Zm3 0v16m3-12h5m-5 4h5m-5 4h3",
  home:"m3 11 9-7 9 7v9H3zM9 20v-6h6v6", settings:"M4 7h16M4 17h16M9 4v6m6 4v6",
  check:"m5 12 4 4L19 6", close:"m6 6 12 12M18 6 6 18",
  wallet:"M4 8V6a2 2 0 0 1 2-2h12v4M4 8h16v12H6a2 2 0 0 1-2-2V8Zm16 4h-5a2 2 0 0 0 0 4h5",
  tool:"M14 6a4 4 0 0 0-5 5L3 17a2.1 2.1 0 0 0 3 3l6-6a4 4 0 0 0 5-5l-3 2-2-2z",
};
const icon = key => `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="${paths[key]}"/></svg>`;
const format = new Intl.NumberFormat("fa-IR");
const number = value => `<bdi dir="ltr">${format.format(value)}</bdi>`;
const safe = value => String(value).replace(/[&<>"']/g, ch => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[ch]));
const action = (id,label,style,glyph="") => `<button type="button" data-action="${id}" class="${style}">${glyph ? icon(glyph) : ""}${label}</button>`;
const iconButton = (id,label,glyph) => `<button type="button" data-action="${id}" class="icon-button" aria-label="${label}">${icon(glyph)}</button>`;
const defaultDate = "۱۴۰۵/۰۸/۲۵";
const blank = () => ({amount:"",volume:"",category:"",date:defaultDate,mileage:"",notes:""});

let selected = "samand";
let route = "home";
let entryOrigin = "home";
let editRecord = null;
let removeMileage = false;
let mode = "";
let values = blank();
let illustrativeKeyboard = false;
let expanded = {};
let errors = {};
let message = "";
let modalTrigger;

if (scene === "fuel-keyboard") {
  mode = "fuel";
  illustrativeKeyboard = true;
  values.amount = "۲۴۰٬۰۰۰";
  values.volume = "۴۰";
} else if (scene === "fuel" || scene === "fuel-error") {
  mode = "fuel";
  if (scene === "fuel-error") { values.amount = "۰"; values.volume = "۴۰"; errors.amount = "مبلغ باید بیشتر از صفر باشد."; }
} else if (scene === "service") {
  mode = "service"; values.category = "oil_change";
} else if (scene === "expense") {
  mode = "expense";
  values.category = "wash";
  values.amount = "۱۸۰٬۰۰۰";
}

function toolbar(title, back = false) {
  return `<header class="toolbar">${back ? iconButton("back","بازگشت","back") : ""}
    <div class="subject"><h1 tabindex="-1" id="screen-title">${title}</h1>
    ${back ? `<small><bdi>${vehicles[selected].name}</bdi></small>` : ""}</div>
    ${back ? "" : iconButton("go:settings","تنظیمات","settings")}</header>`;
}
function phone(body) {
  return `<div class="phone"><div class="status" aria-hidden="true"><span>۹:۴۱</span><svg viewBox="0 0 32 12" fill="currentColor"><path d="M1 10h9V1zM12 10h7V3zM22 2h8v8h-8zm8 2h2v4h-2z"/></svg></div>
    ${body}<div class="gesture" aria-hidden="true"><span></span></div></div>`;
}
const content = (html,css="") => `<main class="body" aria-labelledby="screen-title"><div class="content ${css}">${html}</div></main>`;

function home() {
  const car = vehicles[selected];
  const primaryVehicle = selected === "samand";
  const due = primaryVehicle ? `<button class="due" type="button" data-action="reminder:due">
    ${icon("bell")}<span class="name"><strong>بیمه شخص ثالث</strong><small>موعد ۲۳ آبان ۱۴۰۵</small></span>
    <span class="tag">سررسیدشده</span></button>` : "";
  const journal = primaryVehicle ? `<div class="timeline">${records.map((item,index) => `<div class="timeline-group">
    <span class="timeline-date">${item.day}<small>آبان</small></span>
    <button type="button" class="record" data-action="record:${index}">
      <span class="sr-only">${item.day} آبان، </span>
      <span class="label"><strong>${item.title}</strong><small>${item.detail}</small></span>
      <span class="amount">${number(item.amount)}<small>تومان</small></span>
    </button></div>`).join("")}</div>` : '<p class="meta">هنوز هزینه یا سرویسی ثبت نشده است.</p>';
  return toolbar("دفتر خودرو") + content(`
    <section class="between wrap journal-scope" aria-label="خودروی فعال">
      <button class="scope" type="button" data-action="switch" aria-haspopup="dialog"><span><bdi>${car.name}</bdi><small>مدل ${car.year}</small></span>${icon("down")}<span class="sr-only">انتخاب خودرو</span></button>
      <div class="odometer">آخرین کارکرد<br><strong>${number(car.mileage)}</strong> کیلومتر</div>
    </section>
    ${due}
    <section class="stack"><div class="between wrap"><h2>آخرین ثبت‌ها</h2><span class="meta">آبان ۱۴۰۵</span></div>${journal}</section>
    <p class="preview-message" role="status" id="status-message">${safe(message)}</p>`) +
    dock("home",true);
}

function field(key,label,inputMode,placeholder="",required=false) {
  const error = errors[key];
  return `<div class="field ${key}-input"><label for="${key}">${label}</label>
    ${key === "notes" ? `<textarea id="notes" name="notes" dir="auto" rows="2">${safe(values.notes)}</textarea>`
      : `<input id="${key}" name="${key}" dir="ltr" inputmode="${inputMode}" value="${safe(values[key])}" placeholder="${placeholder}"
        ${required ? 'aria-required="true"' : ""} ${error ? `aria-invalid="true" aria-describedby="${key}-error"` : ""}>`}
    ${error ? `<small class="field-error" id="${key}-error">${error}</small>` : ""}</div>`;
}

function categoryField() {
  return `<div class="field"><label for="category">دسته‌بندی</label>
    <select id="category" name="category" aria-required="true" ${errors.category ? 'aria-invalid="true" aria-describedby="category-error"' : ""}>
      <option value="">انتخاب دسته‌بندی</option>
      ${(editRecord ? [...modes.expense.categories,...modes.service.categories,["fuel","سوخت"]] : modes[mode].categories).map(([key,label]) => `<option value="${key}" ${values.category === key ? "selected" : ""}>${label}</option>`).join("")}
    </select>${errors.category ? `<small class="field-error" id="category-error">${errors.category}</small>` : ""}</div>`;
}

function details() {
  return `<details class="date-line" data-detail="date" ${expanded.date ? "open" : ""}>
      <summary><span class="inline">تاریخ ثبت ${icon("down")}</span><bdi dir="ltr" id="date-summary">${safe(values.date || defaultDate)}</bdi></summary>
      <div class="detail-fields">${field("date","تاریخ شمسی","text","۱۴۰۵/۰۸/۲۵",true)}</div></details>
    <details data-detail="optional" ${expanded.optional ? "open" : ""}><summary><span>کارکرد و یادداشت <small>(اختیاری)</small></span>${icon("down")}</summary>
      <div class="stack detail-fields">${removeMileage ? '<p class="meta">کارکرد همراه این ثبت با ذخیره حذف می‌شود.</p>' : field("mileage","کارکرد (کیلومتر)","numeric")}
      ${editRecord?.key === "fuel" ? action("remove-mileage",removeMileage?"نگه داشتن کارکرد":"حذف کارکرد این ثبت","text-button danger") : ""}
      <small>آخرین کارکرد ثبت‌شده ${number(vehicles[selected].mileage)} کیلومتر</small>${field("notes","یادداشت","text")}</div>
    </details>`;
}

function keypad() {
  return illustrativeKeyboard ? `<div class="keypad" aria-hidden="true">${["۱","۲","۳","−","۴","۵","۶","⌫","۷","۸","۹","✓","٫","۰","٬"].map(key => `<span ${key === "✓" ? 'class="done"' : ""}>${key}</span>`).join("")}</div>` : "";
}

function entry() {
  const fuel = mode === "fuel";
  const optionalAmount = (mode === "service" || editRecord) && ["oil_change","repair","parts"].includes(values.category);
  const title = editRecord ? "ویرایش ثبت" : modes[mode].form;
  const amount = field("amount",(fuel ? "مبلغ کل پرداختی (تومان)" : "مبلغ (تومان)") + (optionalAmount ? ' <small>(اختیاری)</small>' : ""), "decimal",fuel ? "۲۴۰٬۰۰۰" : "۱۸۰٬۰۰۰",!optionalAmount);
  const fields = fuel
    ? `<section class="form-surface pair" aria-label="اطلاعات سوخت‌گیری">${amount}${field("volume","حجم سوخت (لیتر)","decimal","۴۰",true)}</section>`
    : `<section class="form-surface stack" aria-label="اطلاعات ثبت">${categoryField()}${amount}</section>`;
  return toolbar(title,true) + content(`<form class="stack entry-group" id="entry-form" novalidate>${editRecord && fuel ? categoryField() : ""}${fields}${details()}${editRecord ? action("delete","حذف ثبت","text-button danger") : ""}</form>`,"entry-content") +
    `<div class="footer">${action("save",editRecord ? "ذخیره تغییرات" : title,"primary")}</div>` + keypad();
}

function modal(title,markup) {
  modalTrigger = document.activeElement;
  sheet.innerHTML = `<div class="between sheet-header"><h2 id="sheet-title">${title}</h2>${iconButton("close","بستن","close")}</div>${markup}`;
  sheet.showModal();
  sheet.querySelector("button").focus();
}
function closeSheet(restoreFocus = true) {
  sheet.close();
  delete sheet.dataset.protected;
  if (restoreFocus && modalTrigger?.isConnected) modalTrigger.focus();
}
function entrySelector() {
  modal("نوع ثبت",Object.entries(modes).map(([key,item]) => `<button type="button" class="picker-row" data-action="open:${key}">
    <span class="icon-box">${icon(item.icon)}</span><span class="label"><strong>${item.label}</strong><small>${item.detail}</small></span>${icon("next")}</button>`).join(""));
}
function vehicleSelector() {
  modal("انتخاب خودرو",Object.entries(vehicles).map(([key,car]) =>
    `<button type="button" class="picker-row" data-action="vehicle:${key}" aria-pressed="${key === selected}">
      <span class="label"><bdi>${car.name}</bdi><small>مدل ${car.year}</small></span>${key === selected ? icon("check") : ""}</button>`).join("") + action("go:vehicles","مدیریت خودروها","secondary"));
}
function render(focus = false) {
  document.body.dataset.scene = illustrativeKeyboard ? "fuel-keyboard" : mode || "home";
  root.innerHTML = phone(mode ? entry() : screenFor(route));
  if (focus) document.querySelector("#screen-title").focus({preventScroll:true});
}
function normalized(input) {
  return input.trim().replace(/[۰-۹٠-٩]/g,digit => String(digit.charCodeAt(0) & 15)).replaceAll("٫",".");
}
function validAmount(text) {
  const value = normalized(text);
  if (!/^(?:\d+|\d{1,3}(?:,\d{3})+|\d{1,3}(?:٬\d{3})+)(?:\.\d)?$/.test(value)) return false;
  const [whole,fraction="0"] = value.replaceAll("٬","").replaceAll(",","").split(".");
  const rials = BigInt(whole + fraction);
  return rials > 0n && rials <= 9223372036854775807n;
}
function savePreview() {
  errors = {};
  if (mode !== "fuel" && !values.category) errors.category = "دسته‌بندی را انتخاب کنید.";
  if ((!["oil_change","repair","parts"].includes(values.category) || values.amount.trim()) && !validAmount(values.amount)) errors.amount = "مبلغ مثبت را با حداکثر یک رقم اعشار وارد کنید.";
  const volume = normalized(values.volume);
  if (mode === "fuel" && (!/^\d+(?:\.\d{1,3})?$/.test(volume) || Number(volume) <= 0)) errors.volume = "حجم مثبت را با حداکثر سه رقم اعشار وارد کنید.";
  if (!/^1\d{3}\/\d{2}\/\d{2}$/.test(normalized(values.date))) {
    errors.date = "تاریخ را به شکل سال، ماه و روز وارد کنید.";
    expanded.date = true;
  }
  if (!removeMileage && values.mileage.trim() && !/^\d+$/.test(normalized(values.mileage))) {
    errors.mileage = "کارکرد را به کیلومتر و بدون اعشار وارد کنید.";
    expanded.optional = true;
  }
  const first = Object.keys(errors)[0];
  if (first) {
    render();
    document.getElementById(first).focus();
    return;
  }
  const editing = Boolean(editRecord);
  mode = ""; editRecord = null;
  illustrativeKeyboard = false;
  message = editing ? "تغییرات نمونه ذخیره شد." : "ثبت نمونه انجام شد.";
  route = entryOrigin;
  render(true);
}



sheet.addEventListener("cancel",event => { event.preventDefault();if (!sheet.hasAttribute("data-protected")) closeSheet(); });
sheet.addEventListener("click",event => {
  if (!sheet.hasAttribute("data-protected") && event.target === sheet && event.clientY < sheet.getBoundingClientRect().top) closeSheet();
});
document.addEventListener("input",event => {
  const field = event.target;
  if (field.form?.id !== "entry-form" || !Object.hasOwn(values,field.name)) return;
  values[field.name] = field.value;
  if (field.name === "category") {
    if (editRecord) mode=values.category==="fuel"?"fuel":modes.service.categories.some(([key])=>key===values.category)?"service":"expense";
    delete errors.category;
    render();
    document.querySelector("#category")?.focus();
    return;
  }
  if (field.name === "date") document.querySelector("#date-summary").textContent = field.value || defaultDate;
});
document.addEventListener("toggle",event => {
  const key = event.target.dataset?.detail;
  if (key) expanded[key] = event.target.open;
},true);
document.addEventListener("submit",event => {
  if (event.target.id === "entry-form") { event.preventDefault();savePreview(); }
});
document.addEventListener("click",event => {
  const control = event.target.closest("[data-action]");
  if (!control) return;
  const [kind,key] = control.dataset.action.split(":");
  switch (kind) {
    case "new": entrySelector();break;
    case "switch": vehicleSelector();break;
    case "close": closeSheet();break;
    case "open":
      closeSheet(false);entryOrigin=route;editRecord=null;removeMileage=false;mode=key;values=blank();errors={};expanded={};message="";render(true);break;
    case "vehicle":
      closeSheet(false);selected=key;message="";render();document.querySelector(".scope").focus();break;
    case "back":
      if (illustrativeKeyboard) {illustrativeKeyboard=false;render(true);}
      else if (mode) { mode="";editRecord=null;route=entryOrigin;render(true); }
      else goBack();
      break;
    case "save": savePreview();break;
    default: extensionAction(kind,key);
  }
});
