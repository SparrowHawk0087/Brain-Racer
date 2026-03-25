/**
 * Отправка push (FCM) при создании документа в коллекции `notifications`.
 *
 * Деплой (из корня репозитория, с установленным Firebase CLI):
 *   cd functions && npm install && cd ..
 *   firebase login
 *   firebase use <your-project-id>
 *   firebase deploy --only functions
 *
 * Требуется тариф Blaze для вызовов к FCM из Cloud Functions.
 */
const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore, FieldValue} = require("firebase-admin/firestore");
const {getMessaging} = require("firebase-admin/messaging");

initializeApp();

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
