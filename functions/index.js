/**
 * Отправка push (FCM) при создании документа в коллекции `notifications`.
 * Каскадное удаление данных Firestore / Storage после удаления пользователя из Auth (v1 trigger).
 *
 * Деплой (из корня репозитория, с установленным Firebase CLI):
 *   cd functions && npm install && cd ..
 *   firebase login
 *   firebase use <your-project-id>
 *   firebase deploy --only functions
 *
 * Требуется тариф Blaze для вызовов к FCM из Cloud Functions.
 */
const functions = require("firebase-functions/v1");
const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore, FieldValue} = require("firebase-admin/firestore");
const {getStorage} = require("firebase-admin/storage");
const {getMessaging} = require("firebase-admin/messaging");

initializeApp();

const db = getFirestore();
const REGION = "europe-west1";
/** Пакеты Firestore: не больше 500 операций. */
const CHUNK = 400;

/**
 * Удаляет все документы, попадающие под запрос `base.where(field, '==', value)`, порциями.
 * @param {FirebaseFirestore.CollectionReference} base
 */
async function deleteWhereEqual(base, field, value) {
  for (;;) {
    const snap = await base.where(field, "==", value).limit(CHUNK).get();
    if (snap.empty) return;
    const batch = db.batch();
    snap.docs.forEach((d) => batch.delete(d.ref));
    await batch.commit();
  }
}

/** Удалить `uid` из массива `friends` у всех пользователей. */
async function removeUidFromFriendsLists(uid) {
  for (;;) {
    const snap = await db
      .collection("users")
      .where("friends", "array-contains", uid)
      .limit(CHUNK)
      .get();
    if (snap.empty) return;
    const batch = db.batch();
    snap.docs.forEach((doc) => {
      batch.update(doc.ref, {friends: FieldValue.arrayRemove(uid)});
    });
    await batch.commit();
  }
}

/** Удалить викторины пользователя и связанные quiz_stats / quiz_results по этим quizId. */
async function deleteQuizzesCreatedBy(uid) {
  for (;;) {
    const snap = await db
      .collection("quizzes")
      .where("createdBy", "==", uid)
      .limit(50)
      .get();
    if (snap.empty) return;
    for (const quizDoc of snap.docs) {
      const quizId = quizDoc.id;
      await deleteWhereEqual(db.collection("quiz_results"), "quizId", quizId);
      try {
        await db.collection("quiz_stats").doc(quizId).delete();
      } catch (e) {
        console.warn("quiz_stats delete skipped", quizId, e.message);
      }
      await quizDoc.ref.delete();
    }
  }
}

/** Рекурсивно: документ `users/{uid}` и подколлекции (quiz_play_counts, drafts и т.д.). */
async function recursiveDeleteUserDoc(uid) {
  const ref = db.collection("users").doc(uid);
  const doc = await ref.get();
  if (!doc.exists) {
    console.log("cleanupUserData: users doc already missing", uid);
    return;
  }
  await db.recursiveDelete(ref);
}

async function deleteAvatarStoragePrefix(uid) {
  try {
    const bucket = getStorage().bucket();
    await bucket.deleteFiles({prefix: `avatars/${uid}/`});
  } catch (e) {
    console.warn("cleanupUserData: storage avatars", uid, e.message);
  }
}

/**
 * Выполняется после успешного удаления пользователя из Firebase Auth.
 * Auth v1 trigger (в v2 onDelete для auth на момент внедрения недоступен).
 */
exports.cleanupUserDataOnAuthDelete = functions
  .region(REGION)
  .runWith({timeoutSeconds: 540, memory: "512MB"})
  .auth.user()
  .onDelete(async (user) => {
    const uid = user.uid;
    console.log("cleanupUserDataOnAuthDelete start", uid);
    try {
      await removeUidFromFriendsLists(uid);

      await deleteWhereEqual(db.collection("friend_requests"), "senderId", uid);
      await deleteWhereEqual(db.collection("friend_requests"), "receiverId", uid);

      await deleteWhereEqual(db.collection("challenges"), "challengerUserId", uid);
      await deleteWhereEqual(db.collection("challenges"), "challengedUserId", uid);

      await deleteWhereEqual(db.collection("quiz_results"), "userId", uid);

      await deleteWhereEqual(db.collection("notifications"), "recipientUserId", uid);
      await deleteWhereEqual(db.collection("notifications"), "actorUserId", uid);

      await deleteQuizzesCreatedBy(uid);

      await deleteAvatarStoragePrefix(uid);

      await recursiveDeleteUserDoc(uid);
      console.log("cleanupUserDataOnAuthDelete done", uid);
    } catch (err) {
      console.error("cleanupUserDataOnAuthDelete failed", uid, err);
      throw err;
    }
  });

exports.onNotificationCreatedPush = onDocumentCreated(
  {
    document: "notifications/{notificationId}",
    region: "europe-west1",
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const data = snap.data();
    const uid = data.recipientUserId;
    if (!uid || typeof uid !== "string") return;

    const userSnap = await getFirestore().doc(`users/${uid}`).get();
    const token = userSnap.get("fcmToken");
    if (!token || typeof token !== "string") return;

    const title = data.title || "Brain Racer";
    const body =
      typeof data.message === "string" && data.message.length > 0
        ? data.message
        : "Новое уведомление";

    const challengeId =
      typeof data.challengeId === "string" ? data.challengeId : "";
    const type = typeof data.type === "string" ? data.type : "";

    try {
      await getMessaging().send({
        token,
        notification: {title, body},
        data: {
          type,
          challengeId,
        },
        android: {
          priority: "high",
          notification: {channelId: "brain_racer_general"},
        },
      });
    } catch (err) {
      const code = err?.code || err?.errorInfo?.code;
      const staleToken =
        code === "messaging/invalid-registration-token" ||
        code === "messaging/registration-token-not-registered";
      if (staleToken) {
        try {
          await getFirestore().doc(`users/${uid}`).update({
            fcmToken: FieldValue.delete(),
          });
        } catch (updateErr) {
          console.error("Failed to clear stale FCM token", {uid, updateErr});
        }
        console.warn("FCM token removed (stale)", {uid, code});
        return;
      }
      console.error("FCM send failed", {uid, code, message: err?.message});
      throw err;
    }
  }
);
