const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_creator_01";
const BOB_UID = "bob_creator_02";
const ADMIN_UID = "admin_user_01";
const ADMIN_EMAIL = "maansinghyadav095@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(offsetMs = 2000) {
  return new Date(Date.now() - offsetMs);
}

function makeUserProfile(uid, email = "alice@example.com") {
  const ts = pastDate();
  return {
    userId: uid,
    displayName: "Alice Creator",
    email,
    planTier: "FREE",
    dailyClipsUsed: 0,
    dailyClipLimit: 3,
    autoPublishEnabled: false,
    preferredSubtitleLang: "ENGLISH",
    preferredCaptionStyle: "NEON_POP",
    totalClipsGenerated: 0,
    totalPublished: 0,
    storageUsedMb: 0.0,
    createdAt: ts,
    updatedAt: ts,
  };
}

function makeProject(uid) {
  const ts = pastDate();
  return {
    userId: uid,
    title: "How AI Changes Video Creation",
    sourceType: "YOUTUBE_URL",
    sourceUrl: "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
    channelName: "Tech Studio",
    thumbnailUrl: "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
    durationSeconds: 612,
    resolutionLabel: "1920x1080",
    status: "READY",
    progressPercent: 100,
    currentStage: "Completed",
    transcript: "Full transcript of the video content.",
    summary: "Key takeaways on AI video workflows.",
    clipCount: 1,
    createdAt: ts,
    updatedAt: ts,
  };
}

function makeClip(uid, projectId) {
  const ts = pastDate();
  return {
    userId: uid,
    projectId,
    projectTitle: "How AI Changes Video Creation",
    sourceUri: "",
    exportedFilePath: "",
    startMs: 12000,
    endMs: 48000,
    viralScore: 94,
    hookReason: "High-energy opening insight with strong retention hook.",
    emotionTag: "EXCITEMENT",
    viralTitle: "This AI Editing Trick Saves 10 Hours a Week!",
    seoDescription: "Discover how creators automate vertical video clipping.",
    youtubeCaption: "Stop manually cutting Shorts! #Shorts #AI",
    instagramCaption: "Automate your Reels workflow today! #Reels #Creator",
    thumbnailConcept: "Bold neon purple text: 10X FASTER EDITS over creator reaction",
    hashtags: ["#Shorts", "#AI", "#VideoEditing"],
    keywords: ["ai video editor", "youtube shorts", "viral clips"],
    subtitleLanguage: "HINGLISH",
    captionStyle: "NEON_POP",
    subtitleEn: "This single AI workflow turns one podcast into twenty viral clips.",
    subtitleHi: "यह एक AI वर्कफ़्लो एक पॉडकास्ट को बीस वायरल क्लिप्स में बदल देता है।",
    subtitleHinglish: "Yeh ek AI workflow ek podcast ko 20 viral clips mein badal deta hai.",
    aspectRatio: "9:16",
    exportResolution: "1080p",
    exportFps: 60,
    faceTrackingEnabled: true,
    speakerTrackingEnabled: true,
    dynamicZoomEnabled: true,
    noiseReductionEnabled: true,
    audioNormalized: true,
    transitionStyle: "ZOOM_PUNCH",
    renderStatus: "PENDING",
    publishStatus: "DRAFT",
    scheduledTimeIso: "",
    createdAt: ts,
    updatedAt: ts,
  };
}

function makeSocialAccount(uid) {
  const ts = pastDate();
  return {
    userId: uid,
    platform: "YOUTUBE",
    handle: "@alicecreates",
    displayName: "Alice Creates",
    channelOrAccountId: "UC1234567890",
    privacyDefault: "PUBLIC",
    autoPublish: false,
    connectedStatus: "CONNECTED",
    createdAt: ts,
    updatedAt: ts,
  };
}

