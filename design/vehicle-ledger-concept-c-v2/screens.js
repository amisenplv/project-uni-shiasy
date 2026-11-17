"use strict";

const tabs = ["home", "history", "reminders"];
const sceneLabels = {
  home:"خانه", selector:"انتخاب نوع ثبت", "fuel-keyboard":"سوخت با صفحه‌کلید", fuel:"ثبت سوخت",
  "fuel-error":"خطای ثبت سوخت", expense:"ثبت هزینه", service:"ثبت سرویس", first:"شروع دفتر خودرو",
  "vehicle-switch":"انتخاب خودرو", vehicles:"مدیریت خودروها", "vehicle-add":"افزودن خودرو", "vehicle-edit":"ویرایش خودرو",
  catalog:"فهرست مدل‌ها (آزمایشی)", history:"سوابق", "history-empty":"سوابق خالی", loading:"در حال بارگیری",
  "history-error":"خطای بارگیری سوابق", record:"جزئیات ثبت", "record-edit":"ویرایش ثبت", delete:"تأیید حذف",
  report:"گزارش ماهانه", "report-empty":"ماه بدون ثبت", month:"انتخاب ماه",
  reminders:"یادآورها", "reminders-empty":"یادآور خالی", "reminder-create":"یادآور تاریخ‌دار", "reminder-mileage":"یادآور کارکرد",
  "reminder-detail":"جزئیات یادآور", "reminder-cancel":"تأیید لغو یادآور", settings:"تنظیمات", appearance:"انتخاب ظاهر",
  notifications:"اعلان‌ها", "notification-on":"اعلان فعال", "notification-blocked":"اعلان مسدود", backup:"پشتیبان‌گیری دستی", export:"ساخت فایل پشتیبان",
  "export-success":"فایل ذخیره‌شده", restore:"بازیابی از فایل", "restore-blocked":"دفتر پر", "restore-invalid":"فایل نامعتبر",
  "restore-success":"بازیابی موفق", "restore-warning":"بازیابی با هشدار", "restore-working":"در حال بازیابی", "cloud-auth":"ورود آزمایشی",
  "cloud-bound":"پشتیبان آنلاین آزمایشی", "cloud-restore":"بازبینی پشتیبان آنلاین", "cloud-recovery":"کد بازیابی آزمایشی",
};
const labels = { home:"خانه", history:"سوابق", reminders:"یادآورها" };
const tabIcons = { home:"home", history:"book", reminders:"bell" };
const months = { aban:"آبان ۱۴۰۵", mehr:"مهر ۱۴۰۵" };
const allRecords = [...records, {day:"۱۰",key:"parts",title:"قطعات",detail:"فیلتر هوا",amount:null}];
const reminderItems = {
  due:{title:"بیمه شخص ثالث",status:"سررسیدشده",when:"موعد ۲۳ آبان ۱۴۰۵"},
  upcoming:{title:"تعویض فیلتر هوا",status:"در انتظار موعد",when:"موعد در کارکرد ۱۲۳٬۰۰۰ کیلومتر",extra:"۲٬۵۰۰ کیلومتر تا موعد"},
  completed:{title:"تعویض روغن",status:"انجام‌شده",when:"موعد ۲۱ آبان ۱۴۰۵"},
  canceled:{title:"بازدید لاستیک",status:"لغوشده",when:"موعد ۱۸ آبان ۱۴۰۵"},
};
const parents = [];
let activeTab = "home";
let viewedRecord = 2;
let viewedReminder = "due";
let period = "aban";
let theme = query.get("theme") === "dark" ? "dark" : "system";
let threshold = "date";
let vehicleCalendar = "solar";
let vehicleDemo = {name:"",year:""};
let cloudMode = "signin";

const screenBar = (title, subject = "", back = true, end = "") => `<header class="toolbar">
  ${back ? iconButton("back","بازگشت","back") : ""}<div class="subject"><h1 tabindex="-1" id="screen-title">${title}</h1>
  ${subject ? `<small><bdi>${subject}</bdi></small>` : ""}</div>${end}</header>`;
