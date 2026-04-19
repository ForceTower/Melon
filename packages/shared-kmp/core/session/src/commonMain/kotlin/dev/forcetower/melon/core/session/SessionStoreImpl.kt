package dev.forcetower.melon.core.session

import dev.forcetower.melon.core.database.dao.UserDao
import dev.forcetower.melon.core.database.entity.UserEntity
import dev.forcetower.melon.core.storage.KeyValueStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal const val ACCESS_TOKEN_KEY = "melon.access_token"
internal const val REFRESH_TOKEN_KEY = "melon.refresh_token"

internal class SessionStoreImpl(
    private val storage: KeyValueStorage,
    private val userDao: UserDao,
    private val scope: CoroutineScope,
) : SessionStore {

    private val tokenPresent = MutableStateFlow(false)

    override val authState: StateFlow<AuthState> =
        combine(tokenPresent, userDao.observeCurrent()) { hasToken, entity ->
            if (hasToken && entity != null) AuthState.Authenticated(entity.toDomain())
            else AuthState.Unauthenticated
        }.stateIn(scope, SharingStarted.Eagerly, AuthState.Unauthenticated)

    init {
        scope.launch {
            tokenPresent.value = storage.get(ACCESS_TOKEN_KEY) != null
        }
    }

    override suspend fun getAccessToken(): String? = storage.get(ACCESS_TOKEN_KEY)

    override suspend fun persist(accessToken: String, refreshToken: String, user: User) {
        storage.put(ACCESS_TOKEN_KEY, accessToken)
        storage.put(REFRESH_TOKEN_KEY, refreshToken)
        userDao.upsert(UserEntity(id = user.id, name = user.name, imageUrl = user.imageUrl))
        tokenPresent.value = true
    }

    override suspend fun logout() {
        storage.remove(ACCESS_TOKEN_KEY)
        storage.remove(REFRESH_TOKEN_KEY)
        userDao.clear()
        tokenPresent.value = false
    }
}

private fun UserEntity.toDomain() = User(id = id, name = name, imageUrl = imageUrl)
