"use strict";
const root = document.querySelector("#root");
const sheet = document.querySelector("#sheet");
const search = new URLSearchParams(location.search);
const concept = search.get("concept");
const scenario = search.get("scene") || "home";
if (search.get("text") === "150") {
  document.documentElement.dataset.large = "";
  document.documentElement.style.fontSize = "150%";
}

const vehicles = {
  samand: {name:"سمند LX", year:"۱۴۰۰", mileage:120500},
  peugeot: {name:"پژو ۲۰۶", year:"۱۳۹۸", mileage:94610},
};
const entries = [
  {day:"۲۴", title:"تعویض لنت جلو", details:"تعمیر", amount:3200000},
  {day:"۲۱", title:"تعویض روغن", details:"روغن موتور و فیلتر", amount:850000},
  {day:"۱۹", title:"سوخت", details:"۳۸ لیتر", amount:228000},
];
const dueTitle = "بیمه شخص ثالث";
const dueDate = "۲۳ آبان ۱۴۰۵";
const dateDefault = "۱۴۰۵/۰۸/۲۵";
const names = {a:"الف — ثبت سریع", b:"ب — دفتر روزانه"};
const format = new Intl.NumberFormat("fa-IR");
const paths = {
  back:'M5 12h14m-7-7 7 7-7 7', next:'m14 6-6 6 6 6', down:'m6 9 6 6 6-6',
  fuel:'M3 21h10M5 21V4h7v17M5 9h7m0 4h2a2 2 0 0 1 2 2v2a2 2 0 0 0 4 0v-7l-3-3',
  plus:'M12 5v14M5 12h14', bell:'M18 8a6 6 0 0 0-12 0c0 6-3 7-3 9h18c0-2-3-3-3-9m-8 12a2 2 0 0 0 4 0',
  book:'M5 4h14v16H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Zm3 0v16m3-12h5m-5 4h5m-5 4h3',
  home:'m3 11 9-7 9 7v9H3zM9 20v-6h6v6', settings:'M4 7h16M4 17h16M9 4v6m6 4v6',
  check:'m5 12 4 4L19 6', close:'m6 6 12 12M18 6 6 18',
};
const icon = key => `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.65" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="${paths[key]}"/></svg>`;
const number = value => `<bdi dir="ltr">${format.format(value)}</bdi>`;
const safe = value => String(value).replace(/[&<>"']/g, ch => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[ch]));
const action = (id,label,style="quiet",glyph="") =>
  `<button type="button" data-action="${id}" class="${style}">${glyph ? icon(glyph) : ""}${label}</button>`;
const iconButton = (id,label,glyph) =>
  `<button type="button" data-action="${id}" class="icon-button" aria-label="${label}">${icon(glyph)}</button>`;

let selected = "samand";
let page = scenario.startsWith("fuel") || scenario === "keyboard" ? "fuel" : "home";
let step = scenario === "fuel-first" ? 1 : 2;
let illustrativeKeyboard = scenario === "keyboard";
let values = { amount:page === "fuel" && scenario !== "fuel-first" ? "۲۴۰٬۰۰۰" : "", volume:scenario === "keyboard" ? "۰" : scenario === "fuel" ? "۴۰" : "", date:dateDefault, mileage:"", notes:"" };
let errors = illustrativeKeyboard ? {volume:"حجم سوخت باید بیشتر از صفر باشد."} : {};
let expanded = {date:false,optional:false};
let notice = "";
let modalTrigger;

function comparison() {
  const tabs = { home:"خانه", fuel:"ثبت سوخت", "fuel-first":"شروع ثبت سوخت", keyboard:"صفحه‌کلید و خطا", switch:"انتخاب خودرو" };
  const current = Object.hasOwn(tabs,scenario) ? scenario : "home";
  root.innerHTML = `<main class="preview-page"><header><h1>دو طرح برای دفتر خودرو</h1><p class="meta">پیش‌نمایش تعاملی با داده‌های نمونه</p></header>
    <nav class="overview-nav" aria-label="نمای مقایسه">${Object.entries(tabs).map(([key,label]) =>
      `<a href="?scene=${key}" ${key === current ? 'aria-current="page"' : ""}>${label}</a>`).join("")}</nav>
    <div class="preview-grid">${["a","b"].map(key => `<article><h2>${names[key]}</h2><iframe title="${names[key]}" src="?concept=${key}&amp;scene=${current}"></iframe></article>`).join("")}</div></main>`;
}

function currentVehicle() { return vehicles[selected]; }
function toolbar(title, {back=false,subject="",settings=false}={}) {
  return `<header class="toolbar">${back ? iconButton("back","بازگشت","back") : ""}
    <div class="subject"><h1 tabindex="-1" id="screen-title">${title}</h1>${subject ? `<small><bdi>${subject}</bdi></small>` : ""}</div>
    ${settings ? iconButton("outside","تنظیمات","settings") : ""}</header>`;
}

function phone(body) {
  return `<div class="phone"><div class="status" aria-hidden="true"><span>۹:۴۱</span><svg viewBox="0 0 32 12" fill="currentColor"><path d="M1 10h9V1zM12 10h7V3zM22 2h8v8h-8zm8 2h2v4h-2z"/></svg></div>
    ${body}<div class="gesture" aria-hidden="true"><span></span></div></div>`;
}

const content = (html,css="") => `<main class="body" aria-labelledby="screen-title"><div class="content ${css}">${html}</div></main>`;
const footer = label => `<div class="footer">${action("forward",label,"primary")}</div>`;
function scope() {
  const car = currentVehicle();
  return `<button class="scope" type="button" data-action="switch"><span><bdi>${car.name}</bdi><small>مدل ${car.year}</small></span>${icon("down")}<span class="sr-only">انتخاب خودرو</span></button>`;
}
function due() {
  return selected === "samand" ? `<button class="due" type="button" data-action="outside">${icon("bell")}<span class="name"><strong>${dueTitle}</strong><small>موعد ${dueDate}</small></span><span class="tag">سررسیدشده</span></button>` : "";
}
const message = '<p class="preview-message" role="status" id="prototype-status"></p>';

function homeA() {
  const car = currentVehicle();
  return toolbar("دفتر خودرو",{settings:true}) + content(`
    <section class="tray" aria-label="خودروی فعال و ثبت سریع">${scope()}
      <div class="between reading"><span class="meta">آخرین کارکرد</span><span class="inline"><strong>${number(car.mileage)}</strong><small class="meta">کیلومتر</small></span></div>
      <div class="pair">${action("fuel","ثبت سوخت","primary","fuel")}${action("outside","ثبت هزینه","secondary","plus")}</div>
    </section>
    ${due()}
    <section class="stack"><h2>سوابق و گزارش‌ها</h2><div class="link-group">
      ${["سوابق هزینه‌ها","یادآورها","گزارش ماهانه"].map(label =>
        `<button type="button" class="link-row" data-action="outside">${label}${icon("next")}</button>`).join("")}
    </div></section>${message}`);
}

function homeB() {
  const journal = selected === "samand" ? `<section class="stack"><div class="between wrap"><h2>آخرین ثبت‌ها</h2><span class="meta">آبان ۱۴۰۵</span></div><div class="timeline">
    ${entries.map(item => `<div class="timeline-group"><h3 class="timeline-date">${item.day}<small>آبان</small></h3>
    <div class="record"><span class="label"><strong>${item.title}</strong><small>${item.details}</small></span><span class="amount">${number(item.amount)}<small>تومان</small></span></div></div>`).join("")}
    </div></section>` : '<p class="meta">هنوز هزینه‌ای برای این خودرو ثبت نشده است.</p>';
  return toolbar("دفتر خودرو",{settings:true}) + content(`
    <section class="between wrap journal-scope">${scope()}<div class="odo-inline">آخرین کارکرد<br>${number(currentVehicle().mileage)} کیلومتر</div></section>
    ${journal}
    ${selected === "samand" ? `<section class="stack"><h2>پیگیری خودرو</h2>${due()}</section>` : ""}
    ${message}`) + `<div class="dock">${action("new","ثبت جدید","primary","plus")}
    <nav class="nav" aria-label="بخش‌های دفتر خودرو"><button class="nav-item" type="button" aria-current="page">${icon("home")}خانه</button>
    ${action("outside","سوابق","nav-item","book")}${action("outside","یادآورها","nav-item","bell")}</nav></div>`;
}

function field(key,label,mode,placeholder="") {
  const error = errors[key];
  return `<div class="field ${key === "amount" ? "amount-input" : ""}"><label for="${key}">${label}</label>
    ${key === "notes" ? `<textarea id="notes" name="notes" dir="auto" rows="2">${safe(values.notes)}</textarea>`
      : `<input id="${key}" name="${key}" dir="ltr" inputmode="${mode}" value="${safe(values[key])}" placeholder="${placeholder}" ${["amount","volume","date"].includes(key) ? 'aria-required="true"' : ""} ${error ? `aria-invalid="true" aria-describedby="${key}-error"` : ""}>`}
    ${error ? `<small class="field-error" id="${key}-error">${error}</small>` : ""}</div>`;
}

function details() {
  return `<details class="date-line" data-detail="date" ${expanded.date ? "open" : ""}>
      <summary><span class="inline">تاریخ ثبت ${icon("down")}</span><bdi dir="ltr" id="date-summary">${safe(values.date || dateDefault)}</bdi></summary>
      <div class="detail-fields">${field("date","تاریخ شمسی","text","۱۴۰۵/۰۸/۲۵")}</div></details>
    <details data-detail="optional" ${expanded.optional ? "open" : ""}><summary>کارکرد و یادداشت <small>(اختیاری)</small></summary>
      <div class="stack detail-fields">${field("mileage","کارکرد (کیلومتر)","numeric")}
      <small>آخرین کارکرد ثبت‌شده ${number(currentVehicle().mileage)} کیلومتر</small>${field("notes","یادداشت","text")}</div>
    </details>`;
}

function keyboard() {
  return illustrativeKeyboard ? `<div class="keypad" aria-hidden="true">${["۱","۲","۳","۴","۵","۶","۷","۸","۹","٫","۰","⌫"].map(key => `<span>${key}</span>`).join("")}</div>` : "";
}

function entry() {
  const amount = field("amount","مبلغ کل پرداختی (تومان)","decimal","۲۴۰٬۰۰۰");
  const volume = field("volume","حجم سوخت (لیتر)","decimal","۴۰");
  const shared = toolbar("ثبت سوخت",{back:true,subject:currentVehicle().name});
  if (concept === "a") {
    return shared + content(`<form class="stack entry-group" id="fuel-form" novalidate>
      <section class="stack form-surface" aria-label="اطلاعات سوخت‌گیری">
      <div class="${illustrativeKeyboard ? "stack" : "pair"}">${amount}${volume}</div></section>
      ${details()}</form>`, "entry-content") + footer("ثبت سوخت") + keyboard();
  }
  return shared + content(`<form class="stack entry-group" id="fuel-form" novalidate>
    <ol class="steps" aria-label="مرحله ثبت سوخت"><li ${step === 1 ? 'aria-current="step"' : ""}>۱. مبلغ پرداختی</li><li ${step === 2 ? 'aria-current="step"' : ""}>۲. حجم سوخت</li></ol>
    ${step === 1 ? `<section class="stack"><h2>چه مبلغی پرداخت کردید؟</h2>${amount}</section>`
      : `<section class="receipt" aria-label="مرور مبلغ"><div class="between wrap"><span class="meta">مبلغ کل پرداختی</span>
       <span><strong><bdi dir="ltr">${safe(values.amount)}</bdi></strong> <small>تومان</small></span></div></section>
       ${volume}${details()}`}</form>`, "entry-content") + footer(step === 1 ? "ادامه به حجم سوخت" : "ثبت سوخت") + keyboard();
}

function vehicleRows() {
  return Object.entries(vehicles).map(([id,car]) =>
    `<button type="button" class="picker-row" data-action="select:${id}" aria-pressed="${id === selected}"><span><bdi>${car.name}</bdi><small>مدل ${car.year}</small></span>${id === selected ? icon("check") : ""}</button>`).join("");
}
function chooseVehiclePage() {
  return toolbar("انتخاب خودرو",{back:true}) + content(`<section class="stack">${vehicleRows()}${action("outside","افزودن خودرو","secondary","plus")}</section>${message}`);
}
function modal(title,body) {
  modalTrigger = document.activeElement;
  sheet.innerHTML = `<section class="stack"><div class="between"><h2 id="sheet-title">${title}</h2>${iconButton("close","بستن","close")}</div>${body}</section>`;
  sheet.showModal();
  sheet.querySelector("button").focus();
}
function closeSheet() {
  sheet.close();
  if (modalTrigger?.isConnected) modalTrigger.focus();
}

function render(focus = false) {
  root.innerHTML = phone(page === "fuel" ? entry() : page === "vehicles" ? chooseVehiclePage() : concept === "a" ? homeA() : homeB());
  const status = document.querySelector("#prototype-status");
  if (status && notice) {
    status.textContent = notice;
    notice = "";
  }
  if (focus) document.querySelector("#screen-title").focus({preventScroll:true});
}
function normalize(input) {
  return input.trim().replace(/[۰-۹٠-٩]/g,digit => String(digit.charCodeAt(0) & 15)).replaceAll("٫",".").replaceAll("٬","").replaceAll(",","");
}
function digitsValid(key,pattern) { const v=normalize(values[key]); return pattern.test(v) && Number(v)>0; }
function openEntry() {
  if (sheet.open) closeSheet();
  page="fuel";step=1;errors={};expanded={date:false,optional:false};illustrativeKeyboard=false;
  values={amount:"",volume:"",date:dateDefault,mileage:"",notes:""};
  render(true);
}
function next() {
  const amountValid=digitsValid("amount",/^\d+(\.\d)?$/);
  const volumeValid=digitsValid("volume",/^\d+(\.\d{1,3})?$/);
  if (concept === "b" && step === 1 && amountValid) { step=2;errors={};render(true);document.querySelector("#volume").focus();return; }
  errors = {};
  if (!amountValid) errors.amount="مبلغ کل را بیشتر از صفر وارد کنید.";
  if ((concept === "a" || step === 2) && !volumeValid) errors.volume="حجم سوخت را بیشتر از صفر وارد کنید.";
  if (concept === "a" || step === 2) {
    if (!/^1\d{3}\/\d{2}\/\d{2}$/.test(normalize(values.date))) { errors.date = "تاریخ را به شکل سال، ماه و روز وارد کنید."; expanded.date=true; }
    if (values.mileage && !/^\d+$/.test(normalize(values.mileage))) { errors.mileage = "کارکرد را به کیلومتر و بدون اعشار وارد کنید."; expanded.optional=true; }
  }
  const first=Object.keys(errors)[0];
  if (first) {
    if (concept === "b" && first === "amount") step=1;
    render();
    document.getElementById(first)?.focus();
    return;
  }
  page="home";notice="ثبت نمونه انجام شد.";render(true);
}

if (concept !== "a" && concept !== "b") comparison();
else {
  if (scenario === "switch" && concept === "b") page = "vehicles";
  render();
  if (scenario === "switch" && concept === "a") modal("انتخاب خودرو",vehicleRows()+action("outside","افزودن خودرو","secondary","plus"));
  document.addEventListener("input",event => {
    const field = event.target;
    if (field.form?.id !== "fuel-form" || !Object.hasOwn(values,field.name)) return;
    values[field.name]=field.value;
    if (field.name==="date") document.querySelector("#date-summary").textContent=field.value || dateDefault;
  });
  document.addEventListener("toggle",event => {
    const key=event.target.dataset?.detail;
    if (key) expanded[key]=event.target.open;
  },true);
  document.addEventListener("submit",event => { if (event.target.id === "fuel-form") { event.preventDefault(); next(); } });
  sheet.addEventListener("cancel",event => { event.preventDefault();closeSheet(); });
  document.addEventListener("click",event => {
    const control=event.target.closest("[data-action]");
    if (!control) return;
    const [kind,id]=control.dataset.action.split(":");
    switch (kind) {
      case "fuel": openEntry();break;
      case "new":
        modal("نوع ثبت", action("fuel","ثبت سوخت","primary","fuel")+action("outside","ثبت هزینه یا سرویس","secondary","plus"));break;
      case "switch":
        if (concept === "a") modal("انتخاب خودرو",vehicleRows()+action("outside","افزودن خودرو","secondary","plus"));
        else {page="vehicles";render(true);}
        break;
      case "select":
        selected=id;if (sheet.open) closeSheet();page="home";render();document.querySelector(".scope").focus();break;
      case "back":
        if (illustrativeKeyboard) {illustrativeKeyboard=false;render(true);}
        else if (page==="fuel" && concept==="b" && step===2) {step=1;render(true);}
        else {page="home";render(true);}
        break;
      case "close": closeSheet();break;
      case "forward": next();break;
      case "outside":
        if (sheet.open) closeSheet();
        document.querySelector("#prototype-status").textContent="نمای این بخش پس از انتخاب طرح آماده می‌شود.";
        break;
    }
  });
}
