# Learnerport Production Systems — Implementation Plan

Implementation blueprint to add four production-ready systems to the existing Learnerport Android application:
1. **Module 1 (Phase A): Real Views System & Persistent Likes**
2. **Module 2 (Phase B): Gmail 6-Digit OTP Verification**
3. **Module 3 (Phase C): Real SMS OTP Verification (Firebase Phone Auth)**
4. **Module 4 (Phase D): Real-Time 1-on-1 Messaging & Attachment Sharing**
5. **Auth UI Refinement**: Explicitly isolated Google Sign-In vs. manual Log In / Sign Up flows.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> **Explicit Action Isolation for Google Sign-In:**
> Per your instructions (*"bhai tum esa karo ki signup with google par dabau tabhi google ke sath signup hoga ok warna ham login bhi kar sakte ho ya signup bhi"*), Google Sign-In will **never** trigger automatically or preemptively. The Welcome screen will feature distinct, clear action controls:
> 1. **"Continue with Google"** (triggers Google Credential Manager only upon user tap)
> 2. **"Log In with Email / Phone"** (directs returning users to log in)
> 3. **"Create New Account"** (directs new learners to sign up)

> [!NOTE]
> Work will proceed in four sequential phases (**Phase A → Phase B → Phase C → Phase D**). Each phase will be implemented, verified via compilation and Robolectric unit tests, and reported before moving forward.

---

## 1. Overview & Core Concept

Learnerport is transitioning from local in-memory counters to production-grade, distributed Cloud Firestore and Firebase Storage primitives:
- **No Simulated Counters:** Views and likes will be verified against authenticated Firebase UIDs with server timestamps and atomic transactions.
- **Genuine Engagement:** Authors viewing or liking their own resources will be filtered from inflating public counts.
- **Transactional Messaging:** Encrypted subcollections under `/conversations/{id}/messages` with read receipts and attachment streaming.
- **Dual OTP Verification:** Secure backend-hashed 6-digit email OTP and Firebase native SMS OTP for mobile verification.

---

## 2. Phase-by-Phase Architecture & Implementation

### Phase A: Real Views and Persistent Likes System

```
┌────────────────────────────────────────────────────────┐
│                   LEARNERPORT CLIENT                   │
│  User opens Resource / Thought or Taps Like (Heart)    │
└───────────────────────────┬────────────────────────────┘
                            │
               Check Auth UID & Author ID
                            │
          ┌─────────────────┴─────────────────┐
          │                                   │
   [View Event]                          [Like Event]
          │                                   │
  Check /views/{uid}                Run Atomic Transaction on
  Deduplication Window              /likes/{uid} doc
  (e.g., 24h cooldown)                        │
          │                         ┌─────────┴─────────┐
    Not Existing?                   │                   │
          │                    Does not exist?       Exists?
    Write /views/{uid}              │                   │
  FieldValue.increment(1)      Create doc          Delete doc
                            increment(+1)       increment(-1)
```

1. **Real Views System:**
   - **Firestore Subcollections:**
     - `/resources/{resourceId}/views/{uid}` with `{ viewedAt: FieldValue.serverTimestamp(), uid: userId }`
     - `/thoughts/{thoughtId}/views/{uid}` with `{ viewedAt: FieldValue.serverTimestamp(), uid: userId }`
   - **Deduplication Window:** Re-opening the same PDF within a 24-hour window will not increment the counter.
   - **Creator Exclusion:** If `resource.authorId == currentAuthUser.uid`, the view event is logged locally but does not increment public `viewsCount`.
   - **Atomic Increment:** Uses Firestore `FieldValue.increment(1)` on the parent document.

2. **Persistent Likes System:**
   - **Firestore Subcollections:**
     - `/resources/{resourceId}/likes/{uid}` with `{ likedAt: FieldValue.serverTimestamp(), displayName: ... }`
     - `/thoughts/{thoughtId}/likes/{uid}` with `{ likedAt: FieldValue.serverTimestamp(), displayName: ... }`
   - **Atomic Transaction:** Evaluates whether `/likes/{uid}` exists. If absent, creates the record and atomically increments `likesCount + 1`. If already present, deletes the record and decrements `likesCount - 1`.
   - **Real-Time Snapshot Listener:** Attaches a listener to the current user's like document so heart icons reflect state across app restarts.

---

### Phase B: Gmail 6-Digit OTP Delivery & Verification

```
┌──────────────────┐           ┌────────────────────────┐           ┌──────────────────┐
│ Learnerport App  │ ───────>  │  Secure Cloud Backend  │ ───────>  │ Email Provider   │
│ (Enters Email)   │  Request  │  (Hashes OTP, Stores   │  Dispatch │ (Brevo / Resend) │
└──────────────────┘    OTP    │   HMAC, Sets 10m TTL)  │           └────────┬─────────┘
                               └────────────────────────┘                    │
                                                                             ▼
┌──────────────────┐           ┌────────────────────────┐           ┌──────────────────┐
│ Firebase Session │ <───────  │ Verification Endpoint  │ <───────  │ User Gmail Inbox │
│ Established      │  Returns  │ (Compares Hash, Gener- │  Submits  │ (Receives 6-digit│
│ (Custom Token)   │   Token   │  ates Custom Token)    │   Code    │  Learnerport OTP)│
└──────────────────┘           └────────────────────────┘           └──────────────────┘
```

1. **Backend Delivery (Cloud Functions / Cloud Run):**
   - 6-digit cryptographically secure numeric OTP generated exclusively on the server.
   - Only a salted SHA-256 / HMAC hash of the OTP is stored in `/otp_requests/{email}` with a 10-minute expiration timestamp.
   - Dispatches branded email via transactional SMTP/API (Brevo / Resend) with Learnerport template.