function makePublishJob(uid, clipId) {
  const ts = pastDate();
  return {
    userId: uid,
    clipId,
    clipTitle: "This AI Editing Trick Saves 10 Hours a Week!",
    platform: "YOUTUBE",
    accountHandle: "@alicecreates",
    privacy: "PUBLIC",
    status: "SCHEDULED",
    scheduledFor: "2026-11-20T18:00:00Z",
    platformPostUrl: "",
    errorMessage: "",
    createdAt: ts,
    updatedAt: ts,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users are denied all reads and writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(makeUserProfile(ALICE_UID)));
  await assertFails(unauthDb.collection("system_config").doc("global").get());
  await assertFails(unauthDb.collection("admin_logs").get());
});

test("Owner can create, read, and partially update UserProfile; ghost fields & cross-user blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(makeUserProfile(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Partial plan upgrade update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      planTier: "PRO",
      dailyClipLimit: 20,
      updatedAt: pastDate(500),
    })
  );

  // Shadow update with ghost field fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(500),
    })
  );

  // Immutable createdAt change fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: pastDate(100),
      updatedAt: pastDate(100),
    })
  );

  // Cross-user read & write denied (PII isolation)
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ planTier: "PREMIUM", updatedAt: pastDate(200) }));
});

test("Owner project, clip, social account, and publish job lifecycle succeeds; orphan clips & terminal updates denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();

  // Orphan clip without parent project fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("clips").doc("clip_orphan").set(makeClip(ALICE_UID, "missing_proj"))
  );

  // Create project then clip
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").set(makeProject(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("clips").doc("clip_1").set(makeClip(ALICE_UID, "proj_1"))
  );

  // List subcollections matches Kotlin queries
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("clips").get());

  // Cross-user subcollection list/get denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("clips").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").get());

  // Update clip caption style & subtitle language
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("clips").doc("clip_1").update({
      subtitleLanguage: "HINDI",
      captionStyle: "KARAOKE_GLOW",
      updatedAt: pastDate(400),
    })
  );

  // Connect social account
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("social_accounts").doc("acc_yt_1").set(makeSocialAccount(ALICE_UID))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("social_accounts").get());

  // Create & publish job; once PUBLISHED, terminal state locks non-admin edits
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("publish_jobs").doc("job_1").set(makePublishJob(ALICE_UID, "clip_1"))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("publish_jobs").doc("job_1").update({
      status: "PUBLISHED",
      platformPostUrl: "https://youtube.com/shorts/demo",
      updatedAt: pastDate(300),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("publish_jobs").doc("job_1").update({
      status: "FAILED",
      updatedAt: pastDate(200),
    })
  );
});

test("Admin email spoofing fails without email_verified; verified admin manages config, users, and logs", async () => {
  const spoofAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  const configPayload = {
    freeDailyLimit: 3,
    proDailyLimit: 20,
    premiumDailyLimit: 100,
    maxVideoDurationMinutes: 60,
    aiModelName: "gemini-3.8-flash",
    autoPublishGlobalEnabled: true,
    storageQuotaGb: 500,
    updatedAt: pastDate(1000),
  };

  // Unverified spoof admin & regular user denied system_config writes
  await assertFails(spoofAdminDb.collection("system_config").doc("global").set(configPayload));
  await assertFails(aliceDb.collection("system_config").doc("global").set(configPayload));

  // Verified admin succeeds
  await assertSucceeds(realAdminDb.collection("system_config").doc("global").set(configPayload));
  await assertSucceeds(aliceDb.collection("system_config").doc("global").get());

  // User creates own telemetry log; filtered query works, unfiltered fails for non-admin, succeeds for admin
  await assertSucceeds(
    aliceDb.collection("admin_logs").doc("log_1").set({
      userId: ALICE_UID,
      userEmail: "alice@example.com",
      eventType: "AI_ANALYSIS",
      status: "COMPLETED",
      message: "Generated 8 viral clips",
      clipsCount: 8,
      durationMs: 4200,
      createdAt: pastDate(1000),
    })
  );
  await assertSucceeds(aliceDb.collection("admin_logs").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("admin_logs").get());
  await assertSucceeds(realAdminDb.collection("admin_logs").get());
  await assertSucceeds(realAdminDb.collection("users").get());
});
