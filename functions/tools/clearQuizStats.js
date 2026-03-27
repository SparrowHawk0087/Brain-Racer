#!/usr/bin/env node
/**
 * Очистка статистики прохождений и результатов в Firestore (Brain Racer).
 *
 * Удаляет / сбрасывает:
 *   - коллекцию quiz_results
 *   - коллекцию quiz_stats
 *   - подколлекцию users/{uid}/quiz_play_counts для каждого пользователя
 *   - поле users.{uid}.stats — сброс к нулям; quizzes_created = длина createdQuizzes (если есть)
 *
 * Опции:
 *   --dry-run          только лог, без записи
 *   --reset-quiz-stats сбросить вложенное поле stats у каждого документа quizzes/*
 *   --delete-challenges удалить все документы challenges/*
 *
 * Запуск (из каталога functions/):
 *   npm run clear-quiz-stats -- [--dry-run] [--reset-quiz-stats] [--delete-challenges]
 *
 * Учётные данные Admin SDK:
 *   Bash/macOS/Linux:
 *     export GOOGLE_APPLICATION_CREDENTIALS=/path/to/serviceAccount.json
 *   Windows PowerShell (текущая сессия):
 *     $env:GOOGLE_APPLICATION_CREDENTIALS = "C:\path\to\serviceAccount.json"
 *   Windows cmd.exe:
 *     set GOOGLE_APPLICATION_CREDENTIALS=C:\path\to\serviceAccount.json
 *
 * Для локального запуска проще всего сервисный ключ: Firebase Console → Project settings → Service accounts.
 */

const admin = require("firebase-admin");

const args = new Set(process.argv.slice(2));
const DRY = args.has("--dry-run");
const RESET_QUIZ_STATS = args.has("--reset-quiz-stats");
const DELETE_CHALLENGES = args.has("--delete-challenges");

function log(...a) {
  console.log("[clear-quiz-stats]", ...a);
}

function emptyUserStats(createdQuizzesLen) {
  return {
    total_quizzes_taken: 0,
    total_points: 0,
    current_streak: 0,
    longest_streak: 0,
    correct_answers: 0,
    incorrect_answers: 0,
    total_questions_answered: 0,
    average_score: 0.0,
    quizzes_created: createdQuizzesLen,
    solo_completed_quiz_ids: [],
    challenge_xp_day_utc_millis: 0,
    challenge_xp_earned_today: 0,
    challenge_pair_paid_today: {},
    last_challenge_created_at: null,
  };
}

/** Соответствует QuizStats в приложении (Firestore snake_case). */
const emptyQuizDocStats = {
  times_taken: 0,
  average_score: 0.0,
  total_attempts: 0,
  completion_rate: 0.0,
  ratings_count: 0,
  average_rating: 0.0,
};

async function recursiveDeleteCollection(db, collectionRef, label) {
  if (DRY) {
    const snap = await collectionRef.limit(1).get();
    log(`DRY: would recursiveDelete ${label} (exists=${!snap.empty})`);
    return;
  }
  await db.recursiveDelete(collectionRef);
  log(`Deleted collection: ${label}`);
}

async function deleteSubcollectionForAllUsers(db, subName) {
  const usersSnap = await db.collection("users").get();
  log(`Users: ${usersSnap.size}, clearing subcollection "${subName}"…`);
  for (const userDoc of usersSnap.docs) {
    const sub = userDoc.ref.collection(subName);
    const path = `${userDoc.ref.path}/${subName}`;
    if (DRY) {
      const sample = await sub.limit(1).get();
      if (!sample.empty) {
        log(`DRY: would recursiveDelete ${path} (has documents)`);
      }
      continue;
    }
    const snap = await sub.limit(1).get();
    if (!snap.empty) {
      await db.recursiveDelete(sub);
      log(`Deleted ${path}`);
    }
  }
}

async function resetAllUserStats(db) {
  const usersSnap = await db.collection("users").get();
  log(`Resetting stats for ${usersSnap.size} users…`);
  let batch = db.batch();
  let n = 0;
  for (const doc of usersSnap.docs) {
    const data = doc.data() || {};
    const created = Array.isArray(data.createdQuizzes) ? data.createdQuizzes.length : 0;
    const stats = emptyUserStats(created);
    if (DRY) {
      log(`DRY: would update ${doc.ref.path} stats (quizzes_created=${created})`);
      continue;
    }
    batch.set(doc.ref, { stats }, { merge: true });
    n++;
    if (n >= 450) {
      await batch.commit();
      batch = db.batch();
      n = 0;
    }
  }
  if (!DRY && n > 0) {
    await batch.commit();
  }
  log("User stats reset done.");
}

async function resetQuizDocumentsStats(db) {
  const snap = await db.collection("quizzes").get();
  log(`Resetting stats inside ${snap.size} quiz documents…`);
  let batch = db.batch();
  let n = 0;
  for (const doc of snap.docs) {
    if (DRY) {
      log(`DRY: would merge stats on ${doc.ref.path}`);
      continue;
    }
    batch.set(doc.ref, { stats: emptyQuizDocStats }, { merge: true });
    n++;
    if (n >= 450) {
      await batch.commit();
      batch = db.batch();
      n = 0;
    }
  }
  if (!DRY && n > 0) {
    await batch.commit();
  }
  log("Quiz document stats reset done.");
}

async function main() {
  if (!admin.apps.length) {
    admin.initializeApp();
  }
  const db = admin.firestore();

  if (typeof db.recursiveDelete !== "function") {
    console.error(
      "Firestore.recursiveDelete недоступен. Обновите firebase-admin / @google-cloud/firestore."
    );
    process.exit(1);
  }

  log(
    `Mode: ${DRY ? "DRY-RUN" : "LIVE"} resetQuizDocStats=${RESET_QUIZ_STATS} deleteChallenges=${DELETE_CHALLENGES}`
  );

  await recursiveDeleteCollection(db, db.collection("quiz_results"), "quiz_results");
  await recursiveDeleteCollection(db, db.collection("quiz_stats"), "quiz_stats");
  await deleteSubcollectionForAllUsers(db, "quiz_play_counts");
  await resetAllUserStats(db);

  if (RESET_QUIZ_STATS) {
    await resetQuizDocumentsStats(db);
  }

  if (DELETE_CHALLENGES) {
    await recursiveDeleteCollection(db, db.collection("challenges"), "challenges");
  }

  log("Finished.");
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
