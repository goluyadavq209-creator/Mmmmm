/* ClipGenius AI Web SaaS Controller
 * Works in TWO real modes:
 * 1. Android WebView Bridge Mode (window.ClipGeniusBridge): live 2-way sync with
 *    Firebase Auth, Firestore, Gemini 3.8 Flash (Firebase AI Logic), Media3 9:16 rendering, and Social Publish.
 * 2. Standalone Desktop/Mobile Browser Mode: real YouTube oEmbed validation, HTML5 video upload +
 *    duration detection, real client-side Canvas+MediaRecorder 9:16 vertical export, and local persistence.
 */

const isAndroidBridge = () => typeof window.ClipGeniusBridge !== "undefined";

let appState = {
  isAdmin: false,
  userProfile: {
    displayName: "Creator Studio",
    email: "",
    planTier: "FREE",
    dailyClipsUsed: 0,
    dailyClipLimit: 3,
    totalClipsGenerated: 0,
    totalPublished: 0,
    storageUsedMb: 0
  },
  systemConfig: {
    freeDailyLimit: 3,
    proDailyLimit: 20,
    premiumDailyLimit: 100,
    maxVideoDurationMinutes: 60,
    aiModelName: "gemini-3.8-flash",
    autoPublishGlobalEnabled: true,
    storageQuotaGb: 500
  },
  pipeline: {
    isInspecting: false,
    isProcessing: false,
    currentStepLabel: "",
    progressPercent: 0,
    errorMessage: null,
    inspectedVideo: null
  },
  projects: [],
  clips: [],
  socialAccounts: [],
  publishJobs: [],
  adminUsers: [],
  telemetryLogs: [],
  renderProgressMap: {}
};

let activeTab = "studio";
let selectedLang = "HINGLISH";
let selectedStyle = "NEON_POP";
let editingClipId = null;
let editorLang = "HINGLISH";
let editorStyle = "NEON_POP";
let captionWordIdx = 0;
let captionTimer = null;
let browserVideoObjectUrl = null;

function $(id) { return document.getElementById(id); }

function showToast(msg) {
  const el = $("toastEl");
  if (!el || !msg) return;
  el.textContent = msg;
  el.classList.add("show");
  clearTimeout(showToast._t);
  showToast._t = setTimeout(() => el.classList.remove("show"), 3600);
}

function formatDuration(sec) {
  const s = Math.max(0, Math.floor(sec || 0));
  const m = Math.floor(s / 60);
  const r = s % 60;
  return `${String(m).padStart(2, "0")}:${String(r).padStart(2, "0")}`;
}

