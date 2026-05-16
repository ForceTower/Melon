package dev.forcetower.melon.core.network

// Indirection so core/network can stamp a stable installation id on every request
// without depending on persistence. Impl lives wherever the id is actually stored.
interface MachineIdSource {
    suspend fun getMachineId(): String
}
