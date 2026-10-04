package surge.domain.extension.interactor

import kotlinx.coroutines.flow.Flow
import surge.domain.extension.model.ExtensionStore
import surge.domain.extension.repository.ExtensionStoreRepository

class GetExtensionStores(
    private val repository: ExtensionStoreRepository,
) {
    suspend fun get(): List<ExtensionStore> = repository.getAll()

    fun subscribe(): Flow<List<ExtensionStore>> = repository.getAllAsFlow()
}