const footer = (id,title,style="primary") => `<div class="footer">${action(id,title,style)}</div>`;
const group = (title,markup,extra="") => `<section class="stack ${extra}">${title ? `<h2>${title}</h2>` : ""}<div class="list">${markup}</div></section>`;
const item = (id,title,detail="",right="") => `<button type="button" class="item" data-action="${id}"><span><strong>${title}</strong>${detail ? `<small>${detail}</small>` : ""}</span>${right || icon("next")}</button>`;
const noticeView = (title,copy,tone="") => `<div class="notice ${tone}" role="status"><p><strong>${title}</strong></p>${copy ? `<p>${copy}</p>` : ""}</div>`;
const subject = () => vehicles[selected].name;
const feedback = () => message ? noticeView(safe(message),"","success") : "";
const amountValue = value => value === null ? '<small>بدون مبلغ</small>' : `${number(value)} <small>تومان</small>`;
const dateFor = day => "۱۴۰۵/۰۸/" + day;

function dock(tab, entry = false) {
  return `<div class="dock">${entry ? `<button type="button" data-action="new" class="primary" aria-haspopup="dialog">${icon("plus")}ثبت جدید</button>` : ""}
    <nav class="nav" aria-label="بخش‌های دفتر خودرو">${tabs.map(key=>`<button type="button" class="nav-item" data-action="tab:${key}" ${key===tab?'aria-current="page"':""}>${icon(tabIcons[key])}${labels[key]}</button>`).join("")}</nav></div>`;
}
function goto(next) {
  if (sheet.open) closeSheet(false);
  if (next === route) return;
  parents.push(route);
  route = next;
  message = "";
  render(true);
}
function goBack() {
  if (sheet.open) { closeSheet(); return; }
  const parent = parents.pop();
  route = parent || (route === "home" ? "home" : activeTab);
  message = "";
  render(true);
}
function jump(tab) {
  if (sheet.open) closeSheet(false);
  route = activeTab = tab;
  parents.length = 0;
  message = "";
  render(true);
}
function firstLaunch() {
  return screenBar("دفتر خودرو","",false,iconButton("go:settings","تنظیمات","settings")) + content(`
    <section class="status-copy"><h2>اولین خودرو را اضافه کنید</h2><p>هزینه‌ها، سوخت و سرویس‌های خودرو را در همین گوشی ثبت کنید.</p></section>
    ${action("go:vehicle-add","افزودن خودرو","primary","plus")}
    ${group("",item("go:restore","بازیابی از فایل پشتیبان","اگر قبلاً از دفتر خودرو پشتیبان گرفته‌اید"))}`);
}
function historyScreen(state) {
  const heading = screenBar("سوابق",subject(),false);
  const ledgers = state === "history-empty" || selected !== "samand" ? `<section class="status-copy"><h2>هنوز ثبتی ندارید</h2><p>پس از ثبت سوخت، هزینه یا سرویس، آن را اینجا می‌بینید.</p></section>` :
    state === "history-error" ? noticeView("سوابق بارگیری نشد","اتصال به داده‌های دفتر برقرار نشد.","error")+action("retry:history","تلاش دوباره","secondary") :
    state === "loading" ? '<section class="stack" aria-label="در حال بارگیری سوابق" aria-busy="true"><span class="skeleton"></span><span class="skeleton"></span><span class="skeleton"></span></section>' :
    `<section class="stack"><h2>آبان ۱۴۰۵</h2><div class="timeline">${allRecords.map((r,i)=>`<div class="timeline-group"><span class="timeline-date" aria-hidden="true">${r.day}<small>آبان</small></span>
    <button type="button" class="record" data-action="record:${i}"><span class="sr-only">${r.day} آبان، </span>
    <span class="label"><strong>${r.title}</strong><small>${r.detail}</small></span><span class="amount">${amountValue(r.amount)}</span></button></div>`).join("")}</div></section>`;
  return heading + content(feedback()+group("",item("go:report","گزارش ماهانه","جمع هزینه‌ها به تفکیک دسته"))+ledgers) + dock("history",true);
}
function recordView() {
  const r=allRecords[viewedRecord];
  return screenBar("جزئیات ثبت",subject()) + content(`
    <section class="summary"><h2>${r.title}</h2><div class="hero-value"><strong>${r.amount===null ? "بدون مبلغ" : number(r.amount)}</strong>${r.amount===null ? "" : "<small>تومان</small>"}</div><p class="meta">${numberDate(dateFor(r.day))}</p></section>
    ${group("",`<div class="item"><span>دسته‌بندی</span><strong>${categoryName(r.key)}</strong></div>${r.key==="fuel" ? '<div class="item"><span>حجم سوخت</span><strong>۳۸ لیتر</strong></div><div class="item"><span>کارکرد ثبت‌شده</span><strong>۱۱۹٬۸۰۰ کیلومتر</strong></div>' : `<div class="item"><span>یادداشت</span><strong>${r.detail}</strong></div>`}`)}
    ${action("delete","حذف ثبت","text-button danger")}`) + footer("edit-record","ویرایش ثبت");
}
function categoryName(key) {
  return key === "fuel" ? "سوخت" : [...modes.expense.categories,...modes.service.categories].find(([k])=>k===key)?.[1] || "دسته نامشخص";
}
function numberDate(date) { return `<bdi dir="ltr">${date}</bdi>`; }
function editCurrentRecord() {
  const r=allRecords[viewedRecord]; editRecord=r; removeMileage=false; entryOrigin=route;
  mode=r.key==="fuel"?"fuel":modes.service.categories.some(([key])=>key===r.key)?"service":"expense";
  values=blank(); values.category=r.key; values.amount=r.amount===null?"":format.format(r.amount);
  values.date=dateFor(r.day); values.volume=r.key==="fuel"?"۳۸":""; values.mileage=r.key==="fuel"?"۱۱۹۸۰۰":"";
  values.notes=r.key==="fuel"?"":r.detail; expanded={optional:true}; errors={}; render(true);
}
function report(empty) {
  const monthRecords = period==="aban" ? allRecords : [{key:"wash",amount:180000}];
  const rows = monthRecords.filter(r=>r.amount!==null);
  const total = rows.reduce((n,r)=>n+r.amount,0);
  const ruled = rows.map(r=>`<div><div class="between"><strong>${categoryName(r.key)}</strong><span>${amountValue(r.amount)}</span></div>
    <div class="bar-track" aria-hidden="true"><span style="width:${100*r.amount/total}%"></span></div></div>`).join("");
  return screenBar("گزارش ماهانه",subject()) + content(`
    <div class="list">${item("month",months[period],"ماه گزارش",icon("down"))}</div>
    ${empty || selected!=="samand" ? '<section class="status-copy"><h2>در این ماه ثبتی ندارید</h2><p>ماه دیگری را انتخاب کنید یا به سوابق برگردید.</p></section>' :
    `<section class="summary"><h2>جمع هزینه‌های ثبت‌شده</h2><div class="hero-value"><strong>${number(total)}</strong><small>تومان</small></div><p class="meta totals-note">${period==="aban" ? "۴ ثبت، ۱ ثبت بدون مبلغ" : "۱ ثبت"}</p></section>
    <section class="stack"><h2>به تفکیک دسته</h2>${ruled}${period==="aban"?'<div class="between"><strong>قطعات</strong><small>بدون مبلغ</small></div>':""}</section>`}`);
}
function reminders(empty) {
  const list = empty || selected!=="samand" ? '<section class="status-copy"><h2>هنوز یادآوری ندارید</h2><p>برای تاریخ یا کارکرد مشخص یک یادآور بسازید.</p></section>' :
    group("سررسیدشده",item("reminder:due",reminderItems.due.title,reminderItems.due.when),"due-section")+
    group("در انتظار موعد",item("reminder:upcoming",reminderItems.upcoming.title,reminderItems.upcoming.extra))+
    group("سوابق یادآورها", ["completed","canceled"].map(k=>item("reminder:"+k,reminderItems[k].title,reminderItems[k].status)).join(""));
  return screenBar("یادآورها",subject(),false) + content(feedback()+list) +
    `<div class="dock">${action("go:reminder-create","ثبت یادآور","primary","plus")}<nav class="nav" aria-label="بخش‌های دفتر خودرو">${tabs.map(key=>`<button type="button" class="nav-item" data-action="tab:${key}" ${key==="reminders"?'aria-current="page"':""}>${icon(tabIcons[key])}${labels[key]}</button>`).join("")}</nav></div>`;
}
function demoField(id,label,example="",opts={}) {
  return `<div class="field"><label for="${id}">${label}</label><input id="${id}" ${opts.ltr?'dir="ltr"':""} ${opts.numeric?'inputmode="numeric"':""}
    type="${opts.password?"password":"text"}" value="${safe(example)}"></div>`;
}
function reminderForm() {
  const mileage=threshold==="mileage";
  return screenBar("ثبت یادآور",subject())+content(`<section class="form-surface stack">
    ${demoField("reminder-title","عنوان یادآور","تعویض فیلتر هوا")}
    <div class="stack"><h2>موعد یادآور</h2><div class="inline-set">
      <button type="button" class="secondary" data-action="threshold:date" aria-pressed="${!mileage}">تاریخ</button>
      <button type="button" class="secondary" data-action="threshold:mileage" aria-pressed="${mileage}">کارکرد</button></div></div>
    ${mileage?demoField("reminder-mileage","کارکرد موعد (کیلومتر)","۱۲۳۰۰۰",{numeric:true,ltr:true}):demoField("reminder-date","تاریخ موعد (شمسی)","۱۴۰۵/۰۹/۱۰",{ltr:true})}
    </section>`, "entry-content")+footer("notice:reminder","ثبت یادآور");
}
function reminderDetail() {
  const r=reminderItems[viewedReminder];
  const active=["due","upcoming"].includes(viewedReminder);
  return screenBar("جزئیات یادآور",subject()) + content(`
    <section class="stack"><h2>${r.title}</h2>${noticeView(r.status,r.when,viewedReminder==="due"?"warning":"")}
    ${r.extra?`<p class="meta">${r.extra}</p>`:""}</section>
    ${active?action("cancel-reminder","لغو یادآور","text-button danger"):""}`) + (active?footer("complete-reminder","انجام شد"):"");
}
function vehiclesView() {
  return screenBar("مدیریت خودروها") + content(group("خودروها",Object.entries(vehicles).map(([key,car])=>
    item("edit-vehicle:"+key,`<bdi>${car.name}</bdi>`,`مدل ${car.year}${key===selected?"، خودروی فعال":""}`)).join("")))+footer("go:vehicle-add","افزودن خودرو");
}
let vehicleEditKey="samand";
function vehicleForm(editing) {
  return screenBar(editing?"ویرایش خودرو":"افزودن خودرو")+content(`<section class="form-surface stack">
    ${demoField("vehicle-name","نام خودرو",vehicleDemo.name)}
    ${demoField("vehicle-year","سال ساخت (اختیاری)",vehicleDemo.year,{numeric:true,ltr:true})}
    <div id="vehicle-calendar" ${vehicleDemo.year.trim()?"":"hidden"}><div class="stack"><h2>تقویم سال ساخت</h2><div class="inline-set">
    <button class="secondary" type="button" data-action="calendar:solar" aria-pressed="${vehicleCalendar==="solar"}">شمسی</button>
    <button class="secondary" type="button" data-action="calendar:gregorian" aria-pressed="${vehicleCalendar==="gregorian"}">میلادی</button></div></div></div>
    </section>`)+footer("notice:vehicle","ذخیره خودرو");
}
function catalogView() {
  return screenBar("انتخاب مدل خودرو")+content(noticeView("فهرست آزمایشی","نمایش این فهرست به وجود بستهٔ مدل‌های تأییدشده وابسته است.")+
    demoField("model-search","جست‌وجوی مدل","پژو")+group("",item("go:vehicle-add","پژو ۲۰۶","مدل نمونه"))+action("go:vehicle-add","ورود دستی نام خودرو","secondary"));
}
function settingsView() {
  const appearanceLabel={system:"تنظیم گوشی",light:"روشن",dark:"تیره"}[theme];
  return screenBar("تنظیمات") + content(
    group("ترجیحات",item("appearance","ظاهر",appearanceLabel)+item("go:notifications","اعلان‌ها","اعلان یادآورهای تاریخ‌دار"))+
    group("داده‌ها",item("go:backup","پشتیبان‌گیری دستی","ذخیره یا بازیابی از فایل")+
      (query.get("debug")==="cloud"?item("go:cloud-auth","پشتیبان آنلاین","فقط نمای آزمایشی"):"")));
}
function notifications(key) {
  const blocked=key==="notification-blocked";
  const enabled=key==="notification-on";
  return screenBar("اعلان‌ها")+content(`
    <section class="stack"><h2>اعلان یادآورهای تاریخ‌دار</h2><p class="meta">اعلان ممکن است با تأخیر برسد. یادآورهای سررسیدشده در دفتر خودرو هم دیده می‌شوند.</p></section>
    ${blocked ? noticeView("اعلان مسدود است","اعلان را در تنظیمات گوشی مجاز کنید.","warning") : enabled ? noticeView("اعلان‌ها روشن‌اند","اعلان‌ها در تنظیمات گوشی مجاز هستند.","success") : noticeView("اعلان‌ها خاموش‌اند","می‌توانید اعلان یادآورهای تاریخ‌دار را فعال کنید.")}
    ${blocked?action("android-settings","تنظیمات گوشی","primary")+action("go:notifications","خاموش کردن اعلان یادآورها","secondary"):enabled?action("go:notifications","خاموش کردن اعلان یادآورها","secondary"):action("go:notification-on","فعال کردن اعلان","primary")}
    <p id="status-message" class="preview-message" role="status"></p>`);
}
function backupView() {
  return screenBar("پشتیبان‌گیری دستی")+content(`
    ${group("فایل پشتیبان",item("go:export","ذخیره فایل پشتیبان","از همه خودروها و ثبت‌های فعلی")+
      item("go:restore","بازیابی از فایل","فقط در دفتر کاملاً خالی"))}
    ${noticeView("پشتیبان‌گیری دستی است","برای ذخیرهٔ تغییرات بعدی، دوباره پشتیبان بگیرید.")}`);
}
function backupActionScreen(key) {
  const heading=key.startsWith("export")?"ذخیره فایل پشتیبان":"بازیابی از فایل";
  const presets={
    export:["نسخه‌ای از اطلاعات فعلی","این فایل شامل همهٔ خودروها و ثبت‌های موجود در زمان ذخیره است.",
      noticeView("فایل رمزگذاری نمی‌شود","محتوای آن قابل خواندن است. محل امنی برای ذخیره انتخاب کنید.","warning"),"mock-picker:export","انتخاب محل ذخیره"],
    "export-success":["فایل پشتیبان ذخیره شد","فایل را در جایی نگه دارید که پس از تعویض گوشی هم به آن دسترسی داشته باشید.",
      noticeView("ذخیره انجام شد","","success"),"back","بازگشت"],
    restore:["برگرداندن اطلاعات دفتر","فایلی را انتخاب کنید که قبلاً از دفتر خودرو ذخیره کرده‌اید.",
      noticeView("دفتر خودرو باید خالی باشد","اگر خودرو یا ثبتی در دفتر هست، بازیابی ممکن نیست.","warning"),"mock-picker:restore","انتخاب فایل پشتیبان"],
    "restore-blocked":["بازیابی انجام نشد","اطلاعات فعلی حفظ شد. بازیابی فقط در دفتر کاملاً خالی ممکن است.",
      noticeView("دفتر خودرو خالی نیست","","error"),"back","بازگشت"],
    "restore-invalid":["فایل بازیابی نشد","فایل معتبر نیست. اطلاعات فعلی تغییری نکرده است.",
      noticeView("فایل دیگری انتخاب کنید","از فایل پشتیبان دفتر خودرو استفاده کنید.","error"),"mock-picker:restore","انتخاب فایل دیگر"],
    "restore-success":["اطلاعات بازیابی شد","خودروها و ثبت‌ها را در دفتر خودرو ببینید.",
      noticeView("بازیابی انجام شد","","success"),"tab:home","رفتن به دفتر خودرو"],
    "restore-warning":["اطلاعات بازیابی شد","دفتر خودرو آماده است. فایل را دوباره وارد نکنید.",
      noticeView("بررسی اعلان‌ها کامل نشد","وضعیت اعلان‌ها را در تنظیمات بررسی کنید.","warning"),"tab:home","رفتن به دفتر خودرو"],
  };
  const [title,description,notice,id,button] = presets[key];
  return screenBar(heading)+content(`<section class="status-copy"><h2>${title}</h2><p>${description}</p></section>${notice}`)+footer(id,button);
}
function cloudScreen(bound) {
  const demo=noticeView("محیط آزمایشی","فقط برای داده‌های ساختگی. بازیابی آنلاین در نسخه آفلاین نمایش داده نمی‌شود.","warning");
  if (!bound) {
    const register=cloudMode==="register", recovery=cloudMode==="recover";
    const title=register?"ثبت‌نام":recovery?"بازیابی حساب":"ورود";
    return screenBar("پشتیبان آنلاین")+content(demo+`<section class="form-surface stack"><h2>${title}</h2>
      ${demoField("username","نام کاربری","",{ltr:true})}
      ${demoField("password",recovery?"گذرواژه جدید":"گذرواژه","",{ltr:true,password:true})}
      ${register||recovery?demoField("cloud-code",recovery?"کد بازیابی":"کد دعوت","",{ltr:true,password:true}):""}</section>
      <div class="inline-set">${action("cloud-mode:"+(cloudMode==="signin"?"register":"signin"),cloudMode==="signin"?"ثبت‌نام":"ورود","secondary")}
      ${cloudMode==="signin"?action("cloud-mode:recover","بازیابی حساب","secondary"):""}</div>`)+footer("go:cloud-bound",title);
  }
  return screenBar("پشتیبان آنلاین")+content(demo+
    group("حساب",`<div class="item"><span>نام کاربری</span><bdi dir="ltr">demo-driver</bdi></div>`)+
    group("آخرین پشتیبان تأییدشده",`<div class="item"><span>زمان</span><span>۲۵ آبان ۱۴۰۵</span></div><div class="item"><span>شناسه نسخه</span><bdi dir="ltr">demo-r17</bdi></div>`)+
    action("cloud-replace","ارسال پشتیبان جدید","primary")+
    group("بازیابی",item("cloud-restore","بررسی پشتیبان برای بازیابی","فقط در دفتر کاملاً خالی"))+
    action("go:cloud-auth","خروج از حساب","text-button"));
}
function confirmation(title,copy,confirm,kind="primary") {
  modal(title,`<div class="sheet-copy"><p>${copy}</p></div><div class="sheet-actions">${action(confirm[0],confirm[1],kind)}${action("close","انصراف","secondary")}</div>`);
  sheet.querySelector('[data-action="close"]').focus();
}
function screenFor(key) {
  switch(key) {
    case "home": return home();
    case "first": return firstLaunch();
    case "history": case "history-empty": case "history-error": case "loading": return historyScreen(key);
    case "record": return recordView();
    case "report": case "report-empty": return report(key==="report-empty");
    case "reminders": case "reminders-empty": return reminders(key==="reminders-empty");
    case "reminder-create": return reminderForm();
    case "reminder-detail": return reminderDetail();
    case "vehicles": return vehiclesView();
    case "vehicle-add": case "vehicle-edit": return vehicleForm(key==="vehicle-edit");
    case "catalog": return catalogView();
    case "settings": return settingsView();
    case "notifications": case "notification-blocked": case "notification-on": return notifications(key);
    case "backup": return backupView();
    case "export": case "export-success": case "restore": case "restore-blocked": case "restore-invalid": case "restore-success": case "restore-warning": return backupActionScreen(key);
    case "restore-working": return screenBar("بازیابی از فایل")+content(noticeView("در حال بررسی فایل و بازیابی…","این عملیات ممکن است چند لحظه طول بکشد.")+'<progress aria-label="در حال بازیابی"></progress>');
    case "cloud-auth": case "cloud-bound": return cloudScreen(key==="cloud-bound");
    default: return home();
  }
}
function notifyPreview(messageText) { goBack();message=messageText;render(true); }
function setTheme(next) {
  theme=next; const dark = theme==="dark" || (theme==="system"&&matchMedia("(prefers-color-scheme:dark)").matches);
  document.documentElement.dataset.theme=dark?"dark":"light";
}
function extensionAction(kind,key) {
  switch(kind) {
    case "go":
      if (key==="vehicle-add") {vehicleDemo={name:"",year:""};vehicleCalendar="";}
      goto(key);break;
    case "tab": jump(key);break;
    case "retry": route=key;render(true);break;
    case "record": viewedRecord=Number(key);goto("record");break;
    case "edit-record": editCurrentRecord();break;
    case "remove-mileage": removeMileage=!removeMileage;render();document.querySelector('[data-action="remove-mileage"]')?.focus();break;
    case "delete": {
      const r=allRecords[viewedRecord];
      confirmation("این ثبت حذف شود؟", `${r.title}، ${r.day} آبان ۱۴۰۵. ${r.key==="fuel"?"کارکرد همراه این ثبت هم حذف می‌شود.":"پس از حذف، این ثبت در سوابق دیده نمی‌شود."}`,["delete-confirm","حذف ثبت"],"danger-button");
      break;
    }
    case "delete-confirm":
      closeSheet(false);mode="";editRecord=null;route=parents.pop()||"history";message="ثبت نمونه حذف شد.";render(true);break;
    case "month":
      modal("ماه گزارش",Object.entries(months).map(([id,title])=>`<button class="picker-row" type="button" data-action="choose-month:${id}" aria-pressed="${id===period}"><span class="label">${title}</span>${id===period?icon("check"):""}</button>`).join(""));break;
    case "choose-month": closeSheet(false);period=key;render(true);break;
    case "reminder": viewedReminder=key;goto("reminder-detail");break;
    case "threshold": threshold=key;render(true);break;
    case "complete-reminder": notifyPreview("یادآور نمونه انجام شد.");break;
    case "cancel-reminder": confirmation("یادآور لغو شود؟","این یادآور در سوابق یادآورها با وضعیت لغوشده می‌ماند.",["cancel-confirm","لغو یادآور"],"danger-button");break;
    case "cancel-confirm": closeSheet(false);notifyPreview("یادآور نمونه لغو شد.");break;
    case "notice": notifyPreview(key==="vehicle"?"خودروی نمونه ذخیره شد.":"یادآور نمونه ثبت شد.");break;
    case "edit-vehicle": vehicleEditKey=key;vehicleDemo={name:vehicles[key].name,year:vehicles[key].year};vehicleCalendar="solar";goto("vehicle-edit");break;
    case "calendar": vehicleCalendar=key;render();document.querySelector(`[data-action="calendar:${key}"]`)?.focus();break;
    case "appearance":
      modal("ظاهر",Object.entries({system:"تنظیم گوشی",light:"روشن",dark:"تیره"}).map(([id,label])=>`<button type="button" class="picker-row" data-action="theme:${id}" aria-pressed="${id===theme}"><span class="label">${label}</span>${id===theme?icon("check"):""}</button>`).join(""));break;
    case "theme": closeSheet(false);setTheme(key);render(true);break;
    case "android-settings": document.querySelector("#status-message").textContent="در اندروید، تنظیمات اعلان گوشی باز می‌شود.";break;
    case "mock-picker":
      modal("نمونهٔ فایل‌گزین اندروید",`<p class="sheet-copy">در برنامه، فایل‌گزین گوشی باز می‌شود.</p><div class="sheet-actions">
        ${action(key==="export"?"picker-result:export-success":"picker-result:"+((route==="restore-invalid"||parents.includes("first"))?"restore-success":"restore-blocked"),"دیدن نتیجهٔ نمونه","primary")}
        ${key==="restore"?action("picker-result:restore-invalid","دیدن خطای فایل","secondary"):""}
        ${action("close","انصراف","secondary")}</div>`);break;
    case "picker-result": closeSheet(false);route=key;render(true);break;
    case "cloud-mode": cloudMode=key;render(true);break;
    case "cloud-replace": confirmation("پشتیبان آنلاین جایگزین شود؟","نسخهٔ فعلی تأییدشده با نسخهٔ تازه جایگزین می‌شود.",["close","ارسال نسخه جدید"]);break;
    case "cloud-restore":
      modal("بازیابی پشتیبان آنلاین",`<div class="stack sheet-copy">
        <p>بازیابی فقط در دفتر کاملاً خالی انجام می‌شود.</p><div class="list">
        <div class="item"><span>زمان تأیید</span><span>۲۵ آبان ۱۴۰۵</span></div>
        <div class="item"><span>انقضا</span><span>۲۵ بهمن ۱۴۰۵</span></div>
        <div class="item"><span>شناسه نسخه</span><bdi dir="ltr">demo-r17</bdi></div></div></div>
        <div class="sheet-actions">${action("close","بازیابی این نسخه","primary")}${action("close","انصراف","secondary")}</div>`);break;
    case "cloud-recovery":
      modalTrigger=document.activeElement;
      sheet.dataset.protected="";
      sheet.innerHTML='<div class="stack"><h2 id="sheet-title">کد بازیابی</h2><p>این کد نمونه فقط یک بار نمایش داده می‌شود.</p><p class="meta">کد نمونه</p><bdi dir="ltr">DEMO-0000</bdi></div><div class="sheet-actions">'+action("close","کد را یادداشت کردم","primary")+'</div>';
      sheet.showModal();sheet.querySelector("button").focus();break;
  }
}

