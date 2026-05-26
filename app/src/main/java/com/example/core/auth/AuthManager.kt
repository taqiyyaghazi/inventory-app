package com.example.core.auth

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String?,
    val isMock: Boolean = false
)

class AuthManager(private val context: Context) {
    private val _userState = MutableStateFlow<UserProfile?>(null)
    val userState: StateFlow<UserProfile?> = _userState.asStateFlow()

    private var firebaseAuth: FirebaseAuth? = null

    init {
        // Safe programmatic initialization of Firebase
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            firebaseAuth = FirebaseAuth.getInstance()
            listenToAuthChanges()
        } catch (e: Throwable) {
            Log.e("AuthManager", "Firebase initialization failed. Continuing in local-only sandbox mode: ${e.message}")
        }
    }

    private fun listenToAuthChanges() {
        firebaseAuth?.addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                _userState.value = UserProfile(
                    uid = user.uid,
                    displayName = user.displayName ?: "Pengguna Inventaris",
                    email = user.email ?: "",
                    photoUrl = user.photoUrl?.toString(),
                    isMock = false
                )
            } else {
                if (_userState.value?.isMock == false) {
                    _userState.value = null
                }
            }
        }
    }

    fun isFirebaseAvailable(): Boolean {
        return firebaseAuth != null
    }

    // Google Sign-In client creation helper
    fun getGoogleSignInClient(webClientId: String): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    // Link Google credential to Firebase
    fun signInWithGoogle(account: GoogleSignInAccount, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val idToken = account.idToken
        if (idToken == null) {
            onFailure(Exception("ID Token is null from Google Sign-In"))
            return
        }

        val auth = firebaseAuth
        if (auth == null) {
            // Safe simulated sandbox link
            signInMockUser(account.displayName ?: "Demo User", account.email ?: "demo@example.com")
            onSuccess()
            return
        }

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onFailure(it)
            }
    }

    // Support convenient quick sandbox sign-in for testing & styling
    fun signInMockUser(name: String, email: String) {
        val safeName = if (name.isBlank()) "Pengguna Uji Coba" else name
        val safeEmail = if (email.isBlank()) "tester@example.com" else email
        _userState.value = UserProfile(
            uid = "mock_user_12345",
            displayName = safeName,
            email = safeEmail,
            photoUrl = null,
            isMock = true
        )
    }

    fun signOut(onComplete: () -> Unit = {}) {
        try {
            firebaseAuth?.signOut()
        } catch (e: Throwable) {
            Log.e("AuthManager", "Firebase signOut failed: ${e.message}")
        }

        // Revoke Google access as well
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            val mGoogleSignInClient = GoogleSignIn.getClient(context, gso)
            mGoogleSignInClient.signOut()
        } catch (e: Throwable) {
            // Safe silent ignore
        }

        _userState.value = null
        onComplete()
    }
}
