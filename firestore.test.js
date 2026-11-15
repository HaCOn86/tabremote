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

function validDevicePayload(uid, overrides = {}) {
  const pastTime = new Date(Date.now() - 1000);
  return {
    userId: uid,
    deviceName: "Galaxy Tab S9",
    role: "RECEIVER",
    localIp: "192.168.1.42",
    port: 38472,
    sessionToken: "secret_token_1234",
    isOnline: true,
    accessibilityReady: true,
    createdAt: pastTime,
    updatedAt: pastTime,
    ...overrides,
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

test("Unauthenticated user: cannot read or write any devices", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("devices").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("devices").doc("tab_1").set(validDevicePayload(ALICE_UID))
  );
});

test("Owner can create, read, update, and delete valid PairedDevice", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("devices").doc("tab_1");

  const payload = validDevicePayload(ALICE_UID);
  await assertSucceeds(docRef.set(payload));
  await assertSucceeds(docRef.get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("devices").get());

  await assertSucceeds(
    docRef.update({
      isOnline: false,
      localIp: "192.168.1.99",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(docRef.delete());
});

test("Cross-user isolation: Bob cannot read, create, update, or delete Alice's devices", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("devices")
      .doc("alice_tab")
      .set(validDevicePayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceDocRef = bobDb.collection("users").doc(ALICE_UID).collection("devices").doc("alice_tab");

  await assertFails(aliceDocRef.get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("devices").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("devices").doc("bob_spoof").set(validDevicePayload(BOB_UID))
  );
  await assertFails(aliceDocRef.update({ isOnline: false, updatedAt: new Date(Date.now() - 500) }));
  await assertFails(aliceDocRef.delete());
});

test("Validation rejects spoofed userId, invalid role, oversized deviceName, ghost fields, and immutable createdAt change", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const devicesCol = aliceDb.collection("users").doc(ALICE_UID).collection("devices");

  // Spoofed userId
  await assertFails(devicesCol.doc("d1").set(validDevicePayload(BOB_UID)));
  // Invalid role
  await assertFails(devicesCol.doc("d2").set(validDevicePayload(ALICE_UID, { role: "ADMIN" })));
  // Oversized deviceName
  await assertFails(devicesCol.doc("d3").set(validDevicePayload(ALICE_UID, { deviceName: "x".repeat(65) })));
  // Short sessionToken
  await assertFails(devicesCol.doc("d4").set(validDevicePayload(ALICE_UID, { sessionToken: "123" })));
  // Ghost field injection
  await assertFails(devicesCol.doc("d5").set(validDevicePayload(ALICE_UID, { isAdmin: true })));

  // Create valid doc, then try mutating createdAt or userId
  const docRef = devicesCol.doc("d_valid");
  await assertSucceeds(docRef.set(validDevicePayload(ALICE_UID)));
  await assertFails(
    docRef.update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    docRef.update({
      createdAt: new Date(Date.now() - 50000),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
