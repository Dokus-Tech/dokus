package tech.dokus.features.auth.usecases

/**
 * Checks if there's a logged-in user, offline-first style.
 *
 * Looks in the local database first so the app starts instantly even without network.
 * If we find a user, we return them right away and sync from the server in the
 * background to get any updates. Only hits the network if there's no local user.
 */
class CheckAccountUseCase