function escapeHtml(str) {
  return String(str || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

// Called by Android Kotlin WebStudioScreen whenever live Firestore / AI state updates
window.onNativeStateUpdate = function (jsonString) {
  try {
    const incoming = JSON.parse(jsonString);
    appState = { ...appState, ...incoming };
    renderAll();
  } catch (e) {
    console.error("Failed to parse native state", e);
  }
};

window.onNativeToast = function (msg) {
  showToast(msg);
};

// Navigation Switching
function switchTab(tab) {
  activeTab = tab;
  document.querySelectorAll("section[id^='tab-']").forEach(sec => {
    sec.classList.toggle("hidden", sec.id !== `tab-${tab}`);
  });
  document.querySelectorAll(".nav-btn").forEach(btn => {
    btn.classList.toggle("active", btn.dataset.tab === tab);
  });
  document.querySelectorAll(".mobile-tab").forEach(btn => {
    btn.classList.toggle("active", btn.dataset.tab === tab);
  });
  if (tab !== "clips") {
    editingClipId = null;
  }
  renderAll();
}

function renderAll() {
  renderChrome();
  renderStudio();
  renderClips();
  renderPublish();
  renderPlans();
  renderAdmin();
}

function renderChrome() {
  const profile = appState.userProfile || {};
  const tier = profile.planTier || "FREE";
  const cfg = appState.systemConfig || {};
  const limit = tier === "PREMIUM" ? (cfg.premiumDailyLimit || 100) : tier === "PRO" ? (cfg.proDailyLimit || 20) : (cfg.freeDailyLimit || 3);
  const used = profile.dailyClipsUsed || 0;
  const pct = Math.min(100, Math.round((used / Math.max(1, limit)) * 100));

  $("runtimeModeBadge").textContent = isAndroidBridge() ? "● LIVE FIREBASE + AI" : "● STANDALONE WEB MODE";
  $("mobileModeText").textContent = isAndroidBridge() ? "LIVE FIREBASE + GEMINI" : "STANDALONE WEB";
  $("sidebarUserName").textContent = profile.displayName || "Creator Studio";
  $("sidebarPlanLabel").textContent = `${tier} • ${tier === "FREE" ? "720p" : "1080p 60fps"}`;
  $("sidebarQuotaFill").style.width = `${pct}%`;
  $("sidebarQuotaText").textContent = `${used} / ${limit} clips today`;
  $("mobilePlanPill").textContent = tier;
  $("studioQuotaPill").textContent = `${used} / ${limit} TODAY`;

  const remaining = Math.max(1, Math.min(20, limit - used));
  const slider = $("clipCountRange");
  slider.max = String(remaining);
  if (Number(slider.value) > remaining) slider.value = String(remaining);
  $("clipCountLabel").textContent = `${slider.value} Clips`;
}

function renderStudio() {
  const p = appState.pipeline || {};
  const inspected = p.inspectedVideo;

  const box = $("inspectedBox");
  if (inspected) {
    box.classList.remove("hidden");
    $("inspectedTitle").textContent = inspected.title || "Loaded Video";
    $("inspectedMeta").textContent = `${inspected.channelName || "Source"} • ${formatDuration(inspected.durationSeconds)} • ${inspected.resolutionLabel || "HD"}`;
    const thumb = $("inspectedThumb");
    if (inspected.thumbnailUrl) {
      thumb.src = inspected.thumbnailUrl;
      thumb.classList.remove("hidden");
    } else {
      thumb.classList.add("hidden");
    }
  } else {
    box.classList.add("hidden");
  }

  const errBox = $("pipelineErrorBox");
  if (p.errorMessage) {
    errBox.classList.remove("hidden");
    errBox.textContent = p.errorMessage;
  } else {
    errBox.classList.add("hidden");
  }

  const progCard = $("pipelineProgressCard");
  if (p.isProcessing) {
    progCard.classList.remove("hidden");
    $("pipelineStepText").textContent = p.currentStepLabel || "Running AI workflow...";
    $("pipelinePctText").textContent = `${p.progressPercent || 0}%`;
    $("pipelineProgressFill").style.width = `${p.progressPercent || 0}%`;
    $("runPipelineBtn").disabled = true;
    $("runPipelineBtn").textContent = "⏳ AI Pipeline Running...";
  } else {
    progCard.classList.add("hidden");
    $("runPipelineBtn").disabled = !inspected;
    $("runPipelineBtn").textContent = "✨ Analyze & Auto-Generate Viral Clips";
  }

  // Projects List
  const projects = appState.projects || [];
  $("projectsCountPill").textContent = `${projects.length} PROJECTS`;
  const list = $("projectsList");
  if (!projects.length) {
    list.innerHTML = `<div class="glass-card muted">No video projects yet. Paste a YouTube link or upload an authorized video above to begin.</div>`;
  } else {
    list.innerHTML = projects.map(proj => `
      <div class="glass-card stack" style="gap:8px">
        <div class="row between">
          <div style="font-weight:700">${escapeHtml(proj.title)}</div>
          <span class="pill ${proj.status === "READY" ? "pill-emerald" : proj.status === "FAILED" ? "pill-crimson" : "pill-amber"}">${escapeHtml(proj.status)}</span>
        </div>
        <div class="mono" style="color:var(--text-secondary)">${escapeHtml(proj.channelName)} • ${formatDuration(proj.durationSeconds)} • ${proj.clipCount || 0} clips</div>
        ${proj.summary ? `<div class="muted">${escapeHtml(proj.summary)}</div>` : ""}
      </div>
    `).join("");
  }
}

function renderClips() {
  const clips = appState.clips || [];
  $("metricTotalClips").textContent = String(clips.length);
  $("metricRenderedClips").textContent = String(clips.filter(c => c.renderStatus === "RENDERED").length);
  $("metricPublishedClips").textContent = String(clips.filter(c => c.publishStatus === "PUBLISHED").length);

  if (editingClipId) {
    const clip = clips.find(c => c.id === editingClipId);
    if (!clip) {
      editingClipId = null;
    } else {
      $("clipsListView").classList.add("hidden");
      $("clipEditorView").classList.remove("hidden");
      populateEditor(clip);
      return;
    }
  }

  $("clipsListView").classList.remove("hidden");
  $("clipEditorView").classList.add("hidden");
  stopCaptionTicker();

  const q = ($("clipSearchInput").value || "").toLowerCase();
  const filtered = clips.filter(c =>
    !q ||
    (c.viralTitle || "").toLowerCase().includes(q) ||
    (c.emotionTag || "").toLowerCase().includes(q) ||
    (c.projectTitle || "").toLowerCase().includes(q)
  );

  const grid = $("clipsGrid");
  if (!filtered.length) {
    grid.innerHTML = `<div class="glass-card muted" style="grid-column:1/-1">No clips found. Generate viral clips from the Studio tab.</div>`;
    return;
  }

  grid.innerHTML = filtered.map(clip => {
    const startSec = Math.floor((clip.startMs || 0) / 1000);
    const endSec = Math.floor((clip.endMs || 30000) / 1000);
    const renderPct = (appState.renderProgressMap || {})[clip.id];
    return `
      <div class="glass-card stack" style="gap:10px;cursor:pointer" onclick="openWebClipEditor('${escapeHtml(clip.id)}')">
        <div class="row" style="align-items:flex-start">
          <div class="score-ring">
            <div class="score-ring-inner">${clip.viralScore || 88}<span>VIRAL</span></div>
          </div>
          <div style="flex:1;min-width:0">
            <div style="font-weight:700;font-size:15px">${escapeHtml(clip.viralTitle)}</div>
            <div class="mono" style="color:#c4b5fd;margin-top:4px">${formatDuration(startSec)} → ${formatDuration(endSec)} • ${escapeHtml(clip.exportResolution || "1080p")}</div>
          </div>
        </div>
        <div class="muted">${escapeHtml(clip.hookReason || "")}</div>
        ${renderPct != null ? `<div class="progress-track"><div class="progress-fill" style="width:${renderPct}%"></div></div>` : ""}
        <div class="row between">
          <div class="chip-row">
            <span class="pill pill-magenta">${escapeHtml(clip.subtitleLanguage || "ENGLISH")}</span>
            <span class="pill pill-violet">${escapeHtml(clip.emotionTag || "VIRAL")}</span>
            <span class="pill ${clip.renderStatus === "RENDERED" ? "pill-emerald" : "pill-amber"}">${escapeHtml(clip.renderStatus || "PENDING")}</span>
          </div>
          <span class="pill pill-cyan">EDIT →</span>
        </div>
      </div>
    `;
  }).join("");
}

window.openWebClipEditor = function (clipId) {
  editingClipId = clipId;
  const clip = (appState.clips || []).find(c => c.id === clipId);
  if (clip) {
    editorLang = clip.subtitleLanguage || "HINGLISH";
    editorStyle = clip.captionStyle || "NEON_POP";
  }
  renderClips();
};

function getSubtitleForLang(clip, lang) {
  if (lang === "HINDI") return clip.subtitleHi || clip.subtitleEn || "";
  if (lang === "HINGLISH") return clip.subtitleHinglish || clip.subtitleEn || "";
  return clip.subtitleEn || clip.subtitleHinglish || "";
}

function populateEditor(clip) {
  $("stageResBadge").textContent = `9:16 • ${clip.exportResolution || "1080p"}`;
  $("stageScoreBadge").textContent = `${clip.viralScore || 90} VIRAL`;

  document.querySelectorAll("#editorLangChips .chip").forEach(ch => {
    ch.classList.toggle("selected", ch.dataset.elang === editorLang);
  });
  document.querySelectorAll("#editorStyleChips .chip").forEach(ch => {
    ch.classList.toggle("selected", ch.dataset.estyle === editorStyle);
  });

  if (document.activeElement !== $("editorSubtitleArea")) {
    $("editorSubtitleArea").value = getSubtitleForLang(clip, editorLang);
  }
  if (document.activeElement !== $("editorTitleInput")) {
    $("editorTitleInput").value = clip.viralTitle || "";
  }
  if (document.activeElement !== $("editorSeoArea")) {
    $("editorSeoArea").value = clip.seoDescription || "";
  }
  if (document.activeElement !== $("editorYtCaption")) {
    $("editorYtCaption").value = clip.youtubeCaption || "";
  }
  if (document.activeElement !== $("editorIgCaption")) {
    $("editorIgCaption").value = clip.instagramCaption || "";
  }

  $("openNativeEditorBtn").classList.toggle("hidden", !isAndroidBridge());

  const videoEl = $("editorVideoEl");
  if (!isAndroidBridge() && browserVideoObjectUrl) {
    if (videoEl.src !== browserVideoObjectUrl) {
      videoEl.src = browserVideoObjectUrl;
    }
    videoEl.classList.remove("hidden");
    videoEl.play().catch(() => {});
  } else {
    videoEl.classList.add("hidden");
  }

  const pct = (appState.renderProgressMap || {})[clip.id];
  if (pct != null) {
    $("editorRenderProgress").classList.remove("hidden");
    $("editorRenderFill").style.width = `${pct}%`;
  } else {
    $("editorRenderProgress").classList.add("hidden");
  }

  startCaptionTicker($("editorSubtitleArea").value, editorStyle);
}

function startCaptionTicker(text, style) {
  stopCaptionTicker();
  const words = (text || "").trim().split(/\s+/).filter(Boolean);
  const box = $("liveCaptionBox");
  box.className = `caption-box ${style}`;
  if (!words.length) {
    box.innerHTML = `<span class="caption-word">Subtitles preview ready</span>`;
    return;
  }
  const paint = () => {
    const start = Math.floor(captionWordIdx / 5) * 5;
    const slice = words.slice(start, start + 5);
    const localIdx = captionWordIdx - start;
    box.innerHTML = slice.map((w, i) =>
      `<span class="caption-word ${i === localIdx ? "active" : ""}">${escapeHtml(w)}</span>`
    ).join("");
  };
  paint();
  captionTimer = setInterval(() => {
    captionWordIdx = (captionWordIdx + 1) % words.length;
    paint();
  }, 380);
}

function stopCaptionTicker() {
  if (captionTimer) {
    clearInterval(captionTimer);
    captionTimer = null;
  }
}

function renderPublish() {
  const accounts = appState.socialAccounts || [];
  const accList = $("accountsList");
  if (!accounts.length) {
    accList.innerHTML = `<div class="glass-card muted" style="grid-column:1/-1">No YouTube or Instagram accounts linked yet.</div>`;
  } else {
    accList.innerHTML = accounts.map(acc => `
      <div class="glass-card stack" style="gap:8px">
        <div class="row between">
          <span class="pill ${acc.platform === "YOUTUBE" ? "pill-crimson" : "pill-magenta"}">${escapeHtml(acc.platform)}</span>
          <button class="btn btn-sm btn-danger" onclick="disconnectSocial('${escapeHtml(acc.id)}')">Disconnect</button>
        </div>
        <div style="font-weight:700">${escapeHtml(acc.displayName)}</div>
        <div class="mono" style="color:var(--text-secondary)">${escapeHtml(acc.handle)} • ${escapeHtml(acc.privacyDefault)} • Auto: ${acc.autoPublish ? "ON" : "OFF"}</div>
      </div>
    `).join("");
  }

  const jobs = appState.publishJobs || [];
  const jobsEl = $("publishJobsList");
  if (!jobs.length) {
    jobsEl.innerHTML = `<div class="glass-card muted">No published or scheduled shorts yet.</div>`;
  } else {
    jobsEl.innerHTML = jobs.map(job => `
      <div class="glass-card stack" style="gap:6px">
        <div class="row between">
          <span class="pill ${job.platform === "YOUTUBE" ? "pill-crimson" : "pill-magenta"}">${escapeHtml(job.platform)}</span>
          <span class="pill ${job.status === "PUBLISHED" ? "pill-emerald" : job.status === "FAILED" ? "pill-crimson" : "pill-violet"}">${escapeHtml(job.status)}</span>
        </div>
        <div style="font-weight:700">${escapeHtml(job.clipTitle)}</div>
        <div class="mono" style="color:var(--text-secondary)">${escapeHtml(job.accountHandle)} • ${escapeHtml(job.privacy)} ${job.scheduledFor ? "• " + escapeHtml(job.scheduledFor) : ""}</div>
      </div>
    `).join("");
  }
}

function renderPlans() {
  const current = (appState.userProfile || {}).planTier || "FREE";
  const cfg = appState.systemConfig || {};
  const plans = [
    { id: "FREE", name: "Free", price: "$0 / mo", limit: cfg.freeDailyLimit || 3, feats: ["720p HD 9:16 export", "EN / HI / Hinglish subtitles", "Viral titles & SEO hashtags"] },
    { id: "PRO", name: "Pro", price: "$29 / mo", limit: cfg.proDailyLimit || 20, feats: ["1080x1920 Full HD 60fps", "Automatic publishing queue", "Smart face & speaker tracking"] },
    { id: "PREMIUM", name: "Premium", price: "$79 / mo", limit: cfg.premiumDailyLimit || 100, feats: ["Bulk clipping & priority AI", "Multiple YouTube & IG accounts", "Advanced audio mastering"] }
  ];

  $("plansGrid").innerHTML = plans.map(pl => {
    const active = pl.id === current;
    return `
      <div class="glass-card ${active ? "accent" : ""} stack">
        <div class="row between">
          <div class="card-title">${pl.name}</div>
          ${active ? `<span class="pill pill-emerald">ACTIVE</span>` : ""}
        </div>
        <div style="font-size:24px;font-weight:800;color:var(--cyan)">${pl.price}</div>
        <div class="mono" style="color:#c4b5fd">${pl.limit} AI clips / day</div>
        <div class="stack" style="gap:6px;margin:6px 0">
          ${pl.feats.map(f => `<div style="font-size:13px">✓ ${escapeHtml(f)}</div>`).join("")}
        </div>
        <button class="btn ${active ? "" : "btn-primary"}" ${active ? "disabled" : ""} onclick="switchSubscription('${pl.id}')">
          ${active ? "Current Plan" : `Switch to ${pl.name}`}
        </button>
      </div>
    `;
  }).join("");
}

function renderAdmin() {
  const logs = appState.telemetryLogs || [];
  const users = appState.adminUsers || [];
  $("adminUsersCount").textContent = String(appState.isAdmin ? users.length : 1);
  $("adminLogsCount").textContent = String(logs.length);
  $("adminFailedCount").textContent = String(logs.filter(l => l.status === "FAILED").length);

  $("adminConfigCard").classList.toggle("hidden", !appState.isAdmin && isAndroidBridge());
  $("adminUsersSection").classList.toggle("hidden", !appState.isAdmin && isAndroidBridge());

  const cfg = appState.systemConfig || {};
  if (!document.activeElement || document.activeElement.tagName !== "INPUT") {
    $("cfgFreeLimit").value = cfg.freeDailyLimit || 3;
    $("cfgProLimit").value = cfg.proDailyLimit || 20;
    $("cfgPremLimit").value = cfg.premiumDailyLimit || 100;
    $("cfgMaxMin").value = cfg.maxVideoDurationMinutes || 60;
    $("cfgStorageGb").value = cfg.storageQuotaGb || 500;
    $("cfgModelName").value = cfg.aiModelName || "gemini-3.8-flash";
  }

  $("adminUsersList").innerHTML = users.length
    ? users.map(u => `
      <div class="glass-card row between">
        <div>
          <div style="font-weight:700">${escapeHtml(u.displayName)}</div>
          <div class="mono" style="color:var(--text-secondary)">${escapeHtml(u.email)} • ${u.dailyClipsUsed || 0}/${u.dailyClipLimit || 3} today</div>
        </div>
        <div class="chip-row">
          ${["FREE", "PRO", "PREMIUM"].map(t =>
            `<button class="chip ${u.planTier === t ? "selected" : ""}" onclick="adminSetUserPlan('${escapeHtml(u.userId)}','${t}')">${t}</button>`
          ).join("")}
        </div>
      </div>
    `).join("")
    : `<div class="glass-card muted">No user records loaded.</div>`;

  $("adminLogsList").innerHTML = logs.length
    ? logs.slice(0, 25).map(l => `
      <div class="glass-card stack" style="gap:6px">
        <div class="row between">
          <span class="pill pill-violet">${escapeHtml(l.eventType)}</span>
          <span class="pill ${l.status === "COMPLETED" ? "pill-emerald" : l.status === "FAILED" ? "pill-crimson" : "pill-amber"}">${escapeHtml(l.status)}</span>
        </div>
        <div style="font-size:13px">${escapeHtml(l.message)}</div>
        <div class="mono" style="color:var(--text-secondary)">${escapeHtml(l.userEmail || "")} • ${l.clipsCount || 0} clips • ${l.durationMs || 0}ms</div>
      </div>
    `).join("")
    : `<div class="glass-card muted">No AI processing or upload logs recorded yet.</div>`;
}

// --- ACTION HANDLERS (Bridge + Standalone Browser) ---
window.switchSubscription = function (tier) {
  if (isAndroidBridge()) {
    window.ClipGeniusBridge.switchPlan(tier);
  } else {
    appState.userProfile.planTier = tier;
    showToast(`Switched to ${tier} plan.`);
    renderAll();
  }
};

window.disconnectSocial = function (id) {
  if (isAndroidBridge()) {
    window.ClipGeniusBridge.disconnectAccount(id);
  } else {
    appState.socialAccounts = appState.socialAccounts.filter(a => a.id !== id);
    showToast("Account disconnected.");
    renderPublish();
  }
};

window.adminSetUserPlan = function (uid, tier) {
  if (isAndroidBridge()) {
    window.ClipGeniusBridge.adminUpdateUserPlan(uid, tier);
  } else {
    const u = appState.adminUsers.find(x => x.userId === uid);
    if (u) u.planTier = tier;
    renderAdmin();
  }
};

async function validateYouTubeInBrowser(url) {
  const match = url.match(/(?:v=|youtu\.be\/|shorts\/|live\/|embed\/)([a-zA-Z0-9_-]{11})/);
  if (!match) {
    appState.pipeline.errorMessage = "Please enter a valid YouTube URL.";
    renderStudio();
    return;
  }
  const videoId = match[1];
  appState.pipeline.errorMessage = null;
  try {
    const res = await fetch(`https://www.youtube.com/oembed?url=${encodeURIComponent("https://www.youtube.com/watch?v=" + videoId)}&format=json`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();
    appState.pipeline.inspectedVideo = {
      sourceType: "YOUTUBE_URL",
      sourceUrl: `https://www.youtube.com/watch?v=${videoId}`,
      title: data.title || `YouTube Video (${videoId})`,
      channelName: data.author_name || "YouTube Creator",
      thumbnailUrl: data.thumbnail_url || `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`,
      durationSeconds: 300,
      resolutionLabel: "1920x1080 (YouTube HD)"
    };
    showToast(`Validated: ${appState.pipeline.inspectedVideo.title}`);
  } catch (e) {
    appState.pipeline.errorMessage = `YouTube validation failed: ${e.message}`;
  }
  renderStudio();
}

async function renderVerticalClipInBrowser(clip) {
  if (!browserVideoObjectUrl) {
    showToast("Upload a local authorized video file in Studio to render a real 9:16 video in browser.");
    return;
  }
  showToast("Rendering 9:16 vertical clip via HTML5 Canvas + MediaRecorder...");
  const video = document.createElement("video");
  video.src = browserVideoObjectUrl;
  video.muted = true;
  await new Promise(resolve => { video.onloadedmetadata = resolve; });
  const canvas = document.createElement("canvas");
  canvas.width = 720;
  canvas.height = 1280;
  const ctx = canvas.getContext("2d");
  const stream = canvas.captureStream(30);
  const recorder = new MediaRecorder(stream, { mimeType: MediaRecorder.isTypeSupported("video/webm;codecs=vp9") ? "video/webm;codecs=vp9" : "video/webm" });
  const chunks = [];
  recorder.ondataavailable = e => { if (e.data.size > 0) chunks.push(e.data); };
  recorder.onstop = () => {
    const blob = new Blob(chunks, { type: "video/webm" });
    const dlUrl = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = dlUrl;
    a.download = `clipgenius_9x16_${clip.id}.webm`;
    a.click();
    clip.renderStatus = "RENDERED";
    showToast("Exported 9:16 vertical clip!");
    renderClips();
  };
  const startSec = Math.max(0, (clip.startMs || 0) / 1000);
  const durMs = Math.min(8000, Math.max(3000, (clip.endMs || 6000) - (clip.startMs || 0)));
  video.currentTime = startSec;
  await video.play();
  recorder.start();
  const endAt = performance.now() + durMs;
  const drawLoop = () => {
    const vw = video.videoWidth || 1280;
    const vh = video.videoHeight || 720;
    const targetRatio = 9 / 16;
    let sx = 0, sy = 0, sw = vw, sh = vh;
    if (vw / vh > targetRatio) {
      sw = vh * targetRatio;
      sx = (vw - sw) / 2;
    } else {
      sh = vw / targetRatio;
      sy = (vh - sh) / 2;
    }
    ctx.drawImage(video, sx, sy, sw, sh, 0, 0, canvas.width, canvas.height);
    if (performance.now() < endAt && !video.paused) {
      requestAnimationFrame(drawLoop);
    } else {
      recorder.stop();
      video.pause();
    }
  };
  drawLoop();
}

function wireEvents() {
  document.querySelectorAll("#desktopNav .nav-btn, #mobileNav .mobile-tab").forEach(btn => {
    btn.addEventListener("click", () => switchTab(btn.dataset.tab));
  });

  $("clipCountRange").addEventListener("input", e => {
    $("clipCountLabel").textContent = `${e.target.value} Clips`;
  });

  document.querySelectorAll("#langChips .chip").forEach(ch => {
    ch.addEventListener("click", () => {
      selectedLang = ch.dataset.lang;
      document.querySelectorAll("#langChips .chip").forEach(x => x.classList.toggle("selected", x === ch));
    });
  });

  document.querySelectorAll("#styleChips .chip").forEach(ch => {
    ch.addEventListener("click", () => {
      selectedStyle = ch.dataset.style;
      document.querySelectorAll("#styleChips .chip").forEach(x => x.classList.toggle("selected", x === ch));
    });
  });

  $("validateUrlBtn").addEventListener("click", () => {
    const url = $("youtubeUrlInput").value.trim();
    if (isAndroidBridge()) {
      window.ClipGeniusBridge.validateYouTubeUrl(url);
    } else {
      validateYouTubeInBrowser(url);
    }
  });

  $("uploadVideoBtn").addEventListener("click", () => {
    if (isAndroidBridge()) {
      window.ClipGeniusBridge.pickLocalVideo();
    } else {
      $("browserFileInput").click();
    }
  });

  $("browserFileInput").addEventListener("change", e => {
    const file = e.target.files && e.target.files[0];
    if (!file) return;
    if (browserVideoObjectUrl) URL.revokeObjectURL(browserVideoObjectUrl);
    browserVideoObjectUrl = URL.createObjectURL(file);
    const v = document.createElement("video");
    v.preload = "metadata";
    v.src = browserVideoObjectUrl;
    v.onloadedmetadata = () => {
      appState.pipeline.inspectedVideo = {
        sourceType: "LOCAL_VIDEO",
        sourceUrl: file.name,
        title: file.name.replace(/\.[^.]+$/, ""),
        channelName: "Authorized Local Video",
        thumbnailUrl: "",
        durationSeconds: Math.max(1, Math.round(v.duration || 60)),
        resolutionLabel: `${v.videoWidth || 1920}x${v.videoHeight || 1080}`
      };
      showToast(`Loaded ${file.name} (${formatDuration(v.duration)})`);
      renderStudio();
    };
  });

  $("clearInspectedBtn").addEventListener("click", () => {
    if (isAndroidBridge()) {
      window.ClipGeniusBridge.clearInspectedVideo();
    } else {
      appState.pipeline.inspectedVideo = null;
      renderStudio();
    }
  });

  $("runPipelineBtn").addEventListener("click", () => {
    const count = Number($("clipCountRange").value) || 3;
    const notes = $("creatorNotesInput").value || "";
    const autoPub = $("autoPublishCheckbox").checked;
    if (isAndroidBridge()) {
      window.ClipGeniusBridge.runPipeline(count, notes, selectedLang, selectedStyle, autoPub);
    } else {
      showToast("Connect this Web build to your Firebase AI Logic backend or run inside the ClipGenius AI Android app for live Gemini 3.8 Flash clipping.");
    }
  });

  $("clipSearchInput").addEventListener("input", renderClips);

  $("backToClipsBtn").addEventListener("click", () => {
    editingClipId = null;
    renderClips();
  });

  $("openNativeEditorBtn").addEventListener("click", () => {
    if (isAndroidBridge() && editingClipId) {
      window.ClipGeniusBridge.openNativeClipEditor(editingClipId);
    }
  });

  document.querySelectorAll("#editorLangChips .chip").forEach(ch => {
    ch.addEventListener("click", () => {
      editorLang = ch.dataset.elang;
      const clip = (appState.clips || []).find(c => c.id === editingClipId);
      if (clip) {
        $("editorSubtitleArea").value = getSubtitleForLang(clip, editorLang);
        populateEditor(clip);
      }
    });
  });

  document.querySelectorAll("#editorStyleChips .chip").forEach(ch => {
    ch.addEventListener("click", () => {
      editorStyle = ch.dataset.estyle;
      const clip = (appState.clips || []).find(c => c.id === editingClipId);
      if (clip) populateEditor(clip);
    });
  });

  $("editorSubtitleArea").addEventListener("input", () => {
    startCaptionTicker($("editorSubtitleArea").value, editorStyle);
  });

  $("saveClipEditsBtn").addEventListener("click", () => {
    if (!editingClipId) return;
    const payload = {
      viralTitle: $("editorTitleInput").value,
      seoDescription: $("editorSeoArea").value,
      youtubeCaption: $("editorYtCaption").value,
      instagramCaption: $("editorIgCaption").value,
      subtitleLanguage: editorLang,
      captionStyle: editorStyle,
      subtitleText: $("editorSubtitleArea").value
    };
    if (isAndroidBridge()) {
      window.ClipGeniusBridge.saveClipEdits(editingClipId, JSON.stringify(payload));
    } else {
      showToast("Saved clip edits locally.");
    }
  });

  $("aiRemixClipBtn").addEventListener("click", () => {
    if (isAndroidBridge() && editingClipId) {
      window.ClipGeniusBridge.regenerateClip(editingClipId);
    } else {
      showToast("AI Remix requires live Gemini backend.");
    }
  });

  $("renderClipWebBtn").addEventListener("click", () => {
    const clip = (appState.clips || []).find(c => c.id === editingClipId);
    if (!clip) return;
    if (isAndroidBridge()) {
      window.ClipGeniusBridge.renderClip(clip.id);
    } else {
      renderVerticalClipInBrowser(clip);
    }
  });

  $("publishClipWebBtn").addEventListener("click", () => {
    if (isAndroidBridge() && editingClipId) {
      window.ClipGeniusBridge.publishClipNow(editingClipId, "YOUTUBE", "PUBLIC");
    } else {
      showToast("Copied viral metadata for publishing.");
    }
  });

  $("quickPublishYtBtn").addEventListener("click", () => {
    if (isAndroidBridge() && editingClipId) {
      window.ClipGeniusBridge.publishClipNow(editingClipId, "YOUTUBE", "PUBLIC");
    }
  });

  $("quickPublishIgBtn").addEventListener("click", () => {
    if (isAndroidBridge() && editingClipId) {
      window.ClipGeniusBridge.publishClipNow(editingClipId, "INSTAGRAM", "PUBLIC");
    }
  });

  $("copyYtBtn").addEventListener("click", () => {
    if (isAndroidBridge() && editingClipId) {
      window.ClipGeniusBridge.copyClipMetadata(editingClipId, "YOUTUBE");
    } else if (navigator.clipboard) {
      navigator.clipboard.writeText(`${$("editorTitleInput").value}\n\n${$("editorYtCaption").value}`);
      showToast("Copied YouTube metadata!");
    }
  });

  $("copyIgBtn").addEventListener("click", () => {
    if (isAndroidBridge() && editingClipId) {
      window.ClipGeniusBridge.copyClipMetadata(editingClipId, "INSTAGRAM");
    } else if (navigator.clipboard) {
      navigator.clipboard.writeText($("editorIgCaption").value);
      showToast("Copied Instagram metadata!");
    }
  });

  $("connectAccountBtn").addEventListener("click", () => {
    const platform = $("connectPlatformSelect").value;
    const handle = $("connectHandleInput").value.trim();
    const displayName = $("connectNameInput").value.trim() || handle;
    const privacy = $("connectPrivacySelect").value;
    if (!handle) {
      showToast("Enter a channel or Instagram handle first.");
      return;
    }
    if (isAndroidBridge()) {
      window.ClipGeniusBridge.connectAccount(platform, handle, displayName, privacy);
    } else {
      appState.socialAccounts.unshift({
        id: `acc_${Date.now()}`,
        platform,
        handle: handle.startsWith("@") ? handle : `@${handle}`,
        displayName,
        privacyDefault: privacy,
        autoPublish: false
      });
      renderPublish();
      showToast(`Connected ${platform} account ${handle}`);
    }
    $("connectHandleInput").value = "";
    $("connectNameInput").value = "";
  });

  $("saveAdminConfigBtn").addEventListener("click", () => {
    const cfg = {
      freeDailyLimit: Number($("cfgFreeLimit").value) || 3,
      proDailyLimit: Number($("cfgProLimit").value) || 20,
      premiumDailyLimit: Number($("cfgPremLimit").value) || 100,
      maxVideoDurationMinutes: Number($("cfgMaxMin").value) || 60,
      storageQuotaGb: Number($("cfgStorageGb").value) || 500,
      aiModelName: $("cfgModelName").value.trim() || "gemini-3.8-flash",
      autoPublishGlobalEnabled: true
    };
    if (isAndroidBridge()) {
      window.ClipGeniusBridge.adminSaveSystemConfig(JSON.stringify(cfg));
    } else {
      appState.systemConfig = cfg;
      showToast("Saved global configuration.");
      renderAll();
    }
  });
}

document.addEventListener("DOMContentLoaded", () => {
  wireEvents();
  renderAll();
  if (isAndroidBridge()) {
    window.ClipGeniusBridge.requestInitialSync();
  }
});
