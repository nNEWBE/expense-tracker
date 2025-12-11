/**
 * Cloud Functions for TrackExpense App
 * 
 * These functions require Firebase Admin SDK to perform operations
 * that aren't possible from client-side code.
 */

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { getFirestore } = require("firebase-admin/firestore");

// Initialize Firebase Admin
initializeApp();

/**
 * Delete a user from Firebase Authentication
 * 
 * This function can only be called by admin users (verified via Firestore)
 * 
 * @param {string} userId - The UID of the user to delete
 * @returns {Object} - Success status
 */
exports.deleteUser = onCall(async (request) => {
    // Check if the caller is authenticated
    if (!request.auth) {
        throw new HttpsError(
            "unauthenticated",
            "You must be logged in to delete users."
        );
    }

    const callerUid = request.auth.uid;
    const targetUserId = request.data.userId;

    if (!targetUserId) {
        throw new HttpsError(
            "invalid-argument",
            "User ID is required."
        );
    }

    // Verify the caller is an admin
    const db = getFirestore();
    const callerDoc = await db.collection("users").doc(callerUid).get();

    if (!callerDoc.exists || !callerDoc.data().isAdmin) {
        throw new HttpsError(
            "permission-denied",
            "Only admins can delete users."
        );
    }

    // Prevent admin from deleting themselves
    if (callerUid === targetUserId) {
        throw new HttpsError(
            "failed-precondition",
            "You cannot delete your own account from admin panel."
        );
    }

    try {
        // Delete the user from Firebase Authentication
        await getAuth().deleteUser(targetUserId);
        
        console.log(`Successfully deleted user ${targetUserId} from Firebase Auth`);
        
        return {
            success: true,
            message: `User ${targetUserId} deleted from Firebase Authentication`
        };
    } catch (error) {
        console.error(`Error deleting user ${targetUserId}:`, error);
        
        // If user doesn't exist in Auth, that's okay - maybe they were already deleted
        if (error.code === "auth/user-not-found") {
            return {
                success: true,
                message: "User not found in Authentication (may already be deleted)"
            };
        }
        
        throw new HttpsError(
            "internal",
            `Failed to delete user: ${error.message}`
        );
    }
});

/**
 * Block a user by disabling their Firebase Auth account
 * 
 * @param {string} userId - The UID of the user to block/unblock
 * @param {boolean} block - Whether to block (true) or unblock (false)
 */
exports.blockUser = onCall(async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Must be logged in.");
    }

    const callerUid = request.auth.uid;
    const { userId, block } = request.data;

    if (!userId) {
        throw new HttpsError("invalid-argument", "User ID is required.");
    }

    // Verify admin
    const db = getFirestore();
    const callerDoc = await db.collection("users").doc(callerUid).get();

    if (!callerDoc.exists || !callerDoc.data().isAdmin) {
        throw new HttpsError("permission-denied", "Only admins can block users.");
    }

    try {
        // Disable/enable the user in Firebase Auth
        await getAuth().updateUser(userId, { disabled: block });
        
        return {
            success: true,
            message: `User ${block ? "blocked" : "unblocked"} successfully`
        };
    } catch (error) {
        console.error(`Error ${block ? "blocking" : "unblocking"} user:`, error);
        throw new HttpsError("internal", error.message);
    }
});