const samples = {
  selector:["home","new"], "vehicle-switch":["home","switch"], "record-edit":["record","edit-record"], delete:["record","delete"],
  month:["report","month"], "reminder-mileage":["reminder-create"], "reminder-cancel":["reminder-detail","cancel-reminder"],
  appearance:["settings","appearance"], "cloud-restore":["cloud-bound","cloud-restore"], "cloud-recovery":["cloud-bound","cloud-recovery"],
};
document.addEventListener("input",event=>{
  if (event.target.id==="vehicle-name") vehicleDemo.name=event.target.value;
  if (event.target.id==="vehicle-year") {
    vehicleDemo.year=event.target.value;
    if (!vehicleDemo.year.trim()) vehicleCalendar="";
    document.querySelector("#vehicle-calendar").hidden=!vehicleDemo.year.trim();
  }
});

if (scene==="index") {
  root.innerHTML=`<main id="preview-index"><h1>پیش‌نمایش دفتر خودرو</h1><p class="meta">همهٔ نماها با داده‌های آزمایشی</p><nav aria-label="نماهای پیش‌نمایش">
    ${Object.entries(sceneLabels).map(([id,title])=>`<a href="?scene=${id}">${title}</a>`).join("")}
    <a href="?theme=dark">خانه در حالت تیره</a><a href="?scene=fuel-keyboard&amp;text=150">سوخت در اندازه متن ۱۵۰٪</a></nav></main>`;
} else {
  route=samples[scene]?.[0] || (Object.hasOwn(sceneLabels,scene)?scene:"home");
  if (mode) route="home";
  if (scene==="vehicle-edit") vehicleDemo={name:vehicles[vehicleEditKey].name,year:vehicles[vehicleEditKey].year};
  if (["report","report-empty","record","record-edit","delete","month","history-empty","history-error","loading"].includes(scene)) activeTab="history";
  if (scene.startsWith("reminder")) activeTab="reminders";
  if (scene==="reminder-mileage") threshold="mileage";
  setTheme(theme);
  render();
  const trigger=samples[scene]?.[1];
  if (trigger==="new") entrySelector();
  else if (trigger==="switch") vehicleSelector();
  else if (trigger) extensionAction(trigger);
}
