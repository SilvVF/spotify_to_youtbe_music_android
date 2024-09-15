package io.silv

//import android.content.Intent
//import android.net.Uri
//import net.openid.appauth.AuthorizationRequest
//import net.openid.appauth.AuthorizationService
//import net.openid.appauth.AuthorizationServiceConfiguration
//import net.openid.appauth.ResponseTypeValues
//
//
//fun authenticationIntent(authService: AuthorizationService): Intent {
//
//    // Configure the authorization request
//    val serviceConfig = AuthorizationServiceConfiguration(
//        Uri.parse("https://accounts.google.com/o/oauth2/auth"),
//        Uri.parse("https://oauth2.googleapis.com/token")
//    )
//
//    val authRequest = AuthorizationRequest.Builder(
//        serviceConfig,
//        "", // Replace with your client ID
//        ResponseTypeValues.CODE,
//        Uri.parse("io.silv:/oauth2redirect") // Replace with your redirect URI
//    )
//        .setScopes(
//            "https://www.googleapis.com/auth/youtube.readonly",
//            "https://www.googleapis.com/auth/youtube.force-ssl"
//        )
//        .build()
//
//    // Start the authorization request
//    val authIntent = authService.getAuthorizationRequestIntent(authRequest)
//    return authIntent
//}