2. **Strict Client-Side Security:**
   - Provider API keys and secret master keys are never stored in the Android codebase.
   - Rate limiting: Max 5 failed verification attempts before the OTP is invalidated; 60-second resend cooldown.
3. **Authentication Session:**
   - Upon successful server-side OTP validation, the backend returns a Firebase Custom Auth Token, which the Android client consumes via `FirebaseAuth.getInstance().signInWithCustomToken()`.

---

### Phase C: SMS OTP Verification (Firebase Phone Authentication)

1. **Native Integration:**
   - Connects `PhoneAuthProvider.verifyPhoneNumber()` with full callbacks:
     - `onVerificationCompleted`: Instant credential handling for instant auto-retrieval.
     - `onCodeSent`: Delivers `verificationId` and `ForceResendingToken` for 6-digit SMS OTP input.
     - `onVerificationFailed`: Maps errors (e.g., invalid phone format, SMS quota, carrier timeouts).
2. **Safety & Test Credentials:**
   - Whitelisted test phone numbers configured in the Firebase Console (e.g., `+91 9999999999` with code `123456`) to test without consuming production SMS quota.
3. **Resend Timer:** 60-second active countdown before permitting new SMS requests.

---

### Phase D: Real-Time 1-on-1 Messaging & Attachments

```
                  ┌────────────────────────────────────────┐
                  │          /conversations/{id}           │
                  │  participants: [uidA, uidB]            │
                  │  lastMessage: "Here is the syllabus"   │
                  │  lastTimestamp: serverTimestamp()      │
                  │  unreadCount_{uidB}: 2                 │
                  └───────────────────┬────────────────────┘
                                      │
               ┌──────────────────────┴──────────────────────┐
               ▼                                             ▼
┌───────────────────────────────┐             ┌───────────────────────────────┐
│ /messages/{messageId}         │             │ Attachments in Storage        │
│ senderId: uidA                │             │ /chats/{convId}/{msgId}.jpg   │
│ text: "Check this chapter"    │             │ /chats/{convId}/{msgId}.pdf   │
│ status: "sent"|"read"         │             └───────────────────────────────┘
│ attachmentUrl: ...            │
└───────────────────────────────┘
```

1. **Data Model & Firestore Schema:**
   - `/conversations/{conversationId}`: Stores participant UIDs, last message preview, unread count maps.
   - `/conversations/{conversationId}/messages/{messageId}`:
     - `senderId`, `receiverId`, `text`, `timestamp`, `status` (`"SENT"`, `"DELIVERED"`, `"READ"`).
     - Optional attachment metadata: `attachmentType` (`"IMAGE"`, `"PDF"`), `attachmentUrl`, `fileName`.
2. **Security & Access Control:**
   - Zero-trust security rules: only users listed in `conversation.participants` can read or write messages.
3. **Moderation & Safety:**
   - Block User: Gated in `/users/{uid}/blockedUsers/{targetUid}`; blocks immediate message dispatch and hides chat.
   - Report User / Conversation: Transmits abuse report to `/reports/{reportId}`.

---

## 3. Key User Flows & Visual Layouts

1. **Authentication Landing Screen:**
   - Prominent Learnerport Mascot and Wordmark.
   - Primary Action: **"Continue with Google"** (only triggers when tapped).
   - Secondary Choices:
     - **"Log In with Email or Phone"** (opens existing user login).
     - **"Create New Account"** (leads to registration with email/phone choices).
2. **Resource Engagement Bar:**
   - Heart Icon (Like) with live numeric counter; toggles state instantly with animated scale feedback.
   - Eye Icon (Views) showing genuine verified views count.
   - Download Icon displaying live progress and verified offline state.
3. **Chat Screen:**
   - Top Bar: Recipient avatar, display name, and online/last active state.
   - Message Bubbles: Left-aligned for incoming, right-aligned (LearnerBlue) for sent.
   - Status ticks: Single check for sent, double check for read.
   - Attachment picker: Fast access to gallery images or PDF study files.

---

## 4. Verification & Testing Strategy

- **Local Unit Testing (Robolectric):**
  - Verify view deduplication logic (views within 24 hours do not increment).
  - Verify creator view exclusion (author views are filtered).
  - Verify like toggle atomic transactions (like increments, duplicate like decrements).
  - Verify chat permission logic and message state transitions.
- **Build Verification:**
  - Strict compilation check with `compile_applet` after each phase.

---

## 5. What Satish Must Do Manually (External Setup & Costs)

### Phase A:
- **Action:** None required. Operates within the existing active Firestore database (`gen-lang-client-0994170463`).

### Phase B (Gmail OTP Backend):
- **Provider Choice:** Brevo (free tier: 300 emails/day) or Resend (free tier: 3,000 emails/month).
- **Backend Hosting:** Firebase Cloud Functions (requires Google Cloud Blaze Pay-As-You-Go plan; generous free tier of 2M invocations/month).

### Phase C (SMS OTP):
- **Action:** Enable **Phone** in Firebase Console under Authentication → Sign-in method. Add test phone numbers under "Phone numbers for testing" for zero-cost local testing.
- **Cost:** 10 free SMS/day on Spark plan; Blaze plan gives 10,000 free Phone verifications/month.

### Phase D (Messaging & Push Notifications):
- **Action:** Cloud Messaging (FCM) is free and unmetered. Firestore and Firebase Storage rules will be deployed via `storage.rules` and `firestore.rules`.
