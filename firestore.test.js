const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

function validProfilePayload(uid, now) {
  return {
    userId: uid,
    displayName: "NeonPilot",
    bestEndlessScore: 0,
    highestSector: 1,
    totalRuns: 0,
    totalNearMisses: 0,
    preferredControlMode: "ALTERNATING",
    musicOn: true,
    sfxOn: true,
    hapticsOn: true,
    createdAt: now,
    updatedAt: now,
  };
}

function validGhostPayload(uid, now) {
  return {
    userId: uid,
    sectorIndex: 1,
    score: 420,
    distance: 840,
    crashX: 120.5,
    crashY: -840.25,
    packedPoints: "0.0,0.0;45.5,-220.0;120.5,-840.25",
    colorIndex: 2,
    createdAt: now,
    updatedAt: now,
  };
}

function validRunPayload(uid, now) {
  return {
    userId: uid,
    mode: "SECTOR",
    sectorIndex: 1,
    score: 420,
    nearMisses: 3,
    cleared: false,
    createdAt: now,
  };
}

test("Unauthenticated user: cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, now)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("ghosts").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("runs").get());
});

test("UserProfile: owner create, partial update, immutable fields, and shadow field rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, created))
  );

  // Partial update allowed fields only
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      bestEndlessScore: 1250,
      highestSector: 3,
      totalRuns: 5,
      totalNearMisses: 12,
      preferredControlMode: "SPLIT",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject mutating immutable createdAt
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject shadow field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject invalid control mode enum
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredControlMode: "GYRO",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      bestEndlessScore: 2000,
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Cross-user isolation: Bob cannot read or write Alice's profile, ghosts, or runs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, now)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("ghosts").doc("g1").set(validGhostPayload(ALICE_UID, now))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("runs").doc("r1").set(validRunPayload(ALICE_UID, now))
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ displayName: "Hacker", updatedAt: now }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("ghosts").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("ghosts").doc("g2").set(validGhostPayload(BOB_UID, now))
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("ghosts").doc("g1").delete());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("runs").get());
});

test("GhostRecord & RunLog: Kotlin-mirrored queries, creates, deletes, and constraints", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Empty collection list succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("ghosts").where("sectorIndex", "==", 1).get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("runs").orderBy("createdAt", "desc").limit(20).get()
  );

  // Create ghost (no id field)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("ghosts").doc("ghost_1").set(validGhostPayload(ALICE_UID, now))
  );

  // Reject ghost with extra `id` field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("ghosts").doc("ghost_bad").set({
      id: "ghost_bad",
      ...validGhostPayload(ALICE_UID, now),
    })
  );

  // Delete own ghost succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("ghosts").doc("ghost_1").delete()
  );

  // Create run log succeeds, update is forbidden (immutable)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("runs").doc("run_1").set(validRunPayload(ALICE_UID, now))
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("runs").doc("run_1").update({ score: 99999 })
  );
});